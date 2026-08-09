package com.cipherchat.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme

/**
 * Page 5 of 7: AI Features. The copy deliberately leads with PRIVACY
 * ("stays on your phone"), not capability ("smart replies!") —
 * because for a privacy-first messaging app, introducing AI features
 * without immediately addressing "does this mean AI reads my
 * messages?" risks undermining the trust the previous two pages just
 * built. Leading with the reassurance, then the capability, in that
 * order, matches how [com.cipherchat.core.domain.repository.AiRepository]
 * is actually architected (on-device first, opt-in remote fallback) —
 * the marketing claim here is backed by a real architectural
 * constraint, not just copywriting.
 */
class AiFeaturesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        OnboardingPageScaffold(
            pageIndex = 4,
            totalPages = TOTAL_ONBOARDING_PAGES,
            title = "Smart help that\nstays on your phone.",
            description = "Smart replies, summaries, and translation — processed on-device " +
                "whenever possible. Your messages are never used to train any model.",
            primaryActionLabel = "Next",
            onPrimaryAction = { navigator.push(CustomizationScreen()) },
            onSkipAll = { navigator.push(FinishScreen()) },
            illustration = { AiFeaturesIllustration() },
        )
    }
}

@Composable
private fun AiFeaturesIllustration() {
    val colorScheme = LocalCipherChatColorScheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(colorScheme.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = colorScheme.accent,
            modifier = Modifier.size(80.dp),
        )
    }
}
