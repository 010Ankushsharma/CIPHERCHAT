package com.cipherchat.core.domain.repository

import com.cipherchat.core.domain.model.ChatId
import com.cipherchat.core.domain.model.Message
import com.cipherchat.core.domain.model.MessageId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Contract for every AI-powered capability, implemented by core:data.
 *
 * PRIVACY PRINCIPLE (per spec: "Local AI whenever possible"): the
 * implementation must prefer an on-device model for anything that
 * requires reading message plaintext (summarization, smart reply,
 * tone analysis, spam detection) and only fall back to a remote model
 * when the user has explicitly opted in for that feature — and even
 * then, only the minimum text needed is sent, never raw media or full
 * chat history. This interface intentionally has no parameter for
 * "send full chat to server" — that's a deliberate constraint, not an
 * oversight.
 */
interface AiRepository {

    suspend fun summarizeChat(chatId: ChatId, sinceMessageId: MessageId? = null): ChatSummary

    suspend fun summarizeUnread(chatId: ChatId): String

    suspend fun translateMessage(messageId: MessageId, targetLanguage: String): String

    suspend fun rewriteMessage(draft: String, style: RewriteStyle): String

    suspend fun checkGrammar(draft: String): GrammarCheckResult

    /** Generates 2-4 short smart-reply suggestions for the most recent message in a chat. */
    suspend fun generateSmartReplies(chatId: ChatId): List<String>

    suspend fun analyzeTone(draft: String): ToneAnalysis

    suspend fun detectImportantMessages(chatId: ChatId): List<MessageId>

    suspend fun extractTasks(chatId: ChatId): List<ExtractedTask>

    suspend fun detectCalendarEvents(chatId: ChatId): List<ExtractedEvent>

    suspend fun classifyForSafety(message: Message): SafetyClassification

    suspend fun transcribeVoiceNote(messageId: MessageId): String

    suspend fun synthesizeSpeech(text: String, voiceId: String? = null): MediaSynthesisResult

    suspend fun generateSticker(prompt: StickerPrompt): StickerGenerationResult

    /** Natural-language query across the user's own message history, e.g. "the invoice John sent last month." */
    fun naturalLanguageSearch(query: String): Flow<List<Message>>

    // --- AI Memory: durable cross-conversation context ---
    suspend fun rememberFact(scope: MemoryScope, fact: String)

    fun observeMemories(scope: MemoryScope): Flow<List<AiMemory>>

    suspend fun forgetMemory(memoryId: String)
}

data class ChatSummary(val chatId: ChatId, val bulletPoints: List<String>, val generatedAt: Instant)

enum class RewriteStyle { Professional, Casual, Concise, Friendly, Assertive }

data class GrammarCheckResult(val corrected: String, val issues: List<GrammarIssue>)
data class GrammarIssue(val original: String, val suggestion: String, val explanation: String)

data class ToneAnalysis(val primaryTone: String, val confidence: Float, val suggestion: String? = null)

data class ExtractedTask(val description: String, val sourceMessageId: MessageId, val dueDate: Instant? = null)

data class ExtractedEvent(val title: String, val sourceMessageId: MessageId, val startsAt: Instant, val location: String? = null)

/** Result of spam/scam/phishing classification — never silently auto-deletes, always surfaces to the user. */
data class SafetyClassification(
    val isSpam: Boolean,
    val isScam: Boolean,
    val isPhishing: Boolean,
    val confidence: Float,
    val reason: String? = null,
)

data class MediaSynthesisResult(val mediaStorageKey: String, val durationMs: Long)

data class StickerPrompt(val text: String? = null, val sourcePhotoRef: String? = null, val sourceEmoji: String? = null)

sealed class StickerGenerationResult {
    data class Success(val mediaStorageKey: String) : StickerGenerationResult()
    data class Failure(val reason: String) : StickerGenerationResult()
}

enum class MemoryScope { Global, PerChat }

data class AiMemory(val id: String, val scope: MemoryScope, val fact: String, val createdAt: Instant, val relatedChatId: ChatId? = null)
