package com.cipherchat.core.domain.repository

import com.cipherchat.core.domain.model.ChatId
import com.cipherchat.core.domain.model.Message
import com.cipherchat.core.domain.model.MessageId
import com.cipherchat.core.domain.model.MessageContent
import com.cipherchat.core.domain.model.MessageExpiration
import com.cipherchat.core.domain.model.UserId
import kotlinx.coroutines.flow.Flow

/**
 * Contract for all message persistence and transport, implemented by
 * core:data. The domain layer (and every use case built on top of it)
 * only ever sees decrypted [Message] objects — encryption/decryption,
 * network retries, and local caching are entirely hidden behind this
 * interface.
 *
 * Implementations are expected to be OFFLINE-FIRST: [observeMessages]
 * must emit from local storage immediately and update as the network
 * layer syncs, never block on network round-trips.
 */
interface MessageRepository {

    /** Reactive stream of messages in a chat, newest-aware, paginated by the caller via [loadOlderMessages]. */
    fun observeMessages(chatId: ChatId): Flow<List<Message>>

    /**
     * Encrypts and sends [content] to [chatId]. Returns immediately with a
     * locally-created [Message] in [com.cipherchat.core.domain.model.MessageStatus.Sending] state;
     * the repository updates its status asynchronously as delivery receipts arrive.
     */
    suspend fun sendMessage(
        chatId: ChatId,
        content: MessageContent,
        replyTo: MessageId? = null,
        expiration: MessageExpiration = MessageExpiration.None,
    ): Message

    suspend fun editMessage(messageId: MessageId, newContent: MessageContent)

    suspend fun deleteMessage(messageId: MessageId, forEveryone: Boolean)

    suspend fun addReaction(messageId: MessageId, emoji: String)

    suspend fun removeReaction(messageId: MessageId, emoji: String)

    suspend fun markAsRead(messageId: MessageId)

    suspend fun pinMessage(messageId: MessageId, pinned: Boolean)

    suspend fun starMessage(messageId: MessageId, starred: Boolean)

    /** Pagination: fetches and caches the next page of older messages, returns count loaded. */
    suspend fun loadOlderMessages(chatId: ChatId, beforeMessageId: MessageId, pageSize: Int = 50): Int

    /** Full-text + AI-assisted natural-language search across a single chat. */
    fun searchInChat(chatId: ChatId, query: String): Flow<List<Message>>

    /** Global search across all chats the current user is part of. */
    fun searchGlobal(query: String): Flow<List<Message>>

    fun observeStarredMessages(): Flow<List<Message>>

    fun observePinnedMessages(chatId: ChatId): Flow<List<Message>>

    /** Returns the full edit history for a message, newest first. */
    suspend fun getEditHistory(messageId: MessageId): List<Message>
}
