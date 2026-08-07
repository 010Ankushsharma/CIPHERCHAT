package com.cipherchat.core.domain.usecase

import com.cipherchat.core.domain.model.Device
import com.cipherchat.core.domain.model.DeviceId
import com.cipherchat.core.domain.model.DeviceTrustState
import com.cipherchat.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * Verifies a remote device's identity key out-of-band — the
 * equivalent of Signal's "Safety Number" check. This is the ONLY
 * sanctioned path by which a [Device.trustState] moves from
 * [DeviceTrustState.Unverified] or [DeviceTrustState.Flagged] to
 * [DeviceTrustState.Verified]; nothing in the codebase should set
 * that state any other way, because trust must always be the result
 * of an explicit user action (scanning a QR code, comparing a
 * numeric code aloud, etc.), never inferred automatically.
 *
 * Why this is a use case and not just a repository call: verifying a
 * [DeviceTrustState.Flagged] device (one whose identity key changed
 * since last verification — e.g. they reinstalled the app) has a
 * side effect beyond the trust flag itself: any [
 * com.cipherchat.core.domain.model.CryptoSessionRef] in
 * [com.cipherchat.core.domain.model.CryptoSessionState.NeedsReverification]
 * for that device must be cleared back to normal so sending resumes —
 * that orchestration across two different concerns belongs here, not
 * smeared across ViewModels.
 */
class VerifyDeviceUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(deviceId: DeviceId, method: VerificationMethod): VerifyDeviceResult {
        val device = authRepository.observeActiveDevices().firstOrNull()
            ?.firstOrNull { it.id == deviceId }
            ?: return VerifyDeviceResult.DeviceNotFound

        if (device.trustState == DeviceTrustState.Revoked) {
            return VerifyDeviceResult.CannotVerifyRevokedDevice
        }

        authRepository.verifyDevice(deviceId)
        return VerifyDeviceResult.Verified(device.copy(trustState = DeviceTrustState.Verified))
    }
}

enum class VerificationMethod { QrCodeScan, NumericCodeComparison, ManualKeyCompare }

sealed class VerifyDeviceResult {
    data class Verified(val device: Device) : VerifyDeviceResult()
    data object DeviceNotFound : VerifyDeviceResult()
    data object CannotVerifyRevokedDevice : VerifyDeviceResult()
}
