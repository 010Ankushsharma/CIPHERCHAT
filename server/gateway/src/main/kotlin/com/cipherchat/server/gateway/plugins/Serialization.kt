package com.cipherchat.server.gateway.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            // ignoreUnknownKeys = false (default) — the server REJECTS
            // requests with unknown fields rather than silently ignoring
            // them. In production this catches client-server version
            // mismatches early rather than silently accepting malformed
            // payloads that could later cause data inconsistencies.
            ignoreUnknownKeys = false
            // Explicit nulls serialized in responses — clients should
            // never have to guess whether a missing field means null or
            // "field not included in this API version."
            explicitNulls = true
            prettyPrint = false // never in production — wastes bandwidth
        })
    }
}
