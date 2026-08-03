/*
 * androidApp — Build Script
 * -----------------------------------------------------------------------
 * The Android application shell. This module's ONLY job is:
 *   1. Declare the applicationId, version, and signing config
 *   2. Pull in every feature/core module as a dependency
 *   3. Wire Koin DI modules at the app level
 *   4. Host MainActivity which starts the Compose navigation
 *
 * No business logic, no UI components, no repositories belong here —
 * if a line in this module does anything beyond wiring, it's in the
 * wrong place. The shell is intentionally thin so the same logic is
 * reachable from iosApp and desktopApp without duplication.
 */

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget()
}

android {
    namespace = "com.cipherchat.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cipherchat.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Signing config wired via environment variables in CI —
            // never hardcode keystore credentials in build files.
        }
    }

    // Edge-to-edge rendering — CipherChat uses windowInsetsPadding
    // everywhere so the OS nav bar and status bar are handled in Compose,
    // not by the activity window. This allows the glassmorphism bottom
    // nav and top bar to render behind system UI correctly.
    buildFeatures { compose = true }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // Core modules
    implementation(projects.client.core.domain)
    implementation(projects.client.core.designsystem)
    implementation(projects.client.core.crypto)
    implementation(projects.client.core.database)
    implementation(projects.client.core.network)

    // Feature modules
    implementation(projects.client.feature.onboarding)
    implementation(projects.client.feature.auth)
    implementation(projects.client.feature.chat)

    // Android-specific
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // DI
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
}
