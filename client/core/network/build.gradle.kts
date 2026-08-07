/*
 * core:network — Build Script
 * -----------------------------------------------------------------------
 * Wraps the Ktor HTTP client and WebSocket session used for:
 *  - REST calls (auth, device/prekey management, chat/group metadata)
 *  - A persistent WebSocket connection for real-time message delivery,
 *    typing indicators, presence updates, and call signaling
 *
 * Architectural boundary: this module knows about DTOs (wire-format
 * JSON shapes) and ciphertext — it does NOT know about core:domain's
 * rich sealed types like MessageContent. The flow is:
 *
 *   Wire JSON --(this module: DTO)--> ciphertext bytes
 *     --(core:crypto: decrypt)--> plaintext bytes
 *     --(core:data: parse + map)--> core:domain Message
 *
 * core:network depends on core:crypto only for the OPAQUE CipherText/
 * PlainText wrapper types (so DTOs can hold "this field is ciphertext"
 * as a type, not just a ByteArray that could be mistaken for
 * plaintext) — never for SignalProtocolEngine itself. Encrypting and
 * decrypting happens in core:data, which orchestrates both this
 * module and core:crypto; keeping that orchestration out of
 * core:network means this module stays a dumb, swappable transport
 * layer with no business logic and no security-sensitive operations
 * of its own.
 */

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(projects.client.core.domain)
            implementation(projects.client.core.crypto)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.auth)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.ktor:ktor-client-mock:3.0.1")
        }

        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.0.1")
        }

        val iosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:3.0.1")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:3.0.1")
            }
        }
    }
}

android {
    namespace = "com.cipherchat.core.network"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
}
