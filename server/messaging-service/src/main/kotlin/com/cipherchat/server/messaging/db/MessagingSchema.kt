package com.cipherchat.server.messaging.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

/**
 * Messaging service schema. The most important design constraint here:
 * the [Messages] table has a [Messages.ciphertext] column but NO
 * plaintext column — there is nowhere in this schema to store message
 * content in readable form. This is a structural enforcement of the
 * "server never stores plaintext" guarantee, not just a policy.
 *
 * Schema maps to SQLDelight client tables
 * (client/core/database/src/commonMain/sqldelight/) — kept in sync by
 * convention, not code generation, since client and server use
 * different persistence libraries (SQLDelight vs Exposed).
 */

object Chats : Table("chats") {
    val id = varchar("id", 36)
    val kind = varchar("kind", 20)         // "one_to_one" | "group" | "channel" | "community"
    val title = varchar("title", 255).nullable()
    val createdByUserId = varchar("created_by_user_id", 36)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
    init { index(false, createdByUserId) }
}

object ChatMembers : Table("chat_members") {
    val chatId = varchar("chat_id", 36).references(Chats.id)
    val userId = varchar("user_id", 36)
    val role = varchar("role", 20).default("member")   // "owner" | "admin" | "member"
    val joinedAt = timestampWithTimeZone("joined_at")
    val mutedUntil = timestampWithTimeZone("muted_until").nullable()

    override val primaryKey = PrimaryKey(chatId, userId)
    init { index(false, userId) }
}

object Messages : Table("messages") {
    val id = varchar("id", 36)
    val chatId = varchar("chat_id", 36).references(Chats.id)
    val senderUserId = varchar("sender_user_id", 36)
    val senderDeviceId = varchar("sender_device_id", 36)
    /**
     * Per-recipient-device ciphertext, stored as a JSON map of
     * deviceId -> base64CiphertextBlob. The server stores this
     * temporarily so offline recipients can fetch messages they
     * missed while disconnected (pull-on-reconnect). Once all
     * recipient devices have acknowledged delivery, the ciphertext
     * blobs can be purged — the server has no reason to keep them
     * beyond delivery. A background sweep handles purging.
     *
     * This IS the "opaque ciphertext" — the server routes it to
     * recipients but never decrypts, parses, or logs its contents.
     */
    val perDeviceCiphertextJson = text("per_device_ciphertext_json")
    val contentType = varchar("content_type", 30)      // "text" | "image" | etc. — metadata only
    val sentAt = timestampWithTimeZone("sent_at")
    val expiresAt = timestampWithTimeZone("expires_at").nullable()
    // allDelivered: true once every target device has acknowledged.
    // Used by the background purge sweep to identify blobs safe to delete.
    val allDelivered = bool("all_delivered").default(false)
    val allDeliveredAt = timestampWithTimeZone("all_delivered_at").nullable()

    override val primaryKey = PrimaryKey(id)
    init {
        index(false, chatId, sentAt)
        index(false, allDelivered, allDeliveredAt)     // purge sweep
        index(false, expiresAt)                        // expiry sweep
    }
}

object DeliveryReceipts : Table("delivery_receipts") {
    val messageId = varchar("message_id", 36).references(Messages.id)
    val deviceId = varchar("device_id", 36)
    val status = varchar("status", 20)                 // "delivered" | "read"
    val recordedAt = timestampWithTimeZone("recorded_at")

    override val primaryKey = PrimaryKey(messageId, deviceId)
}

object PinnedMessages : Table("pinned_messages") {
    val chatId = varchar("chat_id", 36).references(Chats.id)
    val messageId = varchar("message_id", 36).references(Messages.id)
    val pinnedByUserId = varchar("pinned_by_user_id", 36)
    val pinnedAt = timestampWithTimeZone("pinned_at")

    override val primaryKey = PrimaryKey(chatId, messageId)
}
