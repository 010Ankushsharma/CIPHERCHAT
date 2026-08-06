package com.cipherchat.core.domain.repository

import com.cipherchat.core.domain.model.AuthSession
import com.cipherchat.core.domain.model.Device
import com.cipherchat.core.domain.model.DeviceId
import com.cipherchat.core.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Contract for authentication and device/session management,
 * implemented by core:data. Every method that succeeds in
 * establishing identity returns an [AuthSession] bound to the
 * CURRENT device — core:crypto is responsible for generating that
 * device's identity keypair on first login, this layer never sees
 * private key material, only the resulting session/device records.
 */
interface AuthRepository {

    /** Null if no session is currently active on this device. */
    fun observeCurrentSession(): Flow<AuthSession?>

    fun observeCurrentUser(): Flow<User?>

    // --- Standard credential-based login ---
    suspend fun loginWithEmail(email: String, password: String): AuthResult

    suspend fun registerWithEmail(email: String, password: String, displayName: String): AuthResult

    suspend fun requestPhoneOtp(phoneNumber: String): OtpRequestResult

    suspend fun verifyPhoneOtp(phoneNumber: String, code: String): AuthResult

    // --- OAuth ---
    suspend fun loginWithGoogle(idToken: String): AuthResult

    suspend fun loginWithApple(idToken: String): AuthResult

    suspend fun loginWithGitHub(oauthCode: String): AuthResult

    // --- Privacy-first options ---
    suspend fun createAnonymousSecureSession(): AuthResult

    /** WebAuthn/FIDO2-backed passwordless login. */
    suspend fun loginWithPasskey(): AuthResult

    suspend fun registerPasskey(): PasskeyRegistrationResult

    // --- Multi-device / QR linking ---
    /** Generates a QR payload the primary device displays for a new device to scan. */
    suspend fun generateDeviceLinkQrPayload(): String

    /** Called on the NEW device after scanning a QR shown by an already-authenticated device. */
    suspend fun completeDeviceLinkFromQr(qrPayload: String): AuthResult

    // --- Session / device management (Session Manager screen) ---
    fun observeActiveDevices(): Flow<List<Device>>

    suspend fun logoutDevice(deviceId: DeviceId)

    suspend fun logoutAllOtherDevices()

    suspend fun logoutCurrentDevice()

    suspend fun renameCurrentDevice(name: String)

    /** Marks a device as explicitly trusted after out-of-band safety-number verification. */
    suspend fun verifyDevice(deviceId: DeviceId)

    suspend fun revokeDevice(deviceId: DeviceId)
}

sealed class AuthResult {
    data class Success(val user: User, val session: AuthSession) : AuthResult()
    data class RequiresOtp(val phoneNumber: String) : AuthResult()
    data class RequiresNewDeviceVerification(val deviceId: DeviceId) : AuthResult()
    data class Failure(val reason: AuthFailureReason) : AuthResult()
}

enum class AuthFailureReason {
    InvalidCredentials, AccountNotFound, NetworkError, OtpExpired, OtpIncorrect,
    RateLimited, OAuthCancelled, PasskeyUnavailable, Unknown,
}

sealed class OtpRequestResult {
    data object Sent : OtpRequestResult()
    data class Failure(val reason: AuthFailureReason) : OtpRequestResult()
}

sealed class PasskeyRegistrationResult {
    data object Success : PasskeyRegistrationResult()
    data class Failure(val reason: AuthFailureReason) : PasskeyRegistrationResult()
}
