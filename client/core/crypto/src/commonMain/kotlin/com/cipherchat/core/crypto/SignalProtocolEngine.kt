package com.cipherchat.core.crypto

import com.cipherchat.core.domain.model.DeviceId
import com.cipherchat.core.domain.model.UserId

/**
 * The Signal Protocol engine: X3DH for initial key agreement, Double
 * Ratchet for per-message forward secrecy and post-compromise
 * security. This is the lowest-level crypto contract in CipherChat —
 * everything above it (core:network for transport, core:data for
 * orchestrating encrypt-then-send) calls into this, never around it.
 *
 * Threading/lifecycle note: implementations are expected to be
 * effectively single-threaded per (remoteUserId, remoteDeviceId) pair
 * — Double Ratchet state is a strict sequential state machine, and
 * concurrent encrypt/decrypt calls against the same session would
 * corrupt the ratchet. Implementations must serialize access
 * internally (e.g. a Mutex per session key) rather than push that
 * burden onto every caller.
 *
 * Nothing in this interface returns or accepts a domain-level
 * [com.cipherchat.core.domain.model.Message] or any @Serializable
 * type — only opaque [CipherText] / [PlainText] wrappers around raw
 * bytes, so it's structurally impossible to accidentally serialize a
 * key or a ratchet state alongside application data.
 */
interface SignalProtocolEngine {

    /**
     * Generates this device's long-term identity keypair, signed
     * prekey, and a batch of one-time prekeys. Called exactly once,
     * on first launch after a device is linked — never regenerated
     * for an existing device, since regenerating the identity key
     * would invalidate every Safety Number other users have verified.
     */
    suspend fun generateIdentity(): IdentityKeyBundle

    /** Refills the one-time prekey pool once the server reports it's running low. */
    suspend fun generatePrekeyBatch(count: Int): List<PrekeyBundle>

    /**
     * Performs the X3DH handshake against a remote device's published
     * prekey bundle, establishing a new Double Ratchet session. Safe
     * to call even if a session already exists for this remote
     * device — per Signal Protocol semantics this is a normal
     * "session reset" path (e.g. after [DeviceTrustState.Flagged]
     * re-verification).
     */
    suspend fun establishSession(
        remoteUserId: UserId,
        remoteDeviceId: DeviceId,
        remotePrekeyBundle: PrekeyBundle,
    ): SessionEstablishmentResult

    /** True if an active Double Ratchet session exists for this remote device, without performing any ratchet step. */
    suspend fun hasSession(remoteUserId: UserId, remoteDeviceId: DeviceId): Boolean

    /**
     * Encrypts [plainText] for the given remote device, advancing the
     * sending chain. Throws [NoSessionException] if no session has
     * been established yet — callers must call [establishSession]
     * first rather than have this method silently establish one,
     * keeping "did a handshake happen" an explicit, observable step.
     */
    suspend fun encrypt(
        remoteUserId: UserId,
        remoteDeviceId: DeviceId,
        plainText: PlainText,
    ): CipherText

    /**
     * Decrypts [cipherText] from the given remote device, advancing
     * the receiving chain (and the root key, if the message included
     * a new ratchet public key). Returns [DecryptResult.IdentityChanged]
     * rather than throwing if the sender's identity key no longer
     * matches what we have on file — that's a security-relevant event
     * the caller must surface to the user, never silently swallow or
     * silently re-trust.
     */
    suspend fun decrypt(
        remoteUserId: UserId,
        remoteDeviceId: DeviceId,
        cipherText: CipherText,
    ): DecryptResult

    suspend fun terminateSession(remoteUserId: UserId, remoteDeviceId: DeviceId)

    /** Computes the Safety Number / fingerprint for manual out-of-band verification. */
    suspend fun computeSafetyNumber(remoteUserId: UserId, remoteIdentityPublicKey: ByteArray): String
}

/** Opaque wrapper — intentionally not a typealias for ByteArray, so it can't be passed where a CipherText is expected and vice versa. */
class PlainText(val bytes: ByteArray)
class CipherText(val bytes: ByteArray)

data class IdentityKeyBundle(
    val identityPublicKey: ByteArray,
    val signedPrekeyPublic: ByteArray,
    val signedPrekeySignature: ByteArray,
    val oneTimePrekeysPublic: List<ByteArray>,
)

data class PrekeyBundle(
    val identityPublicKey: ByteArray,
    val signedPrekeyId: Int,
    val signedPrekeyPublic: ByteArray,
    val signedPrekeySignature: ByteArray,
    val oneTimePrekeyId: Int?,
    val oneTimePrekeyPublic: ByteArray?,
)

sealed class SessionEstablishmentResult {
    data object Success : SessionEstablishmentResult()
    data object InvalidSignature : SessionEstablishmentResult()
    data object PrekeyBundleExhausted : SessionEstablishmentResult()
}

sealed class DecryptResult {
    data class Success(val plainText: PlainText) : DecryptResult()
    /** Sender's identity key changed since we last saw them — surface for re-verification, never auto-trust. */
    data class IdentityChanged(val newIdentityPublicKey: ByteArray) : DecryptResult()
    data object DuplicateMessage : DecryptResult()
    data object MalformedCipherText : DecryptResult()
}

class NoSessionException(remoteUserId: UserId, remoteDeviceId: DeviceId) :
    Exception("No Double Ratchet session for user=${remoteUserId.value} device=${remoteDeviceId.value}")
