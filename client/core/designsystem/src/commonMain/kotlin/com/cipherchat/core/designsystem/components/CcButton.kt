package com.cipherchat.core.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.cipherchat.core.designsystem.CcRadius
import com.cipherchat.core.designsystem.CcSpacing
import com.cipherchat.core.designsystem.CcTextStyles
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme

/**
 * Filled, accent-colored button — the highest-emphasis call to action
 * (e.g. "Continue" in onboarding, "Send" verification code, primary
 * dialog confirm). There should typically be at most ONE
 * [CcPrimaryButton] visible on screen at a time; a screen with three
 * competing gold buttons defeats the "one confident accent" principle
 * from the color system — secondary actions should use
 * [CcSecondaryButton] or [CcGhostButton] instead.
 *
 * Includes a built-in loading state ([isLoading]) rather than leaving
 * every call site to build its own spinner-swap logic — auth flows in
 * particular (login, OTP verify) almost always need this, so baking
 * it in here means consistent loading UX everywhere instead of N
 * slightly different hand-rolled versions.
 */
@Composable
fun CcPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    fillWidth: Boolean = true,
) {
    val colorScheme = LocalCipherChatColorScheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // Subtle press-scale gives the "spring animation" tactile feel the
    // spec calls for, without needing a full physics-based animation
    // library for something this small.
    val pressScale = if (isPressed) 0.97f else 1f

    val backgroundColor = if (enabled) colorScheme.accent else colorScheme.accent.copy(alpha = 0.35f)
    val contentColor = if (enabled) colorScheme.onAccent else colorScheme.onAccent.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .let { if (fillWidth) it.fillMaxWidth() else it }
            .scale(pressScale)
            .height(52.dp)
            .clip(CcRadius.shapeMd)
            .background(backgroundColor)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = null, // custom press-scale feedback above replaces the default ripple
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(targetState = isLoading, label = "primaryButtonLoadingState") { loading ->
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = contentColor,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Text(text = text, style = CcTextStyles.titleMedium, color = contentColor)
            }
        }
    }
}

/**
 * Outlined button — secondary-emphasis actions (e.g. "Skip" next to a
 * primary "Continue", "Cancel" next to "Confirm" in a destructive
 * dialog). Uses the border token rather than a filled background, so
 * it visually recedes behind any primary button on the same screen.
 */
@Composable
fun CcSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
) {
    val colorScheme = LocalCipherChatColorScheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale = if (isPressed) 0.97f else 1f

    Box(
        modifier = modifier
            .let { if (fillWidth) it.fillMaxWidth() else it }
            .scale(pressScale)
            .height(52.dp)
            .clip(CcRadius.shapeMd)
            .background(colorScheme.surface)
            .border(
                border = BorderStroke(width = 1.dp, color = colorScheme.border),
                shape = CcRadius.shapeMd,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CcTextStyles.titleMedium,
            color = if (enabled) colorScheme.onSurface else colorScheme.onSurfaceMuted,
        )
    }
}

/**
 * Text-only, lowest-emphasis action — "Forgot password?", "Learn
 * more", inline links inside body text contexts. No background, no
 * border, minimal touch feedback (just opacity dip) so it never
 * visually competes with primary/secondary buttons nearby.
 */
@Composable
fun CcGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colorScheme = LocalCipherChatColorScheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val contentAlpha = if (isPressed) 0.6f else 1f

    Text(
        text = text,
        style = CcTextStyles.labelLarge,
        color = colorScheme.accent.copy(alpha = contentAlpha),
        modifier = modifier
            .clip(CcRadius.shapeSm)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = CcSpacing.sm, vertical = CcSpacing.xs),
    )
}
