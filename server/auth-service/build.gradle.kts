/*
 * auth-service — Build Script
 * -----------------------------------------------------------------------
 * Handles: user registration/login, password hashing, JWT issuance,
 * device/session management, and prekey storage.
 *
 * This module is a LIBRARY, not a standalone server — it exposes
 * service interfaces that the gateway wires via Koin, not its own
 * Ktor application. This keeps the service boundary clean: the
 * gateway owns HTTP/WebSocket concerns, auth-service owns auth
 * business logic. If auth-service ever needs to become a true
 * microservice (separate process, internal gRPC), the interface
 * boundary is already in the right place — only the Koin binding
 * in the gateway changes, not the service code itself.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":shared"))

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.postgresql.driver)
    implementation(libs.hikari)

    // Redis — session store + prekey pool low-water-mark tracking
    implementation(libs.lettuce.core)

    // Auth primitives
    implementation(libs.bcrypt)
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // Serialization / Coroutines
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback.classic)

    testImplementation(libs.kotlin.test.junit5)
}
