package com.cipherchat.server.gateway.routing

import com.cipherchat.server.gateway.plugins.RateLimitName.Companion.API
import com.cipherchat.server.gateway.plugins.RateLimitName.Companion.AUTH
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Mounts all REST route groups. Structure:
 *
 *   /v1/auth/**          — unauthenticated, strict rate limit
 *   /v1/devices/**       — JWT-authenticated, standard rate limit
 *   /v1/users/**         — JWT-authenticated, standard rate limit
 *   /v1/chats/**         — JWT-authenticated, standard rate limit
 *   /v1/messages/**      — JWT-authenticated, standard rate limit
 *
 * All routes are under /v1/ — explicit versioning from day one so a
 * future /v2/ can be introduced without breaking existing clients,
 * which is critical for a mobile app where users don't always update
 * immediately.
 *
 * Route handlers are in separate files (AuthRoutes.kt, DeviceRoutes.kt
 * etc.) rather than all in this file — keeps each file focused on one
 * resource domain, and makes it easy to find the handler for any
 * specific endpoint without scrolling through hundreds of lines.
 */
fun Application.configureRouting() {
    routing {
        route("/v1") {
            // Unauthenticated — auth endpoints under strict rate limit
            rateLimit(AUTH) {
                authRoutes()
            }

            // Authenticated — all other endpoints under standard rate limit
            authenticate("jwt-bearer") {
                rateLimit(API) {
                    deviceRoutes()
                    userRoutes()
                    chatRoutes()
                    messageRoutes()
                }
            }
        }
    }
}
