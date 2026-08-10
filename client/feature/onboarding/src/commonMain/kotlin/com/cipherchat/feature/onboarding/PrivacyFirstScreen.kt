package com.cipherchat.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
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
import com.cipherchat.core.designsystem.CcIconSize
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme

/**
 * Page 2 of 7: Privacy First. Establishes the pattern every remaining
 * onboarding page follows — a thin [Screen] whose entire body is one
 * [OnboardingPageScaffold] call, with page-specific copy, an icon/
 * illustration slot, and navigation to the next page in sequence. See
 * [OnboardingPageScaffold]'s doc comment for why this pattern was
 * extracted rather than repeated per page.
 */
class PrivacyFirstScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        OnboardingPageScaffold(
            pageIndex = 1,
            totalPages = TOTAL_ONBOARDING_PAGES,
            title = "Privacy isn't a feature.\nIt's the foundation.",
            description = "CipherChat is built so that even we can't read your messages. " +
                "Your conversations belong to you — and only the people you choose to share them with.",
            primaryActionLabel = "Next",
            onPrimaryAction = { navigator.push(EndToEndEncryptionScreen()) },
            onSkipAll = { navigator.push(FinishScreen()) },
            illustration = { PrivacyShieldIllustration() },
        )
    }
}

@Composable
private fun PrivacyShieldIllustration() {
    val colorScheme = LocalCipherChatColorScheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(colorScheme.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        // Placeholder iconography — replaced with a bespoke Lottie
        // animation once illustration assets are produced; using a
        // Material icon here keeps this screen visually complete and
        // demonstrable in the meantime rather than leaving an empty box.
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = colorScheme.accent,
            modifier = Modifier.size(80.dp),
        )
    }
}

/** Total page count, referenced by every onboarding page for consistent progress dots. */
internal const val TOTAL_ONBOARDING_PAGES = 7

/** Forward declarations — built out in subsequent onboarding files. */
