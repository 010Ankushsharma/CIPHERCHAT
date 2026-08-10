package com.cipherchat.server.auth.service

import com.cipherchat.server.auth.db.IdentityKeys
import com.cipherchat.server.auth.db.OneTimePrekeys
import com.cipherchat.server.auth.db.dbQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

private val log = LoggerFactory.getLogger("PrekeyService")

/** Threshold below which the gateway sends a PrekeyPoolLow WS event to the client. */
const val PREKEY_LOW_WATER_MARK = 20

class PrekeyService {

    suspend fun uploadIdentityBundle(
        deviceId: String,
        signedPrekeyPublic: String,
        signedPrekeySignature: String,
        oneTimePrekeys: List<PrekeyUploadItem>,
    ) {
        dbQuery {
            // Upsert signed prekey — client may re-upload on app reinstall.
            val existing = IdentityKeys.select { IdentityKeys.deviceId eq deviceId }.firstOrNull()
            if (existing == null) {
                IdentityKeys.insert {
                    it[IdentityKeys.deviceId] = deviceId
                    it[IdentityKeys.signedPrekeyPublic] = signedPrekeyPublic
                    it[IdentityKeys.signedPrekeySignature] = signedPrekeySignature
                    it[uploadedAt] = OffsetDateTime.now()
                }
            } else {
                IdentityKeys.update({ IdentityKeys.deviceId eq deviceId }) {
                    it[IdentityKeys.signedPrekeyPublic] = signedPrekeyPublic
                    it[IdentityKeys.signedPrekeySignature] = signedPrekeySignature
                    it[uploadedAt] = OffsetDateTime.now()
                }
            }

            // Insert new one-time prekeys — skip any with IDs that
            // already exist (client retry safety).
            val existingIds = OneTimePrekeys
                .select { OneTimePrekeys.deviceId eq deviceId }
                .map { it[OneTimePrekeys.prekeyId] }
                .toSet()

            oneTimePrekeys
                .filter { it.prekeyId !in existingIds }
                .forEach { prekey ->
                    OneTimePrekeys.insert {
                        it[OneTimePrekeys.deviceId] = deviceId
                        it[prekeyId] = prekey.prekeyId
                        it[publicKey] = prekey.publicKey
                        it[uploadedAt] = OffsetDateTime.now()
                    }
                }
        }
        log.info("Uploaded ${oneTimePrekeys.size} prekeys for deviceId=$deviceId")
    }

    /**
     * Fetches AND atomically marks consumed one one-time prekey for
     * [deviceId]. Returns null if the pool is exhausted (caller should
     * fall back to signed prekey only, and send a PrekeyPoolLow event
     * to the device owner so they refill promptly).
     *
     * The SELECT + UPDATE must be atomic — done inside a single
     * Exposed transaction so two concurrent callers establishing
     * sessions with the same device can never consume the same prekey.
     */
    suspend fun consumeOneTimePrekey(deviceId: String): PrekeyBundle? = dbQuery {
        val row = OneTimePrekeys.select {
            (OneTimePrekeys.deviceId eq deviceId) and (OneTimePrekeys.consumed eq false)
        }.limit(1).firstOrNull() ?: return@dbQuery null

        OneTimePrekeys.update({ OneTimePrekeys.id eq row[OneTimePrekeys.id] }) {
            it[consumed] = true
            it[consumedAt] = OffsetDateTime.now()
        }

        val identityRow = IdentityKeys.select { IdentityKeys.deviceId eq deviceId }
            .firstOrNull() ?: return@dbQuery null

        PrekeyBundle(
            deviceId = deviceId,
            signedPrekeyPublic = identityRow[IdentityKeys.signedPrekeyPublic],
            signedPrekeySignature = identityRow[IdentityKeys.signedPrekeySignature],
            oneTimePrekeyId = row[OneTimePrekeys.prekeyId],
            oneTimePrekeyPublic = row[OneTimePrekeys.publicKey],
        )
    }

    suspend fun availablePrekeyCount(deviceId: String): Int = dbQuery {
        OneTimePrekeys.select {
            (OneTimePrekeys.deviceId eq deviceId) and (OneTimePrekeys.consumed eq false)
        }.count().toInt()
    }

    suspend fun isPrekeyPoolLow(deviceId: String): Boolean =
        availablePrekeyCount(deviceId) < PREKEY_LOW_WATER_MARK
}

data class PrekeyUploadItem(val prekeyId: Int, val publicKey: String)

data class PrekeyBundle(
    val deviceId: String,
    val signedPrekeyPublic: String,
    val signedPrekeySignature: String,
    val oneTimePrekeyId: Int?,
    val oneTimePrekeyPublic: String?,
)
