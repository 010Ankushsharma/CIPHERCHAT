package com.cipherchat.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cipherchat.core.designsystem.CcSpacing
import com.cipherchat.core.designsystem.CcTextStyles
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme
import com.cipherchat.core.designsystem.components.CcGhostButton
import com.cipherchat.core.designsystem.components.CcPrimaryButton
import com.cipherchat.core.designsystem.components.CcTextField

/**
 * Email login/registration in one screen with a [AuthFormMode] toggle.
 * Deliberately unified rather than split into two separate screens —
 * email + password fields are shared between Login and Register; only
 * displayName is Register-only. Two screens would mean duplicating
 * field validation logic for no user-facing benefit.
 *
 * State is currently local (remember) — should be promoted to an
 * EmailAuthViewModel backed by AuthRepository once Koin wiring exists.
 * Clearly marked with a TODO rather than silently deferred.
 */
class EmailAuthScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val colorScheme = LocalCipherChatColorScheme.current

        var mode by remember { mutableStateOf(AuthFormMode.Login) }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var displayName by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val isFormValid = email.contains("@") &&
            password.length >= 8 &&
            (mode == AuthFormMode.Login || displayName.isNotBlank())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = CcSpacing.xl),
        ) {
            Text(
                text = if (mode == AuthFormMode.Login) "Welcome back." else "Create your account.",
                style = CcTextStyles.headlineLarge,
                color = colorScheme.onBackground,
                modifier = Modifier.padding(top = CcSpacing.xxl, bottom = CcSpacing.xl),
            )

            if (mode == AuthFormMode.Register) {
                CcTextField(
                    value = displayName,
                    onValueChange = { displayName = it; errorMessage = null },
                    label = "Display name",
                    placeholder = "Jane Doe",
                    modifier = Modifier.padding(bottom = CcSpacing.md),
                )
            }

            CcTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = "Email",
                placeholder = "you@example.com",
                keyboardType = KeyboardType.Email,
                modifier = Modifier.padding(bottom = CcSpacing.md),
            )

            CcTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = "Password",
                placeholder = if (mode == AuthFormMode.Register) "At least 8 characters" else "••••••••",
                isPassword = true,
                helperText = if (mode == AuthFormMode.Register)
                    "8+ characters. We'll never see it — it never leaves your device unencrypted."
                else null,
                errorText = errorMessage,
            )

            if (mode == AuthFormMode.Login) {
                CcGhostButton(
                    text = "Forgot password?",
                    onClick = {
                        // navigator.push(PasswordResetScreen()) — added when recovery flow is built
                    },
                    modifier = Modifier.padding(top = CcSpacing.xs),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom,
            ) {
                CcPrimaryButton(
                    text = if (mode == AuthFormMode.Login) "Sign In" else "Create Account",
                    enabled = isFormValid,
                    isLoading = isLoading,
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        // TODO wire to EmailAuthViewModel:
                        // viewModel.submit(email, password, displayName)
                        // then observe AuthResult:
                        //   Success -> navigator.replaceAll(HomeScreen())
                        //   RequiresOtp -> navigator.push(OtpScreen(phoneNumber))
                        //   Failure(InvalidCredentials) -> errorMessage = "Incorrect email or password"
                        //   Failure(NetworkError) -> errorMessage = "Check your connection and try again"
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = CcSpacing.xl),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (mode == AuthFormMode.Login)
                            "Don't have an account?" else "Already have an account?",
                        style = CcTextStyles.bodyMedium,
                        color = colorScheme.onSurfaceMuted,
                    )
                    CcGhostButton(
                        text = if (mode == AuthFormMode.Login) "Sign up" else "Sign in",
                        onClick = {
                            mode = if (mode == AuthFormMode.Login) AuthFormMode.Register else AuthFormMode.Login
                            errorMessage = null
                        },
                        modifier = Modifier.padding(start = CcSpacing.xxs),
                    )
                }
            }
        }
    }
}

private enum class AuthFormMode { Login, Register }
