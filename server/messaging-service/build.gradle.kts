/*
 * messaging-service — Build Script
 * -----------------------------------------------------------------------
 * Stores message METADATA and opaque ciphertext blobs (never plaintext).
 * Publishes message events to Kafka for fan-out to recipient devices.
 *
 * Kafka is used here rather than direct WebSocket delivery from this
 * service because the gateway is horizontally scaled — any gateway
 * instance might hold the recipient's WebSocket connection. Kafka
 * decouples "store + emit event" from "deliver to connected client":
 * the messaging-service stores and publishes, every gateway instance
 * subscribes and delivers to locally-connected sessions it owns.
 * This is the standard fan-out pattern for horizontally-scaled
 * real-time systems (used by Discord, Slack, and similar at scale).
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.postgresql.driver)
    implementation(libs.hikari)

    implementation(libs.kafka.clients)
    implementation(libs.lettuce.core)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback.classic)

    testImplementation(libs.kotlin.test.junit5)
}
