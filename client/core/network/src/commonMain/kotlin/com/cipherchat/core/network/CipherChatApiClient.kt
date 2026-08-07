package com.cipherchat.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.ContentType
import kotlinx.serialization.Serializable

/**
 * REST client for everything that ISN'T real-time push (that's
 * [MessageSocketSession]'s job). Covers: auth, prekey bundle
 * publish/fetch (needed for X3DH handshakes against a device that
 * isn't currently connected), and device/session management.
 *
 * Every method returns [ApiResult] rather than throwing on expected
 * failure modes (401, 404, network unreachable) — exceptions are
 * reserved for genuinely unexpected conditions, so callers in
 * core:data can pattern-match exhaustively instead of wrapping every
 * call in try/catch for control flow.
 */
class CipherChatApiClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val authTokenProvider: suspend () -> String?,
) {
    // --- Auth ---

    suspend fun loginWithEmail(email: String, password: String): ApiResult<AuthResponseDto> =
        post("/v1/auth/login/email", LoginEmailRequestDto(email, password), authenticated = false)

    suspend fun requestPhoneOtp(phoneNumber: String): ApiResult<Unit> =
        post("/v1/auth/otp/request", PhoneRequestDto(phoneNumber), authenticated = false)

    suspend fun verifyPhoneOtp(phoneNumber: String, code: String): ApiResult<AuthResponseDto> =
        post("/v1/auth/otp/verify", OtpVerifyRequestDto(phoneNumber, code), authenticated = false)

    suspend fun loginWithOAuth(provider: String, token: String): ApiResult<AuthResponseDto> =
        post("/v1/auth/oauth/$provider", OAuthRequestDto(token), authenticated = false)

    suspend fun createAnonymousSession(): ApiResult<AuthResponseDto> =
        post("/v1/auth/anonymous", Unit, authenticated = false)

    suspend fun refreshToken(refreshToken: String): ApiResult<AuthResponseDto> =
        post("/v1/auth/refresh", RefreshRequestDto(refreshToken), authenticated = false)

    // --- Identity / prekey bundles (X3DH) ---

    /** Publishes this device's identity key + signed prekey + one-time prekeys so others can establish sessions with us. */
    suspend fun publishIdentityBundle(bundle: IdentityBundleDto): ApiResult<Unit> =
        post("/v1/devices/me/keys", bundle)

    suspend fun publishPrekeyBatch(prekeys: List<PrekeyDto>): ApiResult<Unit> =
        post("/v1/devices/me/keys/prekeys", PrekeyBatchDto(prekeys))

    /** Fetches a remote device's current prekey bundle to begin an X3DH handshake. Server returns and consumes one one-time prekey per fetch. */
    suspend fun fetchPrekeyBundle(userId: String, deviceId: String): ApiResult<PrekeyBundleDto> =
        get("/v1/users/$userId/devices/$deviceId/prekey-bundle")

    suspend fun fetchAllDeviceBundles(userId: String): ApiResult<List<PrekeyBundleDto>> =
        get("/v1/users/$userId/devices/prekey-bundles")

    // --- Device / session management ---

    suspend fun listActiveDevices(): ApiResult<List<DeviceDto>> =
        get("/v1/devices")

    suspend fun renameCurrentDevice(name: String): ApiResult<Unit> =
        post("/v1/devices/me/rename", RenameDeviceRequestDto(name))

    suspend fun logoutDevice(deviceId: String): ApiResult<Unit> =
        delete("/v1/devices/$deviceId")

    suspend fun logoutAllOtherDevices(): ApiResult<Unit> =
        post("/v1/devices/logout-others", Unit)

    suspend fun verifyDevice(deviceId: String): ApiResult<Unit> =
        post("/v1/devices/$deviceId/verify", Unit)

    suspend fun generateDeviceLinkQrToken(): ApiResult<QrLinkTokenDto> =
        post("/v1/devices/link/qr", Unit)

    suspend fun completeDeviceLinkFromQr(qrToken: String): ApiResult<AuthResponseDto> =
        post("/v1/devices/link/complete", QrLinkCompleteRequestDto(qrToken), authenticated = false)

    // --- internal request helpers ---

    private suspend inline fun <reified TReq, reified TRes> post(
        path: String,
        body: TReq,
        authenticated: Boolean = true,
    ): ApiResult<TRes> = safeCall {
        httpClient.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            if (authenticated) attachAuth()
            setBody(body)
        }
    }

    private suspend inline fun <reified TRes> get(path: String): ApiResult<TRes> = safeCall {
        httpClient.get("$baseUrl$path") { attachAuth() }
    }

    private suspend fun delete(path: String): ApiResult<Unit> = safeCall {
        httpClient.delete("$baseUrl$path") { attachAuth() }
    }

    private suspend fun io.ktor.client.request.HttpRequestBuilder.attachAuth() {
        authTokenProvider()?.let { header("Authorization", "Bearer $it") }
    }

    private suspend inline fun <reified T> safeCall(block: suspend () -> HttpResponse): ApiResult<T> {
        return try {
            val response = block()
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created -> ApiResult.Success(response.body<T>())
                HttpStatusCode.Unauthorized -> ApiResult.Unauthorized
                HttpStatusCode.NotFound -> ApiResult.NotFound
                HttpStatusCode.TooManyRequests -> ApiResult.RateLimited
                else -> ApiResult.ServerError(response.status.value)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Unknown network error")
        }
    }
}

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data object Unauthorized : ApiResult<Nothing>()
    data object NotFound : ApiResult<Nothing>()
    data object RateLimited : ApiResult<Nothing>()
    data class ServerError(val statusCode: Int) : ApiResult<Nothing>()
    data class NetworkError(val message: String) : ApiResult<Nothing>()
}

// --- DTOs (wire format — mapped to core:domain models in core:data) ---

@Serializable data class LoginEmailRequestDto(val email: String, val password: String)
@Serializable data class PhoneRequestDto(val phoneNumber: String)
@Serializable data class OtpVerifyRequestDto(val phoneNumber: String, val code: String)
@Serializable data class OAuthRequestDto(val token: String)
@Serializable data class RefreshRequestDto(val refreshToken: String)
@Serializable data class RenameDeviceRequestDto(val name: String)
@Serializable data class QrLinkCompleteRequestDto(val qrToken: String)

@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMs: Long,
    val userId: String,
    val deviceId: String,
    val requiresNewDeviceVerification: Boolean = false,
)

@Serializable
data class IdentityBundleDto(
    val identityPublicKey: String, // base64
    val signedPrekeyPublic: String,
    val signedPrekeySignature: String,
    val oneTimePrekeysPublic: List<String>,
)

@Serializable data class PrekeyDto(val prekeyId: Int, val publicKey: String)
@Serializable data class PrekeyBatchDto(val prekeys: List<PrekeyDto>)

@Serializable
data class PrekeyBundleDto(
    val userId: String,
    val deviceId: String,
    val identityPublicKey: String,
    val signedPrekeyId: Int,
    val signedPrekeyPublic: String,
    val signedPrekeySignature: String,
    val oneTimePrekeyId: Int? = null,
    val oneTimePrekeyPublic: String? = null,
)

@Serializable
data class DeviceDto(
    val deviceId: String,
    val name: String,
    val platform: String,
    val trustState: String,
    val linkedAtEpochMs: Long,
    val lastActiveAtEpochMs: Long,
    val approximateLocation: String? = null,
)

@Serializable data class QrLinkTokenDto(val token: String, val expiresAtEpochMs: Long)
