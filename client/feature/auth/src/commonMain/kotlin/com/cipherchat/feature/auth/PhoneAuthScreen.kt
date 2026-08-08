package com.cipherchat.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cipherchat.core.designsystem.CcRadius
import com.cipherchat.core.designsystem.CcSpacing
import com.cipherchat.core.designsystem.CcTextStyles
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme
import com.cipherchat.core.designsystem.components.CcGhostButton
import com.cipherchat.core.designsystem.components.CcPrimaryButton
import com.cipherchat.core.designsystem.components.CcTextField
import kotlinx.coroutines.delay

/**
 * Phone authentication: step 1 (enter number) → step 2 (enter OTP).
 * Both steps live in one screen as a local [PhoneAuthStep] state
 * machine — the OTP step has no independent navigation entry point
 * and is always reached through step 1, so splitting into two Voyager
 * screens would add navigation complexity for no benefit. The
 * slide animation between steps gives visual continuity even though
 * the content changes significantly.
 *
 * The 6-digit OTP entry uses a custom segmented display rather than a
 * plain text field — each digit gets its own bordered box, so the user
 * can see exactly which position they're filling and the expected
 * length is unambiguous. This pattern is standard in production SMS-
 * OTP flows because it eliminates the "did I enter 6 or 7 digits?"
 * confusion that plagues plain text fields for numeric codes.
 */
class PhoneAuthScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val colorScheme = LocalCipherChatColorScheme.current

        var step by remember { mutableStateOf(PhoneAuthStep.EnterPhone) }
        var phoneNumber by remember { mutableStateOf("") }
        var otpCode by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var resendCountdown by remember { mutableStateOf(0) }

        // Countdown timer for "Resend code" — starts at 30s when OTP
        // step is entered, prevents immediate spam-resend.
        LaunchedEffect(step) {
            if (step == PhoneAuthStep.EnterOtp) {
                resendCountdown = 30
                while (resendCountdown > 0) {
                    delay(1000)
                    resendCountdown--
                }
            }
        }

        // Auto-submit when all 6 OTP digits are entered — eliminates
        // the need for an explicit "Verify" tap in the happy path,
        // which is a meaningful UX improvement for a flow users go
        // through under friction (waiting for an SMS, often on a slow
        // connection).
        LaunchedEffect(otpCode) {
            if (otpCode.length == OTP_LENGTH && step == PhoneAuthStep.EnterOtp) {
                isLoading = true
                // TODO wire to ViewModel: authRepository.verifyPhoneOtp(phoneNumber, otpCode)
                // pattern-match AuthResult same as EmailAuthScreen
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = CcSpacing.xl),
        ) {
            IconButton(
                onClick = {
                    when (step) {
                        PhoneAuthStep.EnterPhone -> navigator.pop()
                        PhoneAuthStep.EnterOtp -> {
                            step = PhoneAuthStep.EnterPhone
                            otpCode = ""
                            errorMessage = null
                        }
                    }
                },
                modifier = Modifier.padding(top = CcSpacing.sm),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colorScheme.onBackground,
                )
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                },
                label = "phoneAuthStep",
                modifier = Modifier.weight(1f),
            ) { currentStep ->
                when (currentStep) {
                    PhoneAuthStep.EnterPhone -> EnterPhoneStep(
                        phoneNumber = phoneNumber,
                        onPhoneChange = { phoneNumber = it; errorMessage = null },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onContinue = {
                            isLoading = true
                            errorMessage = null
                            // TODO: authRepository.requestPhoneOtp(phoneNumber)
                            // on OtpRequestResult.Sent -> step = PhoneAuthStep.EnterOtp
                            // on Failure(RateLimited) -> errorMessage = "Too many attempts..."
                            step = PhoneAuthStep.EnterOtp // placeholder until ViewModel wired
                            isLoading = false
                        },
                    )
                    PhoneAuthStep.EnterOtp -> EnterOtpStep(
                        phoneNumber = phoneNumber,
                        otpCode = otpCode,
                        onOtpChange = { if (it.length <= OTP_LENGTH && it.all { c -> c.isDigit() }) otpCode = it },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        resendCountdown = resendCountdown,
                        onResend = {
                            otpCode = ""
                            resendCountdown = 30
                            // TODO: authRepository.requestPhoneOtp(phoneNumber)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EnterPhoneStep(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onContinue: () -> Unit,
) {
    val isValid = phoneNumber.length >= 7

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "What's your\nphone number?",
            style = CcTextStyles.headlineLarge,
            color = LocalCipherChatColorScheme.current.onBackground,
            modifier = Modifier.padding(bottom = CcSpacing.xl),
        )

        CcTextField(
            value = phoneNumber,
            onValueChange = onPhoneChange,
            label = "Phone number",
            placeholder = "+1 555 000 0000",
            keyboardType = KeyboardType.Phone,
            errorText = errorMessage,
            helperText = if (errorMessage == null) "We'll send a one-time code to verify." else null,
        )

        CcPrimaryButton(
            text = "Send Code",
            enabled = isValid,
            isLoading = isLoading,
            onClick = onContinue,
            modifier = Modifier.padding(top = CcSpacing.xl),
        )
    }
}

@Composable
private fun EnterOtpStep(
    phoneNumber: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    resendCountdown: Int,
    onResend: () -> Unit,
) {
    val colorScheme = LocalCipherChatColorScheme.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Enter the code",
            style = CcTextStyles.headlineLarge,
            color = colorScheme.onBackground,
        )
        Text(
            text = "Sent to $phoneNumber",
            style = CcTextStyles.bodyMedium,
            color = colorScheme.onSurfaceMuted,
            modifier = Modifier.padding(top = CcSpacing.sm, bottom = CcSpacing.xxl),
        )

        // Segmented OTP input — drives a hidden BasicTextField for
        // actual input; the visual representation is the 6 boxes below.
        // This is the standard pattern for segmented OTP inputs in
        // Compose: one invisible input + custom visual display, rather
        // than 6 separate TextFields that each need focus management.
        Box {
            BasicTextField(
                value = otpCode,
                onValueChange = onOtpChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.size(0.dp), // hidden — visuals below
                cursorBrush = SolidColor(colorScheme.accent),
                singleLine = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(CcSpacing.sm)) {
                repeat(OTP_LENGTH) { index ->
                    val digit = otpCode.getOrNull(index)?.toString() ?: ""
                    val isActive = index == otpCode.length
                    val isError = errorMessage != null

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CcRadius.shapeMd)
                            .background(colorScheme.surface)
                            .border(
                                width = if (isActive || isError) 1.5.dp else 1.dp,
                                color = when {
                                    isError -> colorScheme.error
                                    isActive -> colorScheme.accent
                                    else -> colorScheme.border
                                },
                                shape = CcRadius.shapeMd,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = digit,
                            style = CcTextStyles.headlineSmall,
                            color = colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                style = CcTextStyles.labelSmall,
                color = colorScheme.error,
                modifier = Modifier.padding(top = CcSpacing.sm),
                textAlign = TextAlign.Center,
            )
        }

        if (resendCountdown > 0) {
            Text(
                text = "Resend code in ${resendCountdown}s",
                style = CcTextStyles.bodyMedium,
                color = colorScheme.onSurfaceMuted,
                modifier = Modifier.padding(top = CcSpacing.xl),
            )
        } else {
            CcGhostButton(
                text = "Resend code",
                onClick = onResend,
                modifier = Modifier.padding(top = CcSpacing.xl),
            )
        }
    }
}

private enum class PhoneAuthStep { EnterPhone, EnterOtp }
private const val OTP_LENGTH = 6
