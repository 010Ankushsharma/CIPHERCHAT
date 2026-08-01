/*
 * CipherChat — Root Gradle Settings
 * -----------------------------------------------------------------------
 * This file wires together the entire client-side Kotlin Multiplatform
 * monorepo. The backend (Ktor) is a SEPARATE Gradle build under /server
 * (it has its own settings.gradle.kts) so client and server can be
 * versioned, built, and deployed independently.
 *
 * Module layout:
 *   core:*     -> platform-agnostic libraries shared by every feature
 *   feature:*  -> self-contained vertical slices of product functionality
 *   androidApp / iosApp / desktopApp -> the actual installable app shells
 */

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "CipherChat"

// ---------------------------------------------------------------------
// Core modules — no UI, no feature logic. Pure building blocks.
// ---------------------------------------------------------------------
include(":client:core:domain")          // models, repository interfaces, use cases
include(":client:core:data")            // repository implementations
include(":client:core:network")         // Ktor client, WebSocket session, DTOs
include(":client:core:crypto")          // Signal Protocol wrapper, key storage
include(":client:core:database")        // SQLDelight local persistence
include(":client:core:designsystem")    // colors, typography, spacing, components

// ---------------------------------------------------------------------
// Feature modules — one per major product surface
// ---------------------------------------------------------------------
include(":client:feature:onboarding")
include(":client:feature:auth")
include(":client:feature:chat")
include(":client:feature:calls")
include(":client:feature:ai")
include(":client:feature:settings")

// ---------------------------------------------------------------------
// App shells — platform entry points that assemble the modules above
// ---------------------------------------------------------------------
include(":client:androidApp")
include(":client:iosApp")
include(":client:desktopApp")
