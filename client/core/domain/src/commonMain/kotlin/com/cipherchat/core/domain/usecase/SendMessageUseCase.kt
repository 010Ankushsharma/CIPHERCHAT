package com.cipherchat.core.domain.usecase

import com.cipherchat.core.domain.model.ChatId
import com.cipherchat.core.domain.model.Message
import com.cipherchat.core.domain.model.MessageContent
import com.cipherchat.core.domain.model.MessageExpiration
import com.cipherchat.core.domain.model.MessageId
import com.cipherchat.core.domain.repository.ChatRepository
import com.cipherchat.core.domain.repository.MessageRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * Sends a message to a chat, applying business rules that don't
 * belong in [MessageRepository] (a pure persistence/transport
 * contract) and shouldn't be duplicated across every ViewModel that
 * needs to send a message (chat screen, AI smart-reply tap, scheduled
 * message firing, etc.).
 *
 * Rules enforced here:
 *  - Text content cannot be blank/whitespace-only.
 *  - If the target chat has a [Chat.defaultExpiration] set (e.g. the
 *    user turned on Disappearing Messages for this chat) and the
 *    caller didn't explicitly override it, the chat's default is
 *    applied automatically — callers shouldn't have to remember to
 *    look that up themselves every time.
 *  - Whisper Messages ([MessageExpiration.ReadOnce]) are only valid
 *    for [MessageContent.Text], [MessageContent.Image], and
 *    [MessageContent.VoiceNote] per product rules; anything else
 *    falls back to no expiration rather than silently failing.
 */
class SendMessageUseCase(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(
        chatId: ChatId,
        content: MessageContent,
        replyTo: MessageId? = null,
        explicitExpiration: MessageExpiration? = null,
    ): SendMessageResult {
        if (content is MessageContent.Text && content.body.isBlank()) {
            return SendMessageResult.Rejected(SendMessageRejection.EmptyContent)
        }

        val resolvedExpiration = explicitExpiration
            ?: chatRepository.observeChat(chatId).firstOrNull()?.defaultExpiration
            ?: MessageExpiration.None

        val finalExpiration = sanitizeExpirationForContent(resolvedExpiration, content)

        val sent = messageRepository.sendMessage(
            chatId = chatId,
            content = content,
            replyTo = replyTo,
            expiration = finalExpiration,
        )
        return SendMessageResult.Sent(sent)
    }

    private fun sanitizeExpirationForContent(
        expiration: MessageExpiration,
        content: MessageContent,
    ): MessageExpiration {
        val whisperEligible = content is MessageContent.Text ||
            content is MessageContent.Image ||
            content is MessageContent.VoiceNote
        return if (expiration is MessageExpiration.ReadOnce && !whisperEligible) {
            MessageExpiration.None
        } else {
            expiration
        }
    }
}

sealed class SendMessageResult {
    data class Sent(val message: Message) : SendMessageResult()
    data class Rejected(val reason: SendMessageRejection) : SendMessageResult()
}

enum class SendMessageRejection { EmptyContent, RecipientBlocked, ChatArchivedReadOnly }
