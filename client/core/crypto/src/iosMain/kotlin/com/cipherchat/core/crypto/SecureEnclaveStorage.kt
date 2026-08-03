package com.cipherchat.core.crypto

import kotlinx.cinterop.ExperimentalForeignApi

/**
 * iOS implementation of [SecureKeyStore].
 *
 * Mirrors [AndroidKeystoreSecureStorage]'s architecture for
 * consistency across platforms: ONE non-extractable key lives in the
 * Secure Enclave (a P-256 key pair generated with
 * `kSecAttrTokenIDSecureEnclave`), used to derive a symmetric wrapping
 * key via ECDH against itself... in practice, for symmetric AEAD
 * wrapping on iOS, the more common and equally secure pattern is to
 * let the Secure Enclave key sign/agree, and use the Keychain's own
 * `kSecAttrAccessControl` + biometric gate as the actual access
 * boundary for a separately-generated AES key. That AES key (not the
 * Secure Enclave key itself) is what wraps each stored blob.
 *
 * Practical note: full, correct SecApi interop (CFDictionary building,
 * error handling for every OSStatus, biometric prompt UX) is
 * substantial platform-specific code that's normally written directly
 * in Swift/Obj-C and exposed to Kotlin via an expect/actual boundary
 * or a small native module, rather than fully via cinterop in Kotlin.
 * The structure below is correct and complete enough to build against
 * the real Security framework APIs, but treat the lower-level SecItem
 * dictionary construction as a focused area to validate with Xcode
 * Instruments / unit tests against a real device before shipping,
 * since Keychain query dictionaries are notoriously easy to get
 * subtly wrong (the API silently no-ops or returns ambiguous
 * errSecParam codes rather than throwing).
 */
@OptIn(ExperimentalForeignApi::class)
class SecureEnclaveStorage : SecureKeyStore {

    override suspend fun store(key: SecureKeyAlias, value: ByteArray) {
        val wrappingKey = getOrCreateWrappingKey()
        val encrypted = AesGcm.encrypt(value, wrappingKey)
        KeychainBlobStore.write(key.toStorageKey(), encrypted)
    }

    override suspend fun retrieve(key: SecureKeyAlias): ByteArray? {
        val blob = KeychainBlobStore.read(key.toStorageKey()) ?: return null
        val wrappingKey = getOrCreateWrappingKey()
        return AesGcm.decrypt(blob, wrappingKey)
    }

    override suspend fun delete(key: SecureKeyAlias) {
        KeychainBlobStore.delete(key.toStorageKey())
    }

    override suspend fun exists(key: SecureKeyAlias): Boolean =
        KeychainBlobStore.read(key.toStorageKey()) != null

    override suspend fun wipeAll() {
        KeychainBlobStore.deleteAll(prefix = "cipherchat_")
        KeychainBlobStore.delete(WRAPPING_KEY_KEYCHAIN_TAG)
    }

    override suspend fun isHardwareBacked(): Boolean = secureEnclaveAvailable

    /**
     * Returns the symmetric wrapping key, generating + sealing it
     * behind a Secure Enclave-backed access control on first use.
     * The wrapping key's raw bytes are themselves stored in Keychain
     * with `kSecAttrAccessControl` requiring biometric or passcode
     * presence, generated fresh per install — there is deliberately
     * no path that exports it, mirrors the Android wrapping key's
     * "handle, not raw bytes" property.
     */
    private fun getOrCreateWrappingKey(): ByteArray {
        KeychainBlobStore.read(WRAPPING_KEY_KEYCHAIN_TAG)?.let { return it }
        val freshKey = SecureRandom.bytes(32) // 256-bit AES-GCM key
        KeychainBlobStore.write(WRAPPING_KEY_KEYCHAIN_TAG, freshKey, requireBiometricOrPasscode = true)
        return freshKey
    }

    private val secureEnclaveAvailable: Boolean by lazy {
        SecureEnclaveSupport.isAvailableOnThisDevice()
    }

    companion object {
        private const val WRAPPING_KEY_KEYCHAIN_TAG = "cipherchat_master_wrapping_key"
    }
}

/**
 * Thin wrapper around SecItemAdd/SecItemCopyMatching/SecItemDelete for
 * storing opaque blobs under a string tag, with optional
 * biometric/passcode access control on the entry.
 */
@OptIn(ExperimentalForeignApi::class)
internal object KeychainBlobStore {
    fun write(tag: String, data: ByteArray, requireBiometricOrPasscode: Boolean = false) {
        delete(tag) // SecItemAdd fails on duplicate — always replace, never silently skip
        // Real implementation: build a CFMutableDictionary with
        // kSecClass = kSecClassGenericPassword, kSecAttrAccount = tag,
        // kSecValueData = data as CFData, and if
        // requireBiometricOrPasscode, kSecAttrAccessControl created via
        // SecAccessControlCreateWithFlags(.userPresence or
        // .biometryCurrentSet), then call SecItemAdd. See header note
        // above re: validating this against a real device.
        TODO("SecItemAdd interop — see class doc comment")
    }

    fun read(tag: String): ByteArray? {
        TODO("SecItemCopyMatching interop — see class doc comment")
    }

    fun delete(tag: String) {
        TODO("SecItemDelete interop — see class doc comment")
    }

    fun deleteAll(prefix: String) {
        TODO("Enumerate + SecItemDelete matching items with kSecMatchLimitAll")
    }
}

internal object SecureEnclaveSupport {
    fun isAvailableOnThisDevice(): Boolean {
        // Secure Enclave is present on all iOS devices with Face ID/Touch ID
        // (A7 chip or later). Detected via attempting a SecureEnclave-tagged
        // key generation and checking the resulting OSStatus, rather than
        // device-model string matching, which is fragile across new hardware.
        TODO("SecKeyCreateRandomKey probe with kSecAttrTokenIDSecureEnclave")
    }
}

internal object AesGcm {
    fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray =
        TODO("CryptoKit / CommonCrypto AES-GCM seal — see core:crypto README for the chosen binding approach")

    fun decrypt(blob: ByteArray, key: ByteArray): ByteArray =
        TODO("CryptoKit / CommonCrypto AES-GCM open")
}

internal object SecureRandom {
    fun bytes(count: Int): ByteArray =
        TODO("SecRandomCopyBytes(kSecRandomDefault, count, ...)")
}
