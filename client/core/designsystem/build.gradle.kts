/*
 * core:designsystem — Build Script
 * -----------------------------------------------------------------------
 * Holds the visual language of CipherChat: color tokens, typography
 * scale, spacing scale, shape/elevation tokens, and reusable Compose
 * components (buttons, cards, the message bubble, etc.).
 *
 * This is deliberately the ONLY core:* module that depends on Compose.
 * core:domain/crypto/network/database stay UI-framework-free so they
 * remain testable in plain JVM unit tests and reusable if CipherChat
 * ever needed a non-Compose surface (e.g. a CLI admin tool against the
 * same domain layer). Every feature module depends on this one for
 * anything visual — no feature module should hardcode a raw Color(...)
 * or sp/dp value; it should reference a token from here, so a future
 * rebrand or theme change touches one module, not forty screens.
 */

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.animation)
            implementation(compose.components.resources)

            implementation(libs.haze)
            implementation(libs.coil.compose)
            implementation(libs.lottie.compose)
        }
    }
}

android {
    namespace = "com.cipherchat.core.designsystem"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
}
