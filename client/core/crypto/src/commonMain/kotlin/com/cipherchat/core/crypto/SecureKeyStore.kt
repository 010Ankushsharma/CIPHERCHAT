package com.cipherchat.core.crypto

/**
 * Contract for storing key material in hardware-backed secure storage.
 * Implementations live per-platform:
 *  - androidMain: Android Keystore (StrongBox-backed where available)
 *  - iosMain: Secure Enclave + Keychain
 *  - desktopMain: OS keychain (Keychain on macOS, Credential Manager on
 *    Windows, libsecret on Linux) — see DesktopSecureKeyStore
 *
 * This interface is the ONLY way [SignalProtocolEngine] implementations
 * touch persisted key material. It never returns raw key bytes for
 * storage elsewhere — [retrieve] returns bytes for immediate
 * in-memory use (e.g. one ratchet step), and callers must not cache
 * them beyond that operation's scope.
 *
 * All values are stored as opaque, already-encrypted-at-rest blobs
 * from the OS's perspective; this interface adds no encryption of its
 * own — it delegates entirely to the platform's hardware-backed store,
 * which is the appropriate trust boundary (avoids a second, redundant,
 * easier-to-get-wrong software encryption layer on top of one already
 * provided correctly by the OS).
 */
interface SecureKeyStore {

    suspend fun store(key: SecureKeyAlias, value: ByteArray)

    /** Returns null if no value exists for [key] — never throws for the "not found" case. */
    suspend fun retrieve(key: SecureKeyAlias): ByteArray?

    suspend fun delete(key: SecureKeyAlias)

    suspend fun exists(key: SecureKeyAlias): Boolean

    /** Wipes ALL CipherChat key material from this store. Used only by "Logout" and "Delete Account" flows. */
    suspend fun wipeAll()

    /**
     * True if this platform's secure storage is backed by dedicated
     * security hardware (StrongBox / Secure Enclave / TPM) rather than
     * a software-only fallback. Surfaced in Settings > Encryption so
     * users on older/lower-end hardware know their actual security
     * posture rather than assuming hardware backing universally.
     */
    suspend fun isHardwareBacked(): Boolean
}

/**
 * Typed alias for a stored key, scoped by [namespace] so different
 * concerns (identity keys vs. per-session ratchet state vs. local
 * database encryption key) can never collide even if a raw string
 * key were accidentally reused.
 */
data class SecureKeyAlias(val namespace: KeyNamespace, val identifier: String) {
    fun toStorageKey(): String = "${namespace.prefix}:$identifier"
}

enum class KeyNamespace(val prefix: String) {
    IdentityKey("identity"),
    SignedPrekey("signed_prekey"),
    OneTimePrekey("otp_prekey"),
    RatchetSessionState("ratchet_session"),
    LocalDatabaseEncryptionKey("db_key"),
    BackupEncryptionKey("backup_key"),
}
