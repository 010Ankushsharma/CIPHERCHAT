package com.cipherchat.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A single message inside a [Chat].
 *
 * CRITICAL ARCHITECTURAL NOTE: this type represents a message ONLY
 * after decryption, held in memory (or in the encrypted local
 * SQLDelight store — see core:database, which encrypts at rest via
 * SQLCipher). It must never be constructed from raw server payloads
 * directly; core:network DTOs are decrypted by core:crypto into this
 * type, and that boundary is the one place plaintext briefly exists
 * outside the device's secure memory.
 */
@Serializable
data class Message(
    val id: MessageId,
    val chatId: ChatId,
    val senderId: UserId,
    val content: MessageContent,
    val status: MessageStatus,
    val sentAt: Instant,
    val editedAt: Instant? = null,
    val editHistory: List<MessageEdit> = emptyList(),
    val replyTo: MessageId? = null,
    val threadRootId: MessageId? = null,
    val reactions: List<Reaction> = emptyList(),
    val mentions: List<UserId> = emptyList(),
    val isPinned: Boolean = false,
    val isStarred: Boolean = false,
    val expiration: MessageExpiration = MessageExpiration.None,
    val deliveryReceipts: List<DeliveryReceipt> = emptyList(),
)

@Serializable
@JvmInline
value class MessageId(val value: String)

@Serializable
@JvmInline
value class ChatId(val value: String)

/**
 * Sealed so the UI exhaustively handles every message type at compile
 * time — adding a new content type (e.g. AI-generated sticker) forces
 * every renderer to consciously decide how to display it, rather than
 * silently falling through to a default case.
 */
@Serializable
sealed class MessageContent {
    @Serializable
    data class Text(val body: String, val markdown: Boolean = true) : MessageContent()

    @Serializable
    data class VoiceNote(val mediaRef: MediaRef, val durationMs: Long, val waveform: List<Float> = emptyList()) : MessageContent()

    @Serializable
    data class Video(val mediaRef: MediaRef, val durationMs: Long, val thumbnailRef: MediaRef? = null) : MessageContent()

    @Serializable
    data class Image(val mediaRef: MediaRef, val width: Int, val height: Int) : MessageContent()

    @Serializable
    data class Document(val mediaRef: MediaRef, val fileName: String, val sizeBytes: Long) : MessageContent()

    @Serializable
    data class Sticker(val mediaRef: MediaRef, val packId: String, val aiGenerated: Boolean = false) : MessageContent()

    @Serializable
    data class Gif(val mediaRef: MediaRef) : MessageContent()

    @Serializable
    data class Location(val latitude: Double, val longitude: Double, val isLive: Boolean = false, val label: String? = null) : MessageContent()

    @Serializable
    data class Poll(val question: String, val options: List<PollOption>, val allowsMultiple: Boolean) : MessageContent()

    @Serializable
    data class Code(val source: String, val language: String) : MessageContent()

    @Serializable
    data class MathEquation(val latex: String) : MessageContent()

    @Serializable
    data class ContactCard(val contactUserId: UserId) : MessageContent()

    @Serializable
    data class SystemEvent(val description: String) : MessageContent() // "Jane joined the group"
}

/** Opaque reference to encrypted media stored via the media service — never a raw URL to plaintext bytes. */
@Serializable
data class MediaRef(val storageKey: String, val encryptionKeyRef: String)

@Serializable
data class PollOption(val id: String, val text: String, val voteCount: Int = 0)

@Serializable
enum class MessageStatus { Sending, Sent, Delivered, Read, Failed }

@Serializable
data class DeliveryReceipt(val deviceId: DeviceId, val status: MessageStatus, val at: Instant)

@Serializable
data class Reaction(val userId: UserId, val emoji: String, val at: Instant)

@Serializable
data class MessageEdit(val previousContent: MessageContent, val editedAt: Instant)

/**
 * Drives Disappearing Messages, Self-Destruct Chats, and Whisper
 * Messages. [ReadOnce] additionally implies (enforced in the UI layer,
 * not here) no screenshot, no forward, no copy — see the Whisper
 * Messages feature.
 */
@Serializable
sealed class MessageExpiration {
    @Serializable
    data object None : MessageExpiration()

    @Serializable
    data class AfterDuration(val durationSeconds: Long) : MessageExpiration()

    /** Whisper Messages: disappears immediately after being read. */
    @Serializable
    data object ReadOnce : MessageExpiration()

    @Serializable
    data class AtTimestamp(val expiresAt: Instant) : MessageExpiration()
}
