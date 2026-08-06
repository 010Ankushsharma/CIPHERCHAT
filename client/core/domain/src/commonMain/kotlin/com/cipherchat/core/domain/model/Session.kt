package com.cipherchat.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * An authenticated login session for a [Device]. This is the model
 * behind the Session Manager screen ("Active Devices", "Logout All
 * Devices") and Login Alerts.
 *
 * Distinct from [CryptoSessionRef] below: an [AuthSession] is about
 * "is this device allowed to act as this user right now" (auth/API
 * access), while a [CryptoSessionRef] is about "do we have an
 * established Double Ratchet session to encrypt to this device."
 * A device can be authenticated without messages being encryptable to
 * it yet (e.g. right after linking, before the first X3DH handshake
 * completes) — keeping these separate prevents that distinction from
 * collapsing into one ambiguous "isActive" flag.
 */
@Serializable
data class AuthSession(
    val id: SessionId,
    val deviceId: DeviceId,
    val userId: UserId,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val lastSeenAt: Instant,
    val ipApproxLocation: String? = null,  // coarse only, for login alerts
    val loginMethod: LoginMethod,
)

@Serializable
@JvmInline
value class SessionId(val value: String)

@Serializable
enum class LoginMethod {
    Email, Password, Phone, OtpSms, GoogleOAuth, AppleOAuth, GitHubOAuth,
    AnonymousSecure, Passkey, QrDeviceLink,
}

/**
 * Reference to an established (or pending) Signal Protocol session
 * between the current device and a specific remote device. The actual
 * Double Ratchet chain keys, root key, and X3DH-derived shared secret
 * NEVER appear here or anywhere in core:domain — they live exclusively
 * in core:crypto's secure, non-serializable session store.
 *
 * This type exists purely so higher layers (e.g. a chat screen showing
 * "encrypting..." or a Safety Number banner) can reason about session
 * state without ever touching key material.
 */
@Serializable
data class CryptoSessionRef(
    val remoteUserId: UserId,
    val remoteDeviceId: DeviceId,
    val state: CryptoSessionState,
    val establishedAt: Instant? = null,
    val lastRatchetAt: Instant? = null,
)

@Serializable
enum class CryptoSessionState {
    /** No X3DH handshake performed yet — first message to this device will trigger one. */
    NotEstablished,
    /** X3DH complete, Double Ratchet active — normal sending/receiving state. */
    Established,
    /** Remote identity key changed since we last verified — block sending until re-verified. */
    NeedsReverification,
    /** Device was revoked/logged out remotely — session torn down. */
    Terminated,
}
