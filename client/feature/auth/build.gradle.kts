/*
 * feature:auth — Build Script
 * -----------------------------------------------------------------------
 * The login/registration flow: Email+Password, Phone+OTP, Google/
 * Apple/GitHub OAuth, Anonymous Secure Session, Passkeys, and QR
 * device linking — every method from
 * [com.cipherchat.core.domain.repository.AuthRepository].
 *
 * Depends on core:domain (for AuthRepository and the use cases this
 * module's ViewModels call) and core:designsystem (UI), but NOT on
 * core:network or core:crypto directly — this module asks core:domain
 * interfaces to do things, it never reaches past them into how those
 * things actually happen. The concrete AuthRepository implementation
 * (which DOES touch core:network/core:crypto) lives in core:data and
 * is wired in via Koin at the app-shell level, not referenced here by
 * type.
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
            implementation("cafe.adriel.voyager:voyager-screenmodel:1.1.0-beta03")
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "com.cipherchat.feature.auth"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
}
