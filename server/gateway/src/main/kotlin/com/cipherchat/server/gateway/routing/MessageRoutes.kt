package com.cipherchat.server.gateway.routing

import com.cipherchat.server.gateway.plugins.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

/**
 * Chat metadata and message history endpoints.
 *
 * IMPORTANT: real-time message delivery is NOT here — it happens over
 * WebSocket via [com.cipherchat.server.gateway.websocket.configureWebSockets].
 * These REST endpoints serve:
 *  - Chat metadata (create group, list chats, archive)
 *  - Message history pagination (loading older messages on scroll)
 *  - Delivery receipt acknowledgement (fallback for missed WS events)
 *
 * The server stores only metadata + opaque ciphertext here — never
 * plaintext. [MessageMetadataRow] intentionally has no "body" field.
 */
fun Route.chatRoutes() {
    route("/chats") {
        get {
            // Returns chat list metadata (id, title, last message preview)
            // — NOT message content. Content is decrypted client-side.
            call.respond(HttpStatusCode.NotImplemented, "messaging-service not yet wired")
        }

        post {
            val req = call.receive<CreateChatRequest>()
            if (req.memberIds.isEmpty()) throw ValidationException("memberIds cannot be empty")
            call.respond(HttpStatusCode.NotImplemented, "messaging-service not yet wired")
        }

        post("/{chatId}/archive") {
            call.respond(HttpStatusCode.NotImplemented, "messaging-service not yet wired")
        }

        post("/{chatId}/members") {
            val req = call.receive<AddMembersRequest>()
            if (req.userIds.isEmpty()) throw ValidationException("userIds cannot be empty")
            call.respond(HttpStatusCode.NotImplemented, "messaging-service not yet wired")
        }
    }
}

fun Route.messageRoutes() {
    route("/chats/{chatId}/messages") {
        get {
            // Paginated message history — returns encrypted ciphertext
            // blobs + metadata (sender, timestamp, status). Client
            // decrypts locally. beforeMessageId used as a cursor.
            val chatId = call.parameters["chatId"]
                ?: throw ValidationException("chatId path param required")
            val beforeMessageId = call.request.queryParameters["beforeMessageId"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
            call.respond(HttpStatusCode.NotImplemented, "messaging-service not yet wired")
        }

        post("/{messageId}/receipts") {
            // Delivery/read receipt — called when client confirms
            // receipt of a message it processed from history (supplements
            // the WebSocket receipt path for messages received while offline).
            val messageId = call.parameters["messageId"]
                ?: throw ValidationException("messageId path param required")
            val req = call.receive<ReceiptUpdate>()
            call.respond(HttpStatusCode.OK)
        }
    }
}

@Serializable
data class CreateChatRequest(
    val kind: String, // "one_to_one" | "group" | "channel" | "community"
    val memberIds: List<String>,
    val title: String? = null,
)

@Serializable data class AddMembersRequest(val userIds: List<String>)
@Serializable data class ReceiptUpdate(val status: String) // "delivered" | "read"

@Serializable
data class MessageMetadataRow(
    val messageId: String,
    val chatId: String,
    val senderUserId: String,
    val senderDeviceId: String,
    val ciphertext: String, // base64 — client decrypts
    val sentAtEpochMs: Long,
    // NO plaintext body field — intentional, not an oversight
)
