/*
 * feature:onboarding — Build Script
 * -----------------------------------------------------------------------
 * The animated, multi-page onboarding flow: Welcome -> Privacy First
 * -> End-to-End Encryption -> Multi Device -> AI Features ->
 * Customization -> Finish.
 *
 * Depends on core:designsystem (all visual building blocks) and
 * core:domain (so the "Finish" page can eventually call into auth use
 * cases / save the user's customization choices) — but explicitly
 * NOT on core:network, core:crypto, or core:database directly. Any
 * actual data operation this feature needs should go through a
 * core:domain use case/repository interface, injected via Koin, not
 * by reaching past the architecture's boundaries directly into
 * lower-level modules just because it's onboarding and "feels simple."
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
            implementation(libs.lottie.compose)
        }
    }
}

android {
    namespace = "com.cipherchat.feature.onboarding"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
}
