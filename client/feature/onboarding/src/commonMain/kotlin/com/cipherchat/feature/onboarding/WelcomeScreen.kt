package com.cipherchat.feature.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.cipherchat.core.designsystem.components.GlassCard
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * The first screen a new user ever sees. Sets the tone for the entire
 * app's "premium, futuristic, privacy-first" identity in one glance —
 * worth spending real design attention here since first impressions
 * disproportionately shape how trustworthy/polished the rest of the
 * app feels, especially for an app whose entire value proposition is
 * "trust us with your private conversations."
 *
 * Demonstrates the design system working together end-to-end:
 * gradient background -> Haze blur source -> GlassCard hero element
 * with edge highlight -> premium display typography -> gold primary
 * CTA -> ghost secondary action. Every visual choice here traces back
 * to a token from core:designsystem, not a hardcoded value.
 */
class WelcomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val colorScheme = LocalCipherChatColorScheme.current
        val hazeState = remember { HazeState() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(colorScheme.background, colorScheme.surface),
                    ),
                )
                .haze(state = hazeState), // marks this Box's content as the blur SOURCE for any GlassCard below
        ) {
            // Soft ambient gold glow, slowly pulsing — a small detail
            // that reads as "alive" rather than static, in keeping with
            // the spec's "dynamic lighting, smooth motion" direction,
            // without resorting to anything distracting on a screen
            // meant to feel calm and trustworthy.
            AmbientGlow(modifier = Modifier.align(Alignment.TopCenter))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = CcSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Placeholder for the Lottie hero illustration — actual
                // animation file wired in once client/core/designsystem's
                // composeResources/lottie/ assets are added.
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(colorScheme.accent.copy(alpha = 0.15f)),
                )

                Text(
                    text = "CipherChat",
                    style = CcTextStyles.displayLarge,
                    color = colorScheme.onBackground,
                    modifier = Modifier.padding(top = CcSpacing.xl),
                )

                Text(
                    text = "Conversations that belong to only you.",
                    style = CcTextStyles.titleMedium,
                    color = colorScheme.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = CcSpacing.sm),
                )

                GlassCard(
                    hazeState = hazeState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = CcSpacing.xxl),
                ) {
                    Column(modifier = Modifier.padding(CcSpacing.lg)) {
                        FeatureHighlightRow(
                            title = "End-to-end encrypted",
                            description = "Every message, call, and file — locked to your devices only.",
                        )
                        FeatureHighlightRow(
                            title = "AI that works for you, locally",
                            description = "Smart replies and summaries, processed on-device whenever possible.",
                            modifier = Modifier.padding(top = CcSpacing.md),
                        )
                        FeatureHighlightRow(
                            title = "Yours across every device",
                            description = "Seamless, secure multi-device sync — no compromises.",
                            modifier = Modifier.padding(top = CcSpacing.md),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = CcSpacing.xxl, bottom = CcSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CcPrimaryButton(
                        text = "Get Started",
                        onClick = { navigator.push(PrivacyFirstScreen()) },
                    )
                    CcGhostButton(
                        text = "I already have an account",
                        onClick = { /* navigator.push(LoginScreen()) — wired once feature:auth exists */ },
                        modifier = Modifier.padding(top = CcSpacing.sm),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(title: String, description: String, modifier: Modifier = Modifier) {
    val colorScheme = LocalCipherChatColorScheme.current
    Column(modifier = modifier) {
        Text(text = title, style = CcTextStyles.titleMedium, color = colorScheme.onSurface)
        Text(
            text = description,
            style = CcTextStyles.bodyMedium,
            color = colorScheme.onSurfaceMuted,
            modifier = Modifier.padding(top = CcSpacing.xxs),
        )
    }
}

@Composable
private fun AmbientGlow(modifier: Modifier = Modifier) {
    val colorScheme = LocalCipherChatColorScheme.current
    val transition = rememberInfiniteTransition(label = "ambientGlow")
    val alpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambientGlowAlpha",
    )

    Box(
        modifier = modifier
            .padding(top = 80.dp)
            .size(280.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(colorScheme.accent.copy(alpha = alpha), colorScheme.accent.copy(alpha = 0f)),
                ),
            ),
    )
}
