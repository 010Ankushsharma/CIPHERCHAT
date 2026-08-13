package com.cipherchat.server.messaging.service

import com.cipherchat.server.messaging.db.ChatMembers
import com.cipherchat.server.messaging.db.Chats
import com.cipherchat.server.messaging.db.DeliveryReceipts
import com.cipherchat.server.messaging.db.Messages
import com.cipherchat.server.messaging.db.PinnedMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.serialization.Serializable

private val log = LoggerFactory.getLogger("MessagingService")

/**
 * Core messaging business logic. Two key invariants maintained here:
 *
 *  1. This service NEVER inspects [IncomingMessage.perDeviceCiphertextJson]
 *     beyond storing and forwarding it. No JSON parsing of the inner
 *     ciphertext, no content logging, no content-based routing.
 *
 *  2. Fan-out to connected clients happens via Kafka, not direct
 *     WebSocket calls — this service has no reference to any WebSocket
 *     session, making the storage and delivery concerns fully decoupled.
 */
class MessagingService(
    private val kafkaProducer: KafkaProducer<String, String>,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // --- Message storage + fan-out ---

    suspend fun storeAndPublish(message: IncomingMessage): String {
        val messageId = UUID.randomUUID().toString()

        // 1. Verify sender is a member of the chat.
        val isMember = dbQuery {
            ChatMembers.select {
                (ChatMembers.chatId eq message.chatId) and
                    (ChatMembers.userId eq message.senderUserId)
            }.firstOrNull() != null
        }
        if (!isMember) throw MessagingException.NotAChatMember(message.senderUserId, message.chatId)

        // 2. Persist metadata + ciphertext blob.
        dbQuery {
            Messages.insert {
                it[id] = messageId
                it[chatId] = message.chatId
                it[senderUserId] = message.senderUserId
                it[senderDeviceId] = message.senderDeviceId
                it[perDeviceCiphertextJson] = message.perDeviceCiphertextJson
                it[contentType] = message.contentType
                it[sentAt] = OffsetDateTime.now()
                it[expiresAt] = message.expiresAtEpochMs?.let {
                    OffsetDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(it),
                        java.time.ZoneOffset.UTC,
                    )
                }
            }
        }

        // 3. Publish fan-out event to Kafka. Every gateway instance
        //    subscribes to this topic and delivers to locally-connected
        //    recipient sessions. The Kafka message key is the chatId
        //    so all messages for a chat go to the same partition,
        //    preserving ordering within a chat.
        val fanOutEvent = MessageFanOutEvent(
            messageId = messageId,
            chatId = message.chatId,
            senderUserId = message.senderUserId,
            senderDeviceId = message.senderDeviceId,
            perDeviceCiphertextJson = message.perDeviceCiphertextJson,
            contentType = message.contentType,
            sentAtEpochMs = System.currentTimeMillis(),
        )

        kafkaProducer.send(
            ProducerRecord(
                MESSAGES_TOPIC,
                message.chatId,                          // partition key
                json.encodeToString(fanOutEvent),
            ),
        )

        log.debug("Stored + published messageId=$messageId chatId=${message.chatId}")
        return messageId
    }

    // --- Chat management ---

    suspend fun createChat(
        kind: String,
        createdByUserId: String,
        memberIds: List<String>,
        title: String? = null,
    ): String {
        val chatId = UUID.randomUUID().toString()
        dbQuery {
            Chats.insert {
                it[id] = chatId
                it[Chats.kind] = kind
                it[Chats.title] = title
                it[Chats.createdByUserId] = createdByUserId
                it[createdAt] = OffsetDateTime.now()
                it[updatedAt] = OffsetDateTime.now()
            }
            // Creator gets Owner role; all other members get Member role.
            (memberIds + createdByUserId).distinct().forEach { userId ->
                ChatMembers.insert {
                    it[chatId] = chatId
                    it[ChatMembers.userId] = userId
                    it[role] = if (userId == createdByUserId) "owner" else "member"
                    it[joinedAt] = OffsetDateTime.now()
                }
            }
        }
        return chatId
    }

    suspend fun addMember(chatId: String, userId: String, role: String = "member") {
        dbQuery {
            ChatMembers.insert {
                it[ChatMembers.chatId] = chatId
                it[ChatMembers.userId] = userId
                it[ChatMembers.role] = role
                it[joinedAt] = OffsetDateTime.now()
            }
        }
    }

    suspend fun getMessageHistory(
        chatId: String,
        requestingUserId: String,
        beforeMessageId: String? = null,
        limit: Int = 50,
    ): List<MessageMetadata> {
        // Verify membership before returning any history.
        val isMember = dbQuery {
            ChatMembers.select {
                (ChatMembers.chatId eq chatId) and (ChatMembers.userId eq requestingUserId)
            }.any()
        }
        if (!isMember) throw MessagingException.NotAChatMember(requestingUserId, chatId)

        return dbQuery {
            val query = if (beforeMessageId != null) {
                val cursor = Messages.select { Messages.id eq beforeMessageId }
                    .firstOrNull()?.get(Messages.sentAt)
                if (cursor != null) {
                    Messages.select { (Messages.chatId eq chatId) and (Messages.sentAt less cursor) }
                } else {
                    Messages.select { Messages.chatId eq chatId }
                }
            } else {
                Messages.select { Messages.chatId eq chatId }
            }

            query
                .orderBy(Messages.sentAt to org.jetbrains.exposed.sql.SortOrder.DESC)
                .limit(limit)
                .map { row ->
                    MessageMetadata(
                        messageId = row[Messages.id],
                        chatId = row[Messages.chatId],
                        senderUserId = row[Messages.senderUserId],
                        senderDeviceId = row[Messages.senderDeviceId],
                        ciphertextForDevice = extractCiphertextForDevice(
                            row[Messages.perDeviceCiphertextJson],
                            requestingUserId,
                        ),
                        contentType = row[Messages.contentType],
                        sentAtEpochMs = row[Messages.sentAt].toInstant().toEpochMilli(),
                    )
                }
        }
    }

    // --- Delivery receipts ---

    suspend fun recordDeliveryReceipt(messageId: String, deviceId: String, status: String) {
        dbQuery {
            DeliveryReceipts.insert {
                it[DeliveryReceipts.messageId] = messageId
                it[DeliveryReceipts.deviceId] = deviceId
                it[DeliveryReceipts.status] = status
                it[recordedAt] = OffsetDateTime.now()
            }
            // Check if all recipient devices have now delivered.
            // If so, mark allDelivered = true so the background purge
            // sweep can remove the ciphertext blobs.
            checkAndMarkAllDelivered(messageId)
        }
    }

    private fun checkAndMarkAllDelivered(messageId: String) {
        // TODO: compare DeliveryReceipts count against expected
        // recipient device count from perDeviceCiphertextJson.
        // Mark Messages.allDelivered = true when they match.
        // Kept as a TODO rather than incomplete logic that could
        // accidentally purge undelivered messages.
    }

    // --- Helpers ---

    /**
     * Extracts only the ciphertext slice for the requesting device
     * from the full per-device map. A client must only ever receive
     * its own ciphertext slice — returning the full map would expose
     * ciphertext encrypted for other devices, which leaks the
     * recipient device list even if the content is still opaque.
     */
    private fun extractCiphertextForDevice(perDeviceJson: String, userId: String): String? {
        return runCatching {
            val map = json.decodeFromString<Map<String, String>>(perDeviceJson)
            map[userId]
        }.getOrNull()
    }

    companion object {
        const val MESSAGES_TOPIC = "cipherchat.messages.v1"
    }
}

private suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

// --- Input / output models ---

data class IncomingMessage(
    val chatId: String,
    val senderUserId: String,
    val senderDeviceId: String,
    val perDeviceCiphertextJson: String, // Map<deviceId, base64Ciphertext>
    val contentType: String,
    val expiresAtEpochMs: Long? = null,
)

@Serializable
data class MessageFanOutEvent(
    val messageId: String,
    val chatId: String,
    val senderUserId: String,
    val senderDeviceId: String,
    val perDeviceCiphertextJson: String,
    val contentType: String,
    val sentAtEpochMs: Long,
)

data class MessageMetadata(
    val messageId: String,
    val chatId: String,
    val senderUserId: String,
    val senderDeviceId: String,
    val ciphertextForDevice: String?,
    val contentType: String,
    val sentAtEpochMs: Long,
)

sealed class MessagingException(message: String) : Exception(message) {
    class NotAChatMember(userId: String, chatId: String) :
        MessagingException("User $userId is not a member of chat $chatId")
}
