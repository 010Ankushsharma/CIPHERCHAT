package com.cipherchat.server.gateway.routing

import com.cipherchat.server.gateway.auth.userId
import com.cipherchat.server.gateway.auth.deviceId
import com.cipherchat.server.gateway.plugins.NotFoundException
import com.cipherchat.server.gateway.plugins.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

/**
 * Device management + prekey endpoints.
 *
 * Prekey endpoints are performance-critical: every new X3DH session
 * establishment (first message to a device) fetches one one-time
 * prekey here, consuming it permanently. The server must:
 *  1. Return a prekey atomically (fetch + mark consumed in one
 *     transaction) so two concurrent requests can't get the same
 *     prekey — using the same one-time prekey twice breaks the
 *     forward-secrecy guarantee of X3DH.
 *  2. Alert the client (via [WsServerEvent.PrekeyPoolLow]) when the
 *     pool drops below a threshold so the client uploads more before
 *     it hits zero — once the pool is empty, fallback to signed prekey
 *     only (still secure, but loses the one-time prekey's extra
 *     deniability property).
 */
fun Route.deviceRoutes() {
    route("/devices") {
        get {
            val principal = call.principal<JWTPrincipal>()!!
            // TODO: fetch devices for principal.userId from auth-service
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        post("/me/rename") {
            val req = call.receive<RenameDeviceRequest>()
            if (req.name.isBlank()) throw ValidationException("name is required")
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        delete("/{deviceId}") {
            val targetDeviceId = call.parameters["deviceId"]
                ?: throw ValidationException("deviceId path param required")
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        post("/logout-others") {
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        post("/{deviceId}/verify") {
            val targetDeviceId = call.parameters["deviceId"]
                ?: throw ValidationException("deviceId path param required")
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        // Prekey management
        route("/me/keys") {
            post {
                val bundle = call.receive<IdentityBundleUpload>()
                bundle.validate()
                // TODO: store in auth-service's prekey table.
                // Idempotent — re-uploading the same signed prekey is
                // fine (client may retry on network failure).
                call.respond(HttpStatusCode.OK)
            }

            post("/prekeys") {
                val batch = call.receive<PrekeyBatchUpload>()
                if (batch.prekeys.isEmpty()) throw ValidationException("prekeys list cannot be empty")
                if (batch.prekeys.size > 100) throw ValidationException("Maximum 100 prekeys per batch")
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    // Fetch remote device's prekey bundle for X3DH session establishment.
    // Under /users/{userId}/devices/{deviceId} rather than /devices/
    // because it's a read operation scoped to another user's device,
    // not an operation on your own devices.
    route("/users/{userId}/devices") {
        get("/{deviceId}/prekey-bundle") {
            val targetUserId = call.parameters["userId"]
                ?: throw ValidationException("userId path param required")
            val targetDeviceId = call.parameters["deviceId"]
                ?: throw ValidationException("deviceId path param required")
            // TODO: fetch + atomically consume one-time prekey from auth-service
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        get("/prekey-bundles") {
            val targetUserId = call.parameters["userId"]
                ?: throw ValidationException("userId path param required")
            // Returns bundles for ALL active devices of targetUserId —
            // used when sending a first message to someone to establish
            // sessions with every device they have in one round trip.
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }
    }
}

@Serializable data class RenameDeviceRequest(val name: String)

@Serializable
data class IdentityBundleUpload(
    val identityPublicKey: String,
    val signedPrekeyPublic: String,
    val signedPrekeySignature: String,
    val oneTimePrekeysPublic: List<String>,
) {
    fun validate() {
        if (identityPublicKey.isBlank()) throw ValidationException("identityPublicKey required")
        if (signedPrekeyPublic.isBlank()) throw ValidationException("signedPrekeyPublic required")
        if (signedPrekeySignature.isBlank()) throw ValidationException("signedPrekeySignature required")
    }
}

@Serializable data class PrekeyUpload(val prekeyId: Int, val publicKey: String)
@Serializable data class PrekeyBatchUpload(val prekeys: List<PrekeyUpload>)
