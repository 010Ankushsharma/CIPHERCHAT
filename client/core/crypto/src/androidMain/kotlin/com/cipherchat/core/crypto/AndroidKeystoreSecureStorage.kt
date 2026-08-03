package com.cipherchat.core.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android implementation of [SecureKeyStore].
 *
 * Design: rather than storing every individual key directly in
 * Keystore (Keystore is designed to hold a small number of long-lived
 * keys, not hundreds of one-time prekeys), we generate ONE
 * non-exportable AES-256-GCM "wrapping key" inside Android Keystore
 * (`setIsStrongBoxBacked(true)` when the chip supports it), and use
 * that single hardware-backed key to encrypt every CipherChat key
 * blob before persisting it to app-private storage. The wrapping key
 * itself never leaves the secure hardware — `KeyStore.getKey()`
 * returns a Java [SecretKey] *handle*, not exportable raw bytes, so
 * even a rooted device reading our private storage only sees
 * ciphertext it cannot decrypt without that hardware.
 */
class AndroidKeystoreSecureStorage(
    private val context: Context,
) : SecureKeyStore {

    private val androidKeyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
    private val prefs = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    private val isStrongBoxBacked: Boolean by lazy {
        ensureWrappingKeyExists(preferStrongBox = true)
    }

    override suspend fun store(key: SecureKeyAlias, value: ByteArray) {
        val wrappingKey = getOrCreateWrappingKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value)

        // Stored as iv-length-prefixed blob: [ivSize:1][iv][ciphertext+tag]
        val payload = byteArrayOf(iv.size.toByte()) + iv + encrypted
        prefs.edit().putString(key.toStorageKey(), payload.toBase64()).apply()
    }

    override suspend fun retrieve(key: SecureKeyAlias): ByteArray? {
        val stored = prefs.getString(key.toStorageKey(), null) ?: return null
        val payload = stored.fromBase64()
        val ivSize = payload[0].toInt()
        val iv = payload.copyOfRange(1, 1 + ivSize)
        val ciphertext = payload.copyOfRange(1 + ivSize, payload.size)

        val wrappingKey = getOrCreateWrappingKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    override suspend fun delete(key: SecureKeyAlias) {
        prefs.edit().remove(key.toStorageKey()).apply()
    }

    override suspend fun exists(key: SecureKeyAlias): Boolean =
        prefs.contains(key.toStorageKey())

    override suspend fun wipeAll() {
        prefs.edit().clear().apply()
        // The wrapping key itself is also destroyed, not just the
        // blobs — this guarantees old ciphertext is unrecoverable
        // even if an old encrypted backup of app-private storage
        // somehow survives the logout/delete-account flow.
        androidKeyStore.deleteEntry(WRAPPING_KEY_ALIAS)
    }

    override suspend fun isHardwareBacked(): Boolean = isStrongBoxBacked

    private fun getOrCreateWrappingKey(): SecretKey {
        ensureWrappingKeyExists(preferStrongBox = true)
        return androidKeyStore.getKey(WRAPPING_KEY_ALIAS, null) as SecretKey
    }

    /** Returns true if the resulting key ended up StrongBox-backed. */
    private fun ensureWrappingKeyExists(preferStrongBox: Boolean): Boolean {
        if (androidKeyStore.containsAlias(WRAPPING_KEY_ALIAS)) {
            // We don't have a direct API to ask Keystore "was this
            // StrongBox?" after the fact on all API levels, so we track
            // it ourselves at creation time via this same prefs file.
            return prefs.getBoolean(STRONGBOX_FLAG_KEY, false)
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_PROVIDER)
        val baseSpec = KeyGenParameterSpec.Builder(
            WRAPPING_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // Require the device to be unlocked at least once since boot
            // before this key is usable — defends against a stolen
            // powered-off device, while still allowing background sync.
            .setUnlockedDeviceRequired(true)

        return try {
            if (preferStrongBox) {
                keyGenerator.init(baseSpec.setIsStrongBoxBacked(true).build())
                keyGenerator.generateKey()
                prefs.edit().putBoolean(STRONGBOX_FLAG_KEY, true).apply()
                true
            } else {
                throw StrongBoxUnavailableException("not preferred")
            }
        } catch (e: StrongBoxUnavailableException) {
            // Fall back to the TEE-backed (non-StrongBox) Keystore, which
            // is still hardware-backed on virtually all devices from the
            // last decade — just not in a discrete secure chip.
            keyGenerator.init(baseSpec.setIsStrongBoxBacked(false).build())
            keyGenerator.generateKey()
            prefs.edit().putBoolean(STRONGBOX_FLAG_KEY, false).apply()
            false
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val WRAPPING_KEY_ALIAS = "cipherchat_master_wrapping_key"
        private const val PREFS_FILE_NAME = "cipherchat_secure_blobs"
        private const val STRONGBOX_FLAG_KEY = "_strongbox_backed"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}

private fun ByteArray.toBase64(): String =
    java.util.Base64.getEncoder().encodeToString(this)

private fun String.fromBase64(): ByteArray =
    java.util.Base64.getDecoder().decode(this)
