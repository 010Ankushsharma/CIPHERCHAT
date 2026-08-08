/*
 * feature:chat — Build Script
 * -----------------------------------------------------------------------
 * The core product surface: Home Screen (chat list + tabs), individual
 * chat screen (message bubbles + input), and real-time message delivery.
 * This is the most dependency-heavy feature module because it genuinely
 * needs most of the core layer — but it still depends on INTERFACES
 * (core:domain repositories, use cases) rather than implementations
 * (core:data, core:network directly), keeping the architecture clean.
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
            implementation(projects.client.core.designsystem)
            implementation(projects.client.core.domain)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.animation)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.coil.compose)
            implementation(libs.haze)
        }
    }
}

android {
    namespace = "com.cipherchat.feature.chat"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}
