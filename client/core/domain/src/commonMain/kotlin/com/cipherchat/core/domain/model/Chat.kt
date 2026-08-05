package com.cipherchat.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * A conversation surface. CipherChat unifies 1:1 chats, groups,
 * channels, and communities under one [Chat] type with a [ChatKind]
 * discriminator rather than four separate types, because the vast
 * majority of behavior (message list, search, pinning, archiving,
 * mute, AI assistant access) is identical across all four — only
 * membership/permission semantics differ, which live in [ChatKind].
 */
@Serializable
data class Chat(
    val id: ChatId,
    val kind: ChatKind,
    val title: String,
    val avatarUrl: String? = null,
    val lastMessagePreview: MessagePreview? = null,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    /** Hidden chats under Secure Secret Folder — excluded from normal lists, PIN/biometric gated. */
    val isHiddenInSecretFolder: Boolean = false,
    val defaultExpiration: MessageExpiration = MessageExpiration.None,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * Discriminates conversation semantics. Sealed (not an enum) because
 * each kind carries different structural data — e.g. only groups have
 * a member-role map, only channels have a subscriber-only broadcast
 * flag, only communities nest other chats.
 */
@Serializable
sealed class ChatKind {
    @Serializable
    data class OneToOne(val otherUserId: UserId) : ChatKind()

    @Serializable
    data class Group(
        val memberRoles: Map<UserId, GroupRole>,
        val description: String? = null,
    ) : ChatKind()

    @Serializable
    data class Channel(
        val ownerId: UserId,
        val subscriberCount: Long,
        val isBroadcastOnly: Boolean = true, // only owner/admins can post
    ) : ChatKind()

    @Serializable
    data class Community(
        val ownerId: UserId,
        val memberChatIds: List<ChatId>, // nested channels/groups within the community
    ) : ChatKind()
}

@Serializable
enum class GroupRole { Owner, Admin, Member }

/**
 * Lightweight preview shown in the chat list, deliberately separate
 * from a full [Message] so the chat-list screen never needs to load
 * (or decrypt more than) a one-line summary per row.
 */
@Serializable
data class MessagePreview(
    val messageId: MessageId,
    val senderId: UserId,
    val summary: String,       // e.g. "Jane: 📷 Photo" — pre-rendered, content-type aware
    val sentAt: Instant,
    val status: MessageStatus,
)
