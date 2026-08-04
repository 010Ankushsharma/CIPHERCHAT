/*
 * core:database — Build Script
 * -----------------------------------------------------------------------
 * Local, offline-first persistence via SQLDelight. This is the ONE
 * place in the entire client where decrypted message plaintext is
 * allowed to be written to disk (so the chat list and message history
 * work instantly offline, without re-decrypting from the network on
 * every app launch) — and it is NEVER written to a plain unencrypted
 * SQLite file.
 *
 * Encryption-at-rest strategy: the actual SQLite file on disk is
 * encrypted via SQLCipher (a drop-in encrypted SQLite implementation
 * — not a custom encryption layer we wrote). The SQLCipher database
 * key itself is generated once and stored in core:crypto's
 * SecureKeyStore under KeyNamespace.LocalDatabaseEncryptionKey — so
 * this module depends on core:crypto purely to ASK for that key at
 * startup, never to perform any cryptographic operation itself.
 *
 * Why cache plaintext at all instead of re-decrypting from
 * core:crypto on every read: Double Ratchet message keys are
 * single-use and discarded immediately after one decryption (that's
 * what gives forward secrecy) — there is no way to "re-decrypt" a
 * message later even if we wanted to. The plaintext produced at
 * decrypt time MUST be persisted somewhere for message history to
 * exist at all; SQLCipher is that somewhere, encrypted at the
 * filesystem level under a key that itself never leaves secure
 * hardware-backed storage.
 */

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.sqldelight)
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

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }

        androidMain.dependencies {
            implementation("app.cash.sqldelight:android-driver:2.0.2")
            // SQLCipher's Android driver replaces the stock SQLite
            // driver so every connection is transparently encrypted.
            implementation("net.zetetic:android-database-sqlcipher:4.5.6")
        }

        val iosMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.0.2")
                // iOS SQLCipher comes via CocoaPods (SQLCipher pod) wired
                // through the iosApp target's Podfile, not a Kotlin dep —
                // see client/iosApp/Podfile (created when we build that module).
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
            }
        }
    }
}

sqldelight {
    databases {
        create("CipherChatDatabase") {
            packageName.set("com.cipherchat.core.database")
        }
    }
}

android {
    namespace = "com.cipherchat.core.database"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
}
