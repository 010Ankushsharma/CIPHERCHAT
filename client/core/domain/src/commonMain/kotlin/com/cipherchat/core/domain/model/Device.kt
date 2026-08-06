package com.cipherchat.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Represents one device linked to a [UserId] under Signal Protocol's
 * multi-device model: each device has its OWN identity/signed-prekey
 * pair, and messages are encrypted separately to every active device
 * rather than synced as plaintext between them.
 *
 * This model is intentionally "dumb" — it only describes metadata
 * needed for the UI (Session Manager, login alerts, unknown-device
 * warnings) and trust state. It holds NO private key material; that
 * lives only in core:crypto's secure storage, never serialized,
 * never sent to the server, never put in a data class that could be
 * accidentally logged.
 */
@Serializable
data class Device(
    val id: DeviceId,
    val ownerId: UserId,
    val name: String,                     // "Jane's iPhone 16", user-editable
    val platform: DevicePlatform,
    val publicKey: String,                // base64 — this device's identity public key
    val trustState: DeviceTrustState,
    val linkedAt: Instant,
    val lastActiveAt: Instant,
    val isCurrentDevice: Boolean = false,
    val approximateLocation: String? = null, // coarse, e.g. "Mumbai, IN" — for login alerts only, never precise
)

@Serializable
@JvmInline
value class DeviceId(val value: String)

@Serializable
enum class DevicePlatform {
    ANDROID, IOS, DESKTOP_MAC, DESKTOP_WINDOWS, DESKTOP_LINUX, WEB
}

/**
 * Drives "Unknown Device Detection" and the Safety Number flow.
 * A device starts at [Unverified] the moment it's linked (e.g. via QR
 * code) and only becomes [Verified] once the user explicitly confirms
 * it out-of-band — mirroring Signal's safety-number verification, so
 * trust is never silently assumed.
 */
@Serializable
enum class DeviceTrustState {
    Unverified,
    Verified,
    /** Identity key changed since last verification — must re-verify before sending. */
    Flagged,
    Revoked,
}
