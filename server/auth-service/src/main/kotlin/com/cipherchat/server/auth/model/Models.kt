package com.cipherchat.server.auth.model

import kotlinx.serialization.Serializable

/**
 * The token pair returned after successful authentication. Contains
 * both the short-lived access token and the long-lived refresh token.
 * The refresh token is returned ONCE here and never stored in
 * plaintext again — only its bcrypt hash persists in the database.
 */
@Serializable
data class AuthTokenPair(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAtEpochMs: Long,
    val userId: String,
    val deviceId: String,
)

/**
 * Device metadata sent by the client on login/register. The gateway
 * extracts this from the request body and passes it to AuthService
 * so every auth operation also registers or recognises the device,
 * giving us multi-device tracking from the very first request rather
 * than as a separate step.
 */
@Serializable
data class DeviceInfo(
    val name: String,
    val platform: String,
    val identityPublicKey: String,
)

/**
 * Minimal user record returned by auth operations — only what the
 * gateway needs to build a response. Full profile lives separately.
 */
@Serializable
data class UserRecord(
    val id: String,
    val displayName: String,
    val handle: String,
    val email: String?,
    val phoneNumber: String?,
    val isVerified: Boolean,
)
