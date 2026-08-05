/*
 * core:domain — Build Script
 * -----------------------------------------------------------------------
 * This module is the innermost layer of Clean Architecture: pure
 * business logic with ZERO framework dependencies. No Android, no
 * Compose, no Ktor, no SQLDelight. Just:
 *
 *   - data classes (User, Chat, Message, Device, Session, ...)
 *   - repository INTERFACES (implementations live in core:data)
 *   - use cases (single-responsibility business operations)
 *
 * Why this matters for CipherChat specifically: encryption-related
 * business rules (e.g. "a message is never persisted in plaintext",
 * "a Session must be re-verified after a Safety Number change") belong
 * here as pure logic, testable without spinning up a database, a
 * network client, or a UI — and unable to accidentally depend on any
 * concrete crypto implementation.
 */

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // Targets: every platform CipherChat ships on.
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            // Needed for @Serializable domain models that cross module
            // boundaries (e.g. cached to disk, or passed to core:network
            // DTO mappers). The domain layer stays serialization-aware
            // but framework-agnostic — kotlinx.serialization is a
            // language-level library, not a platform framework.
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.cipherchat.core.domain"
    compileSdk = 35
    defaultConfig {
        minSdk = 26 // hardware-backed Keystore reliably available from 26+
    }
}
