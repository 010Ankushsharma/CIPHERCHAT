package com.cipherchat.core.domain.repository

import com.cipherchat.core.domain.model.Chat
import com.cipherchat.core.domain.model.ChatId
import com.cipherchat.core.domain.model.GroupRole
import com.cipherchat.core.domain.model.MessageExpiration
import com.cipherchat.core.domain.model.UserId
import kotlinx.coroutines.flow.Flow

/**
 * Contract for chat lifecycle and chat-list state, implemented by
 * core:data. Mirrors the Home Screen's main tabs (Chats, Groups,
 * Channels, Communities, Archive, Favorites, Unread) as queryable
 * streams rather than one big list the UI filters client-side —
 * letting the data layer push filtering down to SQLDelight queries
 * instead of recomputing it on every emission.
 */
interface ChatRepository {

    /** All non-archived, non-hidden chats, ordered pinned-first then by recent activity. */
    fun observeChats(): Flow<List<Chat>>

    fun observeArchivedChats(): Flow<List<Chat>>

    fun observeFavoriteChats(): Flow<List<Chat>>

    fun observeUnreadChats(): Flow<List<Chat>>

    /** Chats hidden in the Secure Secret Folder — caller must have already passed PIN/biometric gate. */
    fun observeSecretFolderChats(): Flow<List<Chat>>

    fun observeChatsByKind(kind: KindFilter): Flow<List<Chat>>

    fun observeChat(chatId: ChatId): Flow<Chat?>

    suspend fun createOneToOneChat(otherUserId: UserId): Chat

    suspend fun createGroup(title: String, memberIds: List<UserId>): Chat

    suspend fun createChannel(title: String, isBroadcastOnly: Boolean = true): Chat

    suspend fun createCommunity(title: String): Chat

    suspend fun addMembers(chatId: ChatId, userIds: List<UserId>)

    suspend fun removeMember(chatId: ChatId, userId: UserId)

    suspend fun setMemberRole(chatId: ChatId, userId: UserId, role: GroupRole)

    suspend fun setMuted(chatId: ChatId, muted: Boolean)

    suspend fun setPinned(chatId: ChatId, pinned: Boolean)

    suspend fun setFavorite(chatId: ChatId, favorite: Boolean)

    suspend fun setArchived(chatId: ChatId, archived: Boolean)

    /** Moves the chat in/out of the PIN/biometric-gated Secure Secret Folder. */
    suspend fun setHiddenInSecretFolder(chatId: ChatId, hidden: Boolean)

    suspend fun setDefaultExpiration(chatId: ChatId, expiration: MessageExpiration)

    suspend fun deleteChat(chatId: ChatId)

    suspend fun leaveChat(chatId: ChatId)

    suspend fun blockUser(userId: UserId)

    suspend fun reportChat(chatId: ChatId, reason: String)

    /** AI-assisted: "Find the invoice John sent last month" style cross-chat queries. */
    fun searchChatsNaturalLanguage(query: String): Flow<List<Chat>>
}

enum class KindFilter { OneToOne, Group, Channel, Community }
