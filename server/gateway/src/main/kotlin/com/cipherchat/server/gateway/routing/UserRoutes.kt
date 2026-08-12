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

fun Route.userRoutes() {
    route("/users") {
        get("/me") {
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        post("/me/presence") {
            val req = call.receive<PresenceUpdateRequest>()
            if (req.presenceLabel.isBlank()) throw ValidationException("presenceLabel required")
            // Fan out to connected WebSocket clients who have this user
            // in their contact list via the WebSocketSessionRegistry.
            call.respond(HttpStatusCode.OK)
        }

        get("/search") {
            val query = call.request.queryParameters["q"]
                ?: throw ValidationException("q query param required")
            if (query.length < 2) throw ValidationException("Search query must be at least 2 characters")
            call.respond(HttpStatusCode.NotImplemented, "user search not yet wired")
        }

        get("/{userId}") {
            val userId = call.parameters["userId"]
                ?: throw ValidationException("userId path param required")
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }
    }
}

@Serializable data class PresenceUpdateRequest(val presenceLabel: String)
