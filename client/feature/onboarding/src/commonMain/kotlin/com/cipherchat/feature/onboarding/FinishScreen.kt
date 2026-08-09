package com.cipherchat.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme

/**
 * Page 7 of 7: Finish. The handoff point from onboarding to real
 * account creation — this is the LAST page, so unlike pages 2-6 it
 * has no "Skip" action (skipping the final page makes no sense) and
 * its primary action is "Continue" rather than "Next," signaling this
 * is a different kind of step than the previous pages.
 *
 * Deliberately does NOT hardcode navigation to a specific auth screen
 * here ([com.cipherchat.feature.auth] doesn't exist as a module yet
 * in this build sequence). Instead [onContinue] is a required
 * constructor parameter the app-shell wiring supplies once
 * feature:auth exists — e.g. `FinishScreen(onContinue = {
 * navigator.replaceAll(AuthChoiceScreen()) })`. This keeps
 * feature:onboarding from depending on feature:auth directly, which
 * would create a dependency in the wrong direction (a more foundational,
 * earlier-in-the-user-journey feature module should never depend on a
 * later one) — composition of "what comes after onboarding" belongs
 * at the app-shell level, not hardcoded inside onboarding itself.
 */
class FinishScreen(
    private val onContinue: () -> Unit = {},
) : Screen {
    @Composable
    override fun Content() {
        OnboardingPageScaffold(
            pageIndex = 6,
            totalPages = TOTAL_ONBOARDING_PAGES,
            title = "You're all set.",
            description = "Your keys are generated, your conversations are ready to be " +
                "yours alone. Let's get you signed in.",
            primaryActionLabel = "Continue",
            onPrimaryAction = onContinue,
            onSkipAll = null, // last page — nothing left to skip
            illustration = { FinishIllustration() },
        )
    }
}

@Composable
private fun FinishIllustration() {
    val colorScheme = LocalCipherChatColorScheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(colorScheme.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.RocketLaunch,
            contentDescription = null,
            tint = colorScheme.accent,
            modifier = Modifier.size(80.dp),
        )
    }
}
