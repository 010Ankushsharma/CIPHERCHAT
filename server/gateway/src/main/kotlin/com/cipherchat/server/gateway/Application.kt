package com.cipherchat.server.gateway

import com.cipherchat.server.gateway.auth.configureAuthentication
import com.cipherchat.server.gateway.plugins.configureMetrics
import com.cipherchat.server.gateway.plugins.configureRateLimiting
import com.cipherchat.server.gateway.plugins.configureSerialization
import com.cipherchat.server.gateway.plugins.configureStatusPages
import com.cipherchat.server.gateway.routing.configureRouting
import com.cipherchat.server.gateway.websocket.configureWebSockets
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("CipherChatGateway")

/**
 * Application entry point. Intentionally a thin orchestrator:
 * it installs Ktor plugins and delegates everything else to
 * focused module-level functions. No business logic here — if a line
 * in this file does anything more complex than call a configure*()
 * function or install a plugin, it's in the wrong place.
 */
fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val host = System.getenv("HOST") ?: "0.0.0.0"

    log.info("Starting CipherChat Gateway on $host:$port")

    embeddedServer(
        factory = Netty,
        port = port,
        host = host,
        module = Application::module,
    ).start(wait = true)
}

fun Application.module() {
    // Dependency injection — Koin modules registered here, before any
    // other plugin that might try to inject a dependency at install time.
    install(Koin) {
        modules(gatewayKoinModule)
    }

    // Core plugins — order matters:
    // 1. Serialization first (other plugins may produce JSON responses)
    // 2. Authentication (needed by routing)
    // 3. Rate limiting (must come before routing, wraps route handlers)
    // 4. Status pages (catches exceptions thrown by route handlers)
    // 5. Routing (the outermost handler registration)
    // 6. WebSockets (separate from HTTP routing)
    // 7. Metrics (last — wraps everything to measure it)
    configureSerialization()
    configureAuthentication()
    configureRateLimiting()
    configureStatusPages()
    configureRouting()
    configureWebSockets()
    configureMetrics()
}
