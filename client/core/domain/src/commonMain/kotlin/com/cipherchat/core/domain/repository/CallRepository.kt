package com.cipherchat.core.domain.repository

import com.cipherchat.core.domain.model.ChatId
import com.cipherchat.core.domain.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlin.jvm.JvmInline

/**
 * Contract for voice/video calling, implemented by core:data on top of
 * a WebRTC-based transport in core:network. Calls are end-to-end
 * encrypted at the media layer (SRTP with keys derived from the
 * Signal session, not just TLS to the server) — signaling metadata
 * (who's calling whom, call duration) is the only thing the server
 * ever sees.
 */
interface CallRepository {

    fun observeActiveCall(): Flow<ActiveCall?>

    fun observeCallHistory(): Flow<List<CallRecord>>

    suspend fun startCall(chatId: ChatId, media: CallMedia): ActiveCall

    suspend fun answerCall(callId: CallId): ActiveCall

    suspend fun declineCall(callId: CallId)

    suspend fun endCall(callId: CallId)

    suspend fun toggleMute(muted: Boolean)

    suspend fun toggleVideo(enabled: Boolean)

    suspend fun toggleScreenShare(enabled: Boolean)

    suspend fun setVirtualBackground(backgroundId: String?)

    suspend fun setBackgroundBlur(enabled: Boolean)

    suspend fun toggleNoiseCancellation(enabled: Boolean)

    suspend fun startRecording(): Boolean // returns false if disabled by policy/participant objection

    suspend fun stopRecording()

    suspend fun toggleLiveCaptions(enabled: Boolean)

    /** Adds a participant to an in-progress group call. */
    suspend fun addParticipant(callId: CallId, userId: UserId)

    suspend fun removeParticipant(callId: CallId, userId: UserId)

    /** AI Meeting Summary — generated from on-device transcription, never raw audio sent to a third party. */
    suspend fun generateMeetingSummary(callId: CallId): MeetingSummary

    fun observeCallNotes(callId: CallId): Flow<String>

    suspend fun updateCallNotes(callId: CallId, notes: String)
}

@JvmInline
value class CallId(val value: String)

enum class CallMedia { VoiceOnly, Video }

enum class CallState { Ringing, Connecting, Active, OnHold, Ended }

data class ActiveCall(
    val id: CallId,
    val chatId: ChatId,
    val media: CallMedia,
    val state: CallState,
    val participants: List<CallParticipant>,
    val startedAt: Instant,
    val isRecording: Boolean = false,
    val captionsEnabled: Boolean = false,
)

data class CallParticipant(
    val userId: UserId,
    val isMuted: Boolean,
    val isVideoEnabled: Boolean,
    val isScreenSharing: Boolean,
    val connectionQuality: ConnectionQuality,
)

enum class ConnectionQuality { Excellent, Good, Poor, Reconnecting }

data class CallRecord(
    val id: CallId,
    val chatId: ChatId,
    val media: CallMedia,
    val participants: List<UserId>,
    val startedAt: Instant,
    val durationSeconds: Long,
    val wasMissed: Boolean,
)

data class MeetingSummary(
    val callId: CallId,
    val keyPoints: List<String>,
    val actionItems: List<String>,
    val decisions: List<String>,
)
