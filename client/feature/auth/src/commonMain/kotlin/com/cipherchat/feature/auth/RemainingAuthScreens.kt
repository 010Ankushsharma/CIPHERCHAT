package com.cipherchat.feature.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cipherchat.core.designsystem.CcIconSize
import com.cipherchat.core.designsystem.CcSpacing
import com.cipherchat.core.designsystem.CcTextStyles
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme
import com.cipherchat.core.designsystem.components.CcGhostButton
import com.cipherchat.core.designsystem.components.CcPrimaryButton

/**
 * OAuth loading screen — shown while the OS-level OAuth flow is in
 * progress (browser/system sheet has been launched; we're waiting for
 * the deep-link callback with the auth code). Nothing for the user to
 * do here — just a spinner and reassurance. Navigating back cancels
 * the flow entirely (the system sheet would already have been
 * dismissed by the OS before we'd push this screen in real wiring).
 *
 * [provider] determines the human-readable copy ("Signing in with
 * Google" vs "Apple" vs "GitHub") — the actual OAuth launch happens
 * BEFORE this screen is pushed (in [AuthChoiceScreen]'s button
 * handlers once wired to a ViewModel), not here.
 */
class OAuthLoadingScreen(val provider: OAuthProvider) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val colorScheme = LocalCipherChatColorScheme.current

        // Rotation animation for the spinner ring — kept manual rather
        // than using CircularProgressIndicator's built-in animation so
        // the gold accent color matches exactly without fighting
        // Material's internal color resolution.
        val transition = rememberInfiniteTransition(label = "oauthSpinner")
        val rotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "spinnerRotation",
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = CcSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer { rotationZ = rotation },
                color = colorScheme.accent,
                strokeWidth = 3.dp,
            )

            Text(
                text = "Signing in with ${provider.displayName}…",
                style = CcTextStyles.titleMedium,
                color = colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = CcSpacing.xl),
            )

            Text(
                text = "You'll be redirected back automatically.",
                style = CcTextStyles.bodyMedium,
                color = colorScheme.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = CcSpacing.sm),
            )

            CcGhostButton(
                text = "Cancel",
                onClick = { navigator.pop() },
                modifier = Modifier.padding(top = CcSpacing.xxl),
            )
        }
    }
}

private val OAuthProvider.displayName get() = when (this) {
    OAuthProvider.Google -> "Google"
    OAuthProvider.Apple -> "Apple"
    OAuthProvider.GitHub -> "GitHub"
}

/**
 * Passkey authentication screen — triggers the platform's WebAuthn/
 * FIDO2 assertion flow and waits for the result. Like [OAuthLoadingScreen],
 * this is a "waiting" screen — the actual platform sheet (Face ID /
 * fingerprint / security key prompt) is triggered immediately on
 * entry via [LaunchedEffect]. The UI just needs to explain what's
 * happening and offer a fallback if the platform gesture fails or the
 * user dismisses.
 *
 * Passkeys are the highest-security auth option in the spec (hardware-
 * backed, phishing-resistant) — the copy here should reinforce that
 * this is the *better* path, not make users feel they're using an
 * unfamiliar or risky option by showing too much technical detail.
 */
class PasskeyAuthScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val colorScheme = LocalCipherChatColorScheme.current
        var isWaiting by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            // TODO wire to ViewModel: authRepository.loginWithPasskey()
            // -> Success -> navigator.replaceAll(HomeScreen())
            // -> Failure(PasskeyUnavailable) ->
            //      errorMessage = "Passkey not available on this device"
            //      isWaiting = false
            // Platform WebAuthn sheet launches here, before any user
            // interaction on this screen — if they dismiss the sheet,
            // errorMessage is set and the fallback options appear.
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = CcSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Fingerprint,
                contentDescription = null,
                tint = colorScheme.accent,
                modifier = Modifier.size(CcIconSize.xl),
            )

            Text(
                text = "Use your passkey",
                style = CcTextStyles.headlineLarge,
                color = colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = CcSpacing.xl),
            )

            Text(
                text = if (isWaiting)
                    "Follow the prompt to verify — no password needed."
                else
                    errorMessage ?: "Verification cancelled.",
                style = CcTextStyles.bodyLarge,
                color = if (errorMessage != null) colorScheme.error else colorScheme.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = CcSpacing.sm),
            )

            if (!isWaiting || errorMessage != null) {
                CcPrimaryButton(
                    text = "Try Again",
                    onClick = {
                        errorMessage = null
                        isWaiting = true
                        // TODO: re-trigger passkey assertion
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = CcSpacing.xxl),
                )
            }

            CcGhostButton(
                text = "Use a different sign-in method",
                onClick = { navigator.pop() },
                modifier = Modifier.padding(top = CcSpacing.md),
            )
        }
    }
}

/**
 * Anonymous secure session screen. No account, no personal data
 * collected — generates a random identity keypair on-device and
 * requests a session token tied to that keypair, not to any email,
 * phone, or OAuth identity.
 *
 * The copy here is intentionally careful: "Anonymous" could sound
 * either empowering ("no tracking!") or alarming ("no account means
 * I'll lose everything if I switch phones") depending on user context.
 * This screen addresses both: leads with the privacy framing, then
 * explains the data loss risk in plain terms BEFORE the user commits,
 * so they can make an informed choice rather than being surprised when
 * they switch devices and find no history.
 */
class AnonymousSessionScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val colorScheme = LocalCipherChatColorScheme.current
        var isLoading by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = CcSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = colorScheme.accent,
                modifier = Modifier.size(CcIconSize.xl),
            )

            Text(
                text = "No account required.",
                style = CcTextStyles.headlineLarge,
                color = colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = CcSpacing.xl),
            )

            Text(
                text = "Start messaging immediately — no email, no phone, no tracking. " +
                    "Your identity is a random key generated on this device right now.",
                style = CcTextStyles.bodyLarge,
                color = colorScheme.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = CcSpacing.sm),
            )

            // Risk disclosure — shown before the confirm button, not
            // buried in a settings screen after the user has already
            // committed. Informed consent is part of the privacy-first
            // principle, not just a legal checkbox.
            Text(
                text = "⚠ Your messages and contacts only exist on this device. " +
                    "If you uninstall or switch phones, they're gone permanently.",
                style = CcTextStyles.bodyMedium,
                color = colorScheme.warning,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    top = CcSpacing.xl,
                    bottom = CcSpacing.xxl,
                ),
            )

            CcPrimaryButton(
                text = "Continue Anonymously",
                isLoading = isLoading,
                onClick = {
                    isLoading = true
                    // TODO wire to ViewModel:
                    // authRepository.createAnonymousSession()
                    // -> Success -> navigator.replaceAll(HomeScreen())
                    // -> Failure -> show error toast
                },
            )

            CcGhostButton(
                text = "Create an account instead",
                onClick = { navigator.pop() },
                modifier = Modifier.padding(top = CcSpacing.md),
            )
        }
    }
}
