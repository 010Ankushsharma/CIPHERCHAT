package com.cipherchat.server.gateway

import com.cipherchat.server.gateway.websocket.WebSocketSessionRegistry
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import org.koin.dsl.module

/**
 * Koin module for the gateway. Bindings are declared as [single] (one
 * instance per Koin container lifetime) for anything that maintains
 * state (Redis connections, WebSocket registry) and [factory] for
 * anything that should be created fresh per injection site (currently
 * nothing — all gateway dependencies are inherently stateful).
 */
val gatewayKoinModule = module {
    single<StatefulRedisConnection<String, String>> {
        val redisUrl = System.getenv("REDIS_URL") ?: "redis://localhost:6379"
        RedisClient.create(redisUrl).connect()
    }

    single {
        WebSocketSessionRegistry(redisConnection = get())
    }
}
