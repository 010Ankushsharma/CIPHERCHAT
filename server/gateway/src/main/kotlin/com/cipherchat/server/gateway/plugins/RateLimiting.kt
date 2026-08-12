package com.cipherchat.server.gateway.plugins

import io.ktor.server.application.Application
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Rate limit tiers. Named rather than applied globally so individual
 * route groups can opt into the right tier:
 *
 *  [AUTH] — strictest, on login/register/OTP endpoints. Credential
 *    stuffing and OTP brute-force are the primary attack vectors for
 *    an auth system; 5 attempts per 5 minutes per IP matches common
 *    production hardening without being so aggressive it locks out
 *    legitimate users on shared IPs (NAT, office networks).
 *
 *  [API] — standard API rate limit. 120 requests/minute per
 *    authenticated user covers normal usage comfortably while
 *    preventing a compromised or buggy client from DOSing the server.
 *
 *  [WEBSOCKET] — connection establishment only (the WebSocket
 *    connection itself is long-lived; this limit is on the initial
 *    HTTP upgrade, not on messages sent over an established
 *    connection, which are rate-limited at the application layer
 *    in the WebSocket handler instead).
 */
val RateLimitName.Companion.AUTH get() = RateLimitName("auth")
val RateLimitName.Companion.API get() = RateLimitName("api")
val RateLimitName.Companion.WEBSOCKET get() = RateLimitName("websocket")

fun Application.configureRateLimiting() {
    install(RateLimit) {
        register(RateLimitName.AUTH) {
            rateLimiter(limit = 5, refillPeriod = 5.minutes)
        }
        register(RateLimitName.API) {
            rateLimiter(limit = 120, refillPeriod = 1.minutes)
        }
        register(RateLimitName.WEBSOCKET) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
        }
    }
}
