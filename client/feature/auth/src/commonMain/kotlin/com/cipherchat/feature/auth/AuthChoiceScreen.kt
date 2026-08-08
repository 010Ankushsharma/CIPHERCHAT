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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cipherchat.core.designsystem.CcSpacing
import com.cipherchat.core.designsystem.CcTextStyles
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme
import com.cipherchat.core.designsystem.components.CcGhostButton
import com.cipherchat.core.designsystem.components.CcPrimaryButton
import com.cipherchat.core.designsystem.components.CcSecondaryButton

/**
 * Landing screen for authentication — every login method from
 * [com.cipherchat.core.domain.repository.AuthRepository] is reachable
 * from here, but NOT given equal visual weight. Email and Phone are
 * the expected primary paths for most users and get full-emphasis
 * primary/secondary buttons; OAuth providers and Anonymous/Passkey/QR
 * are secondary paths a smaller fraction of users want, so they're
 * grouped into a lower-emphasis row below a divider. This mirrors how
 * most production auth screens are actually designed — giving eight
 * options equal visual weight creates decision paralysis, not
 * convenience.
 *
 * Each navigation target below ([EmailAuthScreen], [PhoneAuthScreen],
 * etc.) is a forward reference to a screen built in a subsequent file
 * in this module — see the stub block at the bottom of this file,
 * each replaced in turn as we continue building feature:auth.
 */
class AuthChoiceScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val colorScheme = LocalCipherChatColorScheme.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = CcSpacing.xl),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = "Sign in to CipherChat",
                    style = CcTextStyles.headlineLarge,
                    color = colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Your conversations are waiting — securely.",
                    style = CcTextStyles.bodyLarge,
                    color = colorScheme.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = CcSpacing.sm, bottom = CcSpacing.xxl),
                )

                // Primary paths — full emphasis.
                CcPrimaryButton(
                    text = "Continue with Email",
                    onClick = { navigator.push(EmailAuthScreen()) },
                )
                CcSecondaryButton(
                    text = "Continue with Phone Number",
                    onClick = { navigator.push(PhoneAuthScreen()) },
                    modifier = Modifier.padding(top = CcSpacing.sm),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = CcSpacing.xl),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = colorScheme.border)
                    Text(
                        text = "or",
                        style = CcTextStyles.labelSmall,
                        color = colorScheme.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = CcSpacing.sm),
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = colorScheme.border)
                }

                // Secondary paths — lower emphasis, grouped together.
                CcSecondaryButton(
                    text = "Continue with Google",
                    onClick = { navigator.push(OAuthLoadingScreen(provider = OAuthProvider.Google)) },
                )
                CcSecondaryButton(
                    text = "Continue with Apple",
                    onClick = { navigator.push(OAuthLoadingScreen(provider = OAuthProvider.Apple)) },
                    modifier = Modifier.padding(top = CcSpacing.sm),
                )
                CcSecondaryButton(
                    text = "Continue with GitHub",
                    onClick = { navigator.push(OAuthLoadingScreen(provider = OAuthProvider.GitHub)) },
                    modifier = Modifier.padding(top = CcSpacing.sm),
                )
                CcSecondaryButton(
                    text = "Continue with Passkey",
                    onClick = { navigator.push(PasskeyAuthScreen()) },
                    modifier = Modifier.padding(top = CcSpacing.sm),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = CcSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CcGhostButton(
                    text = "Continue Anonymously",
                    onClick = { navigator.push(AnonymousSessionScreen()) },
                )
                CcGhostButton(
                    text = "Scan QR code from another device",
                    onClick = { navigator.push(QrDeviceLinkScreen()) },
                    modifier = Modifier.padding(top = CcSpacing.xxs),
                )
            }
        }
    }
}

enum class OAuthProvider { Google, Apple, GitHub }
