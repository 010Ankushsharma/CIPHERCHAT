package com.cipherchat.core.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Represents a CipherChat user identity.
 *
 * Design notes:
 * - [id] is a stable, server-issued identifier — never the phone number
 *   or email itself, so contact methods can rotate without breaking
 *   references across the system (and so leaking [id] alone leaks
 *   nothing personally identifying).
 * - [identityPublicKey] is the user's long-term Signal Protocol identity
 *   key (public half only — the private half never leaves secure
 *   storage on-device, see core:crypto). It's what other clients pin
 *   for Safety Number / device-verification.
 * - Presence ([presence]) is intentionally a sealed type, not a raw
 *   boolean "online", because CipherChat supports Smart Presence
 *   (Working / Driving / Sleeping / Custom) and Ghost Mode (invisible).
 */
@Serializable
data class User(
    val id: UserId,
    val displayName: String,
    val handle: String,                  // e.g. "@jane" — unique, mutable
    val avatarUrl: String? = null,
    val identityPublicKey: String,        // base64-encoded public key
    val presence: Presence = Presence.Offline,
    val statusMessage: String? = null,
    val isVerified: Boolean = false,      // digital identity card verification badge
    val createdAt: Instant,
)

@Serializable
@JvmInline
value class UserId(val value: String)

/**
 * Presence is modeled as a sealed hierarchy rather than a boolean so
 * Smart Presence and Ghost Mode are first-class, not bolted on.
 */
@Serializable
sealed class Presence {
    @Serializable
    data object Offline : Presence()

    @Serializable
    data object Online : Presence()

    /** Ghost Mode: user is active but has opted out of broadcasting it. */
    @Serializable
    data object Invisible : Presence()

    @Serializable
    data class Custom(
        val label: String,                // "In Meeting", "Coding", "Driving"...
        val emoji: String? = null,
        val autoDetected: Boolean = false, // true if AI inferred this, not user-set
    ) : Presence()
}
