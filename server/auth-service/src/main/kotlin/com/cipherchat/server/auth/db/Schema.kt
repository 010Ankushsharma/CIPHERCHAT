package com.cipherchat.server.auth.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

/**
 * Database schema for auth-service. Every table definition here maps
 * directly to the domain concepts in
 * [com.cipherchat.core.domain.model.User],
 * [com.cipherchat.core.domain.model.Device], and
 * [com.cipherchat.core.domain.model.AuthSession].
 *
 * Column naming uses snake_case to match PostgreSQL conventions —
 * Exposed maps to camelCase Kotlin properties in the DAO layer.
 */

object Users : Table("users") {
    val id = varchar("id", 36)                          // UUID
    val displayName = varchar("display_name", 100)
    val handle = varchar("handle", 50).uniqueIndex()    // @username — unique
    val email = varchar("email", 320).nullable().uniqueIndex()
    val phoneNumber = varchar("phone_number", 20).nullable().uniqueIndex()
    // Password hash stored as bcrypt output (60 chars). Never stored as
    // plaintext or reversible hash. NULL for OAuth-only and anonymous
    // accounts (no password to hash).
    val passwordHash = varchar("password_hash", 60).nullable()
    val identityPublicKey = text("identity_public_key") // base64 — from core:crypto's generateIdentity()
    val isVerified = bool("is_verified").default(false)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object OAuthAccounts : Table("oauth_accounts") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(Users.id)
    val provider = varchar("provider", 20)              // "google" | "apple" | "github"
    val providerAccountId = varchar("provider_account_id", 255)
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(provider, providerAccountId) }
}

object Devices : Table("devices") {
    val id = varchar("id", 36)
    val ownerId = varchar("owner_id", 36).references(Users.id)
    val name = varchar("name", 100)
    val platform = varchar("platform", 20)
    // Each device has its OWN identity public key (separate from the
    // user's account-level key in Users.identityPublicKey). Signal
    // Protocol's multi-device model: one user, N devices, N independent
    // key pairs — messages encrypted separately to each device.
    val publicKey = text("public_key")
    val trustState = varchar("trust_state", 20).default("unverified")
    val linkedAt = timestampWithTimeZone("linked_at")
    val lastActiveAt = timestampWithTimeZone("last_active_at")
    val approximateLocation = varchar("approximate_location", 100).nullable()

    override val primaryKey = PrimaryKey(id)
    init { index(false, ownerId) }
}

object AuthSessions : Table("auth_sessions") {
    val id = varchar("id", 36)
    val deviceId = varchar("device_id", 36).references(Devices.id)
    val userId = varchar("user_id", 36).references(Users.id)
    val issuedAt = timestampWithTimeZone("issued_at")
    val expiresAt = timestampWithTimeZone("expires_at")
    val lastSeenAt = timestampWithTimeZone("last_seen_at")
    val ipApproxLocation = varchar("ip_approx_location", 100).nullable()
    val loginMethod = varchar("login_method", 30)
    // Refresh token stored as bcrypt hash — same reason as passwords:
    // if the auth_sessions table leaks, raw refresh tokens must not be
    // reusable without the original value. Bcrypt adds ~100ms overhead
    // to refresh operations, which is acceptable (refresh happens
    // every 15 minutes at most, not on every request).
    val refreshTokenHash = varchar("refresh_token_hash", 60)

    override val primaryKey = PrimaryKey(id)
    init {
        index(false, userId)
        index(false, deviceId)
    }
}

object IdentityKeys : Table("identity_keys") {
    val deviceId = varchar("device_id", 36).references(Devices.id)
    val signedPrekeyPublic = text("signed_prekey_public")
    val signedPrekeySignature = text("signed_prekey_signature")
    val uploadedAt = timestampWithTimeZone("uploaded_at")

    override val primaryKey = PrimaryKey(deviceId)
}

object OneTimePrekeys : Table("one_time_prekeys") {
    val id = integer("id").autoIncrement()
    val deviceId = varchar("device_id", 36).references(Devices.id)
    val prekeyId = integer("prekey_id")             // client-assigned ID, not our autoincrement
    val publicKey = text("public_key")              // base64
    // consumed = true once fetched — never deleted immediately so we
    // can audit "how many prekeys has this device consumed" for abuse
    // detection. A background job sweeps consumed prekeys older than
    // 30 days. consumed_at is indexed for that sweep.
    val consumed = bool("consumed").default(false)
    val consumedAt = timestampWithTimeZone("consumed_at").nullable()
    val uploadedAt = timestampWithTimeZone("uploaded_at")

    override val primaryKey = PrimaryKey(id)
    init {
        uniqueIndex(deviceId, prekeyId)             // client IDs must be unique per device
        index(false, deviceId, consumed)            // fast "count available prekeys for device" query
        index(false, consumed, consumedAt)          // background sweep index
    }
}

object OtpCodes : Table("otp_codes") {
    val id = integer("id").autoIncrement()
    val phoneNumber = varchar("phone_number", 20)
    // Code hash, not plaintext — a compromised DB shouldn't let an
    // attacker replay OTP codes that haven't been used yet.
    val codeHash = varchar("code_hash", 60)
    val expiresAt = timestampWithTimeZone("expires_at")
    val used = bool("used").default(false)
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
    init { index(false, phoneNumber, used) }
}
