/*
 * core:crypto — Build Script
 * -----------------------------------------------------------------------
 * This module is the security backbone of CipherChat: the Signal
 * Protocol wrapper (X3DH key agreement + Double Ratchet for forward
 * secrecy) and platform-backed secure key storage.
 *
 * HARD RULES for this module, enforced by convention (and reviewed in
 * code review, not just trusted to be remembered):
 *
 *   1. NEVER implement raw cryptographic primitives (AEAD ciphers,
 *      curve math, KDFs) from scratch. We bind to libsodium, an
 *      audited, battle-tested C library, via Kotlin Multiplatform
 *      bindings. Hand-rolled crypto is a primary source of real-world
 *      vulnerabilities and is never justified by "we only need it for
 *      this one thing."
 *   2. This module depends ONLY on core:domain (for typed IDs like
 *      UserId/DeviceId) — never on core:network, core:data, or any
 *      UI/feature module. Key material must never be reachable from
 *      a code path that also touches the network or a database
 *      query, even indirectly, to keep the blast radius of any other
 *      module's bug contained.
 *   3. No private key, root key, or chain key type in this module is
 *      ever annotated @Serializable. If it can be serialized, it can
 *      be accidentally logged, cached to disk in plaintext, or sent
 *      over the wire. Persistence of encrypted key blobs is handled
 *      via opaque ByteArray blobs written straight to platform secure
 *      storage (Keystore / Secure Enclave), never through SQLDelight
 *      or any general-purpose serializer.
 */

plugins {
    alias(libs.plugins.kotlinMultiplatform)
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
            implementation(libs.libsodium.bindings)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            // Android Keystore access (hardware-backed key storage) is
            // wrapped here; see KeystoreSecureStorage in androidMain.
        }

        val iosMain by getting {
            dependencies {
                // Secure Enclave / Keychain access via platform interop;
                // see SecureEnclaveStorage in iosMain.
            }
        }
    }
}

android {
    namespace = "com.cipherchat.core.crypto"
    compileSdk = 35
    defaultConfig {
        minSdk = 26 // StrongBox/hardware-backed Keystore reliability floor
    }
}
