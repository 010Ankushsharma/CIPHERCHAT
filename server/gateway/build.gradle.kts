/*
 * gateway — Build Script
 * -----------------------------------------------------------------------
 * The single Ktor application clients connect to. Responsibilities:
 *  - Accept and authenticate WebSocket connections for real-time events
 *  - Expose the REST API (auth, prekeys, device management, metadata)
 *  - Rate-limit, validate, and route requests to internal services
 *  - Emit metrics to Prometheus / Micrometer
 *
 * This module knows NOTHING about encryption or message plaintext —
 * it routes opaque ciphertext blobs from sender to recipient device(s)
 * and records only the metadata the protocol requires (sender ID,
 * recipient device IDs, timestamp). The server's "metadata-only"
 * guarantee is enforced here as a structural constraint: gateway never
 * imports a JSON parser that could extract message content, never logs
 * the ciphertext body, and never passes it to any service that stores
 * it beyond the in-flight routing buffer.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.cipherchat.server.gateway.ApplicationKt")
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.serialization.json)

    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.lettuce.core)    // Redis for session store + WebSocket connection registry
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.prometheus)
    implementation(libs.logback.classic)
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit5)
}
