package com.cipherchat.server.gateway.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

/**
 * JWT access token configuration. Two token types:
 *
 *  Access token  — short-lived (15 min), carried in Authorization
 *    header for every API request. Contains userId + deviceId claims.
 *    Short expiry limits the window a stolen token is usable without
 *    needing to invalidate individual tokens server-side (which would
 *    require a token blocklist and Redis lookup on every request).
 *
 *  Refresh token — longer-lived (30 days), used only at
 *    /v1/auth/refresh to get a new access token. Stored server-side
 *    in Redis so it CAN be invalidated (logout, device revocation)
 *    without touching the access token at all. The asymmetry (access
 *    = stateless short-lived, refresh = stateful long-lived) gives
 *    both performance (no Redis on 99% of requests) and revocability
 *    (logout works within 15 minutes via refresh token invalidation).
 */
fun Application.configureAuthentication() {
    val jwtSecret = System.getenv("JWT_SECRET")
        ?: error("JWT_SECRET env var must be set — never hardcode a secret in source code")
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: "cipherchat-gateway"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "cipherchat-clients"

    val algorithm = Algorithm.HMAC256(jwtSecret)
    val verifier = JWT.require(algorithm)
        .withIssuer(jwtIssuer)
        .withAudience(jwtAudience)
        .build()

    install(Authentication) {
        jwt("jwt-bearer") {
            realm = "CipherChat API"
            verifier(verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                val deviceId = credential.payload.getClaim("deviceId").asString()
                if (userId != null && deviceId != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null // null = auth failed → 401
                }
            }
        }
    }
}

/** Convenience extension — extracts userId claim from a validated JWT principal. */
val JWTPrincipal.userId: String
    get() = payload.getClaim("userId").asString()
        ?: error("userId claim missing from validated JWT — should be impossible")

/** Convenience extension — extracts deviceId claim from a validated JWT principal. */
val JWTPrincipal.deviceId: String
    get() = payload.getClaim("deviceId").asString()
        ?: error("deviceId claim missing from validated JWT — should be impossible")
