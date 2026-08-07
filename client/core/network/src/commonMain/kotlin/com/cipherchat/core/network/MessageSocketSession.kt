package com.cipherchat.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.min

/**
 * Owns the single persistent WebSocket connection to CipherChat's
 * realtime gateway. Used for anything that needs server push:
 * incoming encrypted messages, typing indicators, presence/Smart
 * Presence updates, delivery/read receipts, and call signaling
 * (offer/answer/ICE candidates for WebRTC).
 *
 * This class is intentionally "dumb": it frames/unframes JSON
 * envelopes and manages connection lifecycle, but has zero awareness
 * of encryption or domain models — [WsServerEvent.EncryptedMessage]
 * carries opaque ciphertext bytes that core:data is responsible for
 * routing into core:crypto for decryption. That separation means this
 * class (and its reconnection/backoff logic) can be tested with fake
 * servers sending arbitrary bytes, with no crypto involved at all.
 *
 * Reconnection: uses exponential backoff capped at [MAX_BACKOFF_MS],
 * and on reconnect emits [WsConnectionState.Reconnected] rather than
 * silently going from Disconnected straight to Connected — callers
 * (e.g. core:data's sync orchestrator) use that signal to know they
 * should re-fetch anything that might have been missed while offline,
 * rather than assuming the WebSocket alone guarantees no gaps.
 */
class MessageSocketSession(
    private val httpClient: HttpClient,
    private val gatewayUrl: String,
    private val authTokenProvider: suspend () -> String?,
    private val scope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private var socketSession: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null
    private var reconnectAttempt = 0

    private val _connectionState = MutableStateFlow<WsConnectionState>(WsConnectionState.Disconnected)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<WsServerEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsServerEvent> = _events.asSharedFlow()

    fun connect() {
        if (connectionJob?.isActive == true) return
        connectionJob = scope.launch { runConnectionLoop() }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        scope.launch { socketSession?.close() }
        _connectionState.value = WsConnectionState.Disconnected
        reconnectAttempt = 0
    }

    suspend fun send(event: WsClientEvent) {
        val session = socketSession ?: run {
            // Caller's responsibility to retry once Connected is observed;
            // we don't silently buffer-and-replay here because the
            // semantics of "replay after reconnect" differ per event type
            // (a typing indicator should NOT be replayed after a 30s
            // outage; an outgoing message send should go through
            // core:data's outbox queue instead of this transient buffer).
            throw IllegalStateException("Not connected — cannot send ${event::class.simpleName}")
        }
        session.send(Frame.Text(json.encodeToString(event)))
    }

    private suspend fun runConnectionLoop() {
        while (true) {
            try {
                _connectionState.value = if (reconnectAttempt == 0) WsConnectionState.Connecting else WsConnectionState.Reconnecting
                val token = authTokenProvider() ?: run {
                    _connectionState.value = WsConnectionState.Disconnected
                    return
                }

                httpClient.webSocketSession(urlString = gatewayUrl) {
                    header("Authorization", "Bearer $token")
                }.also { socketSession = it }
                    .let { session ->
                        _connectionState.value = if (reconnectAttempt == 0) {
                            WsConnectionState.Connected
                        } else {
                            WsConnectionState.Reconnected
                        }
                        reconnectAttempt = 0
                        receiveLoop(session)
                    }
            } catch (e: Exception) {
                _connectionState.value = WsConnectionState.Disconnected
            }

            reconnectAttempt += 1
            val backoffMs = min(BASE_BACKOFF_MS * (1 shl min(reconnectAttempt, 6)), MAX_BACKOFF_MS)
            delay(backoffMs)
        }
    }

    private suspend fun receiveLoop(session: DefaultClientWebSocketSession) {
        for (frame in session.incoming) {
            if (frame !is Frame.Text) continue
            val event = runCatching { json.decodeFromString<WsServerEvent>(frame.readText()) }.getOrNull()
            if (event != null) _events.emit(event)
        }
    }

    companion object {
        private const val BASE_BACKOFF_MS = 500L
        private const val MAX_BACKOFF_MS = 30_000L
    }
}

sealed class WsConnectionState {
    data object Disconnected : WsConnectionState()
    data object Connecting : WsConnectionState()
    data object Connected : WsConnectionState()
    data object Reconnecting : WsConnectionState()
    /** Distinct from [Connected] — signals callers should reconcile state that may have drifted while offline. */
    data object Reconnected : WsConnectionState()
}
