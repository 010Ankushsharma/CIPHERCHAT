package com.cipherchat.server.gateway.websocket

import com.cipherchat.server.gateway.auth.userId
import com.cipherchat.server.gateway.auth.deviceId
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import com.cipherchat.server.gateway.plugins.RateLimitName.Companion.WEBSOCKET

private val log = LoggerFactory.getLogger("WebSocketHandler")

fun Application.configureWebSockets() {
    install(WebSockets) {
        // Server-initiated pings every 30s — detects silently dropped
        // connections (e.g. mobile client goes offline without a clean
        // TCP close) faster than TCP keepalive alone, which can take
        // minutes on mobile networks.
        pingPeriod = 30.seconds
        timeout = 60.seconds
        maxFrameSize = 64 * 1024 // 64KB max frame — message ciphertext
        // should comfortably fit; anything larger is likely malformed
        masking = false // server-to-client frames don't need masking per RFC 6455
    }

    val sessionRegistry: WebSocketSessionRegistry by inject()

    routing {
        authenticate("jwt-bearer") {
            rateLimit(WEBSOCKET) {
                webSocket("/v1/ws") {
                    val principal = call.principal<JWTPrincipal>()
                        ?: run { close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized")); return@webSocket }

                    val userId = principal.userId
                    val deviceId = principal.deviceId

                    log.info("WebSocket connected: userId=$userId deviceId=$deviceId")
                    sessionRegistry.register(userId, deviceId, this)

                    try {
                        incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                handleClientEvent(frame.readText(), userId, deviceId, sessionRegistry)
                            }
                        }
                    } finally {
                        sessionRegistry.unregister(userId, deviceId)
                        log.info("WebSocket disconnected: userId=$userId deviceId=$deviceId")
                    }
                }
            }
        }
    }
}

private val json = Json { ignoreUnknownKeys = true }

private suspend fun handleClientEvent(
    rawText: String,
    userId: String,
    deviceId: String,
    registry: WebSocketSessionRegistry,
) {
    // Parse just the "type" discriminator first — avoids deserializing
    // the full payload for events we might want to rate-limit or drop
    // before touching the body.
    val type = runCatching {
        json.parseToJsonElement(rawText)
            .let { it as? kotlinx.serialization.json.JsonObject }
            ?.get("type")
            ?.let { it as? kotlinx.serialization.json.JsonPrimitive }
            ?.content
    }.getOrNull() ?: run {
        log.warn("Malformed WS frame from userId=$userId — no type discriminator")
        return
    }

    when (type) {
        "send_encrypted_message" -> handleSendEncryptedMessage(rawText, userId, deviceId, registry)
        "typing_indicator" -> handleTypingIndicator(rawText, userId, registry)
        "presence_update" -> handlePresenceUpdate(rawText, userId, registry)
        "read_receipt" -> handleReadReceipt(rawText, userId, deviceId)
        "call_signal" -> handleCallSignal(rawText, userId, registry)
        "ghost_mode_toggle" -> handleGhostModeToggle(rawText, userId)
        else -> log.warn("Unknown WS event type '$type' from userId=$userId — ignored")
    }
}

private suspend fun handleSendEncryptedMessage(
    raw: String,
    senderUserId: String,
    senderDeviceId: String,
    registry: WebSocketSessionRegistry,
) {
    // TODO: parse SendEncryptedMessage, persist metadata to
    // messaging-service, then fan out per-device ciphertext to each
    // recipient's connected session(s) via registry.sendToDevice().
    // The server NEVER inspects perDeviceCiphertext contents — it
    // routes the opaque blobs by (recipientUserId, deviceId) only.
    log.debug("send_encrypted_message from userId=$senderUserId")
}

private suspend fun handleTypingIndicator(raw: String, senderUserId: String, registry: WebSocketSessionRegistry) {
    // TODO: parse, fan out to other chat members' connected sessions.
    // Typing indicators are ephemeral — not persisted, not delivered
    // if the recipient is currently offline.
}

private suspend fun handlePresenceUpdate(raw: String, userId: String, registry: WebSocketSessionRegistry) {
    // TODO: update presence in Redis (TTL-keyed so it auto-expires if
    // the client disconnects without sending Offline), fan out to
    // contacts' connected sessions.
}

private suspend fun handleReadReceipt(raw: String, userId: String, deviceId: String) {
    // TODO: update delivery_receipt in messaging-service, push receipt
    // event back to the original sender's connected session(s).
}

private suspend fun handleCallSignal(raw: String, userId: String, registry: WebSocketSessionRegistry) {
    // TODO: parse targetUserId + signalPayload, relay to target user's
    // connected session. Gateway never inspects signalPayload (opaque
    // SDP/ICE JSON for WebRTC — end-to-end encrypted at the media layer).
}

private suspend fun handleGhostModeToggle(raw: String, userId: String) {
    // TODO: update Ghost Mode flag in Redis for this user. While set,
    // presence_update events from contacts' queries for this user
    // return Invisible rather than the real presence state.
}

/**
 * In-process WebSocket session map + Redis-backed cross-instance
 * routing. On a single-node deployment only the in-process map is
 * used. On a multi-node deployment (Kubernetes horizontal scaling),
 * [sendToDevice] publishes to a Redis pub/sub channel; every gateway
 * instance subscribes and delivers to locally-connected sessions that
 * match the target (userId, deviceId).
 */
class WebSocketSessionRegistry(
    private val redisConnection: StatefulRedisConnection<String, String>,
) {
    // Local in-process sessions: (userId:deviceId) -> WebSocketSession
    private val localSessions = ConcurrentHashMap<String, DefaultWebSocketSession>()

    fun register(userId: String, deviceId: String, session: DefaultWebSocketSession) {
        localSessions["$userId:$deviceId"] = session
        // TODO: publish "device connected on this node" to Redis so other
        // nodes know where to route messages destined for this device.
    }

    fun unregister(userId: String, deviceId: String) {
        localSessions.remove("$userId:$deviceId")
    }

    suspend fun sendToDevice(userId: String, deviceId: String, frameText: String) {
        val session = localSessions["$userId:$deviceId"]
        if (session != null) {
            session.send(Frame.Text(frameText))
        } else {
            // Not connected to this node — publish to Redis pub/sub for
            // other nodes to pick up and deliver locally.
            // TODO: redisConnection.async().publish("ws:route:$userId:$deviceId", frameText)
        }
    }

    suspend fun sendToAllUserDevices(userId: String, frameText: String) {
        localSessions.entries
            .filter { it.key.startsWith("$userId:") }
            .forEach { (_, session) -> session.send(Frame.Text(frameText)) }
        // TODO: also publish to Redis for devices connected on other nodes.
    }
}
