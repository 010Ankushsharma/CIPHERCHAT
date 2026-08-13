/*
 * CipherChat Server — Root Gradle Settings
 * -----------------------------------------------------------------------
 * This is a SEPARATE Gradle build from the client (client/settings.gradle.kts).
 * Client and server are intentionally decoupled at the build level so
 * they can be versioned, built, and deployed independently — the server
 * doesn't need to know the client's Compose/KMP version, and the client
 * doesn't need the server's database drivers on its classpath.
 *
 * Module layout:
 *   shared          -> DTOs, domain models, and constants shared across services
 *   gateway         -> Ktor WebSocket + REST gateway (the single entry point
 *                      clients connect to; routes to internal services)
 *   auth-service    -> Authentication, device/session management, prekey store
 *   messaging-service -> Message routing, metadata storage (NEVER plaintext),
 *                      media upload coordination
 *
 * Future services (not yet declared):
 *   notification-service  -> FCM/APNs push notification dispatch
 *   media-service         -> Encrypted media upload/download proxy
 *   presence-service      -> Online status, Smart Presence fan-out
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "CipherChatServer"

include(":shared")
include(":gateway")
include(":auth-service")
include(":messaging-service")
