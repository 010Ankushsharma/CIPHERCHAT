package com.cipherchat.server.gateway.routing

import com.cipherchat.server.gateway.plugins.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

/**
 * Auth endpoints — all unauthenticated, all under the strict
 * [com.cipherchat.server.gateway.plugins.RateLimitName.Companion.AUTH]
 * rate limit tier applied in [configureRouting].
 *
 * Request/response DTOs are defined inline in this file (not in
 * :shared) because they're specific to the HTTP transport layer —
 * they don't need to be known by the WebSocket gateway or any other
 * service. Only types genuinely shared across multiple services
 * (e.g. the prekey bundle format used by both gateway and
 * messaging-service) live in :shared.
 */
fun Route.authRoutes() {
    route("/auth") {
        post("/login/email") {
            val req = call.receive<EmailLoginRequest>()
            req.validate()
            // TODO: delegate to auth-service via internal HTTP or
            // shared service interface injected via Koin.
            // Returns AuthTokenResponse on success, throws
            // AuthenticationException on invalid credentials.
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        post("/register/email") {
            val req = call.receive<EmailRegisterRequest>()
            req.validate()
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        post("/otp/request") {
            val req = call.receive<OtpRequestBody>()
            if (req.phoneNumber.isBlank()) throw ValidationException("phoneNumber is required")
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        post("/otp/verify") {
            val req = call.receive<OtpVerifyBody>()
            req.validate()
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        post("/oauth/{provider}") {
            val provider = call.parameters["provider"]
                ?: throw ValidationException("provider path param required")
            if (provider !in listOf("google", "apple", "github")) {
                throw ValidationException("Unsupported OAuth provider: $provider")
            }
            val req = call.receive<OAuthBody>()
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        post("/anonymous") {
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        post("/refresh") {
            val req = call.receive<RefreshTokenBody>()
            if (req.refreshToken.isBlank()) throw ValidationException("refreshToken is required")
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        post("/devices/link/qr") {
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }

        post("/devices/link/complete") {
            val req = call.receive<QrLinkCompleteBody>()
            if (req.qrToken.isBlank()) throw ValidationException("qrToken is required")
            call.respond(HttpStatusCode.NotImplemented, "auth-service not yet wired")
        }
    }
}

// --- Request DTOs ---

@Serializable
data class EmailLoginRequest(val email: String, val password: String) {
    fun validate() {
        if (!email.contains("@")) throw ValidationException("Invalid email address")
        if (password.length < 8) throw ValidationException("Password must be at least 8 characters")
    }
}

@Serializable
data class EmailRegisterRequest(val email: String, val password: String, val displayName: String) {
    fun validate() {
        if (!email.contains("@")) throw ValidationException("Invalid email address")
        if (password.length < 8) throw ValidationException("Password must be at least 8 characters")
        if (displayName.isBlank()) throw ValidationException("displayName is required")
    }
}

@Serializable data class OtpRequestBody(val phoneNumber: String)

@Serializable
data class OtpVerifyBody(val phoneNumber: String, val code: String) {
    fun validate() {
        if (phoneNumber.isBlank()) throw ValidationException("phoneNumber is required")
        if (code.length != 6 || !code.all { it.isDigit() }) throw ValidationException("code must be 6 digits")
    }
}

@Serializable data class OAuthBody(val token: String)
@Serializable data class RefreshTokenBody(val refreshToken: String)
@Serializable data class QrLinkCompleteBody(val qrToken: String)

// --- Response DTOs ---

@Serializable
data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMs: Long,
    val userId: String,
    val deviceId: String,
    val requiresNewDeviceVerification: Boolean = false,
)
