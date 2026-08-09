package com.cipherchat.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
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
 * Page 4 of 7: Multi Device. Like [EndToEndEncryptionScreen], this
 * stays at the "what you get" level (phone, tablet, desktop, all in
 * sync) rather than explaining that each device actually holds its
 * own independent identity key and the app encrypts separately to
 * each one — that's an implementation detail (see
 * [com.cipherchat.core.domain.model.Device],
 * [com.cipherchat.core.domain.model.CryptoSessionRef]) the user
 * benefits from without needing to understand it here.
 */
class MultiDeviceScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        OnboardingPageScaffold(
            pageIndex = 3,
            totalPages = TOTAL_ONBOARDING_PAGES,
            title = "One conversation,\nevery device.",
            description = "Start a message on your phone, finish it on your laptop. " +
                "Every device you link gets its own secure key — lose one, the rest stay safe.",
            primaryActionLabel = "Next",
            onPrimaryAction = { navigator.push(AiFeaturesScreen()) },
            onSkipAll = { navigator.push(FinishScreen()) },
            illustration = { MultiDeviceIllustration() },
        )
    }
}

@Composable
private fun MultiDeviceIllustration() {
    val colorScheme = LocalCipherChatColorScheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(colorScheme.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Devices,
            contentDescription = null,
            tint = colorScheme.accent,
            modifier = Modifier.size(80.dp),
        )
    }
}
