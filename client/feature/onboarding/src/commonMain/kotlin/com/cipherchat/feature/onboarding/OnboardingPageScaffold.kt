package com.cipherchat.feature.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cipherchat.core.designsystem.CcRadius
import com.cipherchat.core.designsystem.CcSpacing
import com.cipherchat.core.designsystem.CcTextStyles
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme
import com.cipherchat.core.designsystem.components.CcGhostButton
import com.cipherchat.core.designsystem.components.CcPrimaryButton

/**
 * Shared layout for onboarding pages 2 through 7 (Privacy First, End-
 * to-End Encryption, Multi Device, AI Features, Customization,
 * Finish). [WelcomeScreen] stays a bespoke, one-off layout since it
 * carries extra weight as a first impression — but every page after
 * it follows the same rhythm: illustration, headline, supporting
 * copy, progress dots, primary continue action. Extracting that
 * rhythm here means adding a future onboarding page (if the product
 * grows beyond seven) is a ~15-line call site, not a new 150-line
 * screen copy-pasted from the previous one with the text changed.
 *
 * @param pageIndex zero-based index of this page among
 *   [totalPages], drives the progress dots.
 * @param illustration slot for page-specific Lottie animation/SVG —
 *   kept as a composable slot rather than an enum of fixed
 *   illustrations, so each page's animation can be fully bespoke.
 * @param onSkipAll if non-null, shows a "Skip" ghost action that jumps
 *   straight to onboarding completion — present on every page except
 *   the last (where "Skip" and "Finish" would be redundant).
 */
@Composable
fun OnboardingPageScaffold(
    pageIndex: Int,
    totalPages: Int,
    title: String,
    description: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    onSkipAll: (() -> Unit)? = null,
    illustration: @Composable () -> Unit = {},
) {
    val colorScheme = LocalCipherChatColorScheme.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = CcSpacing.xl),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = CcSpacing.md),
            horizontalArrangement = Arrangement.End,
        ) {
            onSkipAll?.let {
                CcGhostButton(text = "Skip", onClick = it)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(modifier = Modifier.size(220.dp)) {
                illustration()
            }

            Text(
                text = title,
                style = CcTextStyles.headlineLarge,
                color = colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = CcSpacing.xl),
            )

            Text(
                text = description,
                style = CcTextStyles.bodyLarge,
                color = colorScheme.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = CcSpacing.sm, start = CcSpacing.sm, end = CcSpacing.sm),
            )
        }

        OnboardingProgressDots(
            pageIndex = pageIndex,
            totalPages = totalPages,
            modifier = Modifier.padding(bottom = CcSpacing.xl).align(Alignment.CenterHorizontally),
        )

        CcPrimaryButton(
            text = primaryActionLabel,
            onClick = onPrimaryAction,
            modifier = Modifier.padding(bottom = CcSpacing.xl),
        )
    }
}

/**
 * Progress indicator dots — the active dot is wider (a "pill") rather
 * than just a different color, since shape change reads more clearly
 * than color change for users with color vision deficiency, in
 * keeping with the spec's accessibility requirements (Color Blind
 * Support).
 */
@Composable
private fun OnboardingProgressDots(pageIndex: Int, totalPages: Int, modifier: Modifier = Modifier) {
    val colorScheme = LocalCipherChatColorScheme.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(CcSpacing.xs)) {
        repeat(totalPages) { index ->
            val isActive = index == pageIndex
            val width by animateDpAsState(
                targetValue = if (isActive) 24.dp else 8.dp,
                animationSpec = tween(durationMillis = 250),
                label = "dotWidth",
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CcRadius.shapeFull)
                    .background(if (isActive) colorScheme.accent else colorScheme.border),
            )
        }
    }
}
