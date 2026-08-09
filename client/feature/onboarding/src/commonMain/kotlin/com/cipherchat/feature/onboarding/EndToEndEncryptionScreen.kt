package com.cipherchat.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
 * Page 3 of 7: End-to-End Encryption. Deliberately explains the
 * concept in plain, reassuring language ("only unlock on your
 * devices") rather than technical terms like "Double Ratchet" or
 * "forward secrecy" — those terms belong in Settings > Encryption for
 * users who want the technical detail, not in a first-time onboarding
 * flow whose job is building trust quickly, not teaching cryptography.
 */
class EndToEndEncryptionScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        OnboardingPageScaffold(
            pageIndex = 2,
            totalPages = TOTAL_ONBOARDING_PAGES,
            title = "Locked the moment\nyou hit send.",
            description = "Messages, calls, and files are scrambled on your device and only " +
                "unlock on the recipient's — never in between, never on our servers.",
            primaryActionLabel = "Next",
            onPrimaryAction = { navigator.push(MultiDeviceScreen()) },
            onSkipAll = { navigator.push(FinishScreen()) },
            illustration = { EncryptionLockIllustration() },
        )
    }
}

@Composable
private fun EncryptionLockIllustration() {
    val colorScheme = LocalCipherChatColorScheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(colorScheme.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = colorScheme.accent,
            modifier = Modifier.size(80.dp),
        )
    }
}
