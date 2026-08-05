package com.cipherchat.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cipherchat.core.designsystem.CcRadius
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

/**
 * The frosted-glass surface used throughout CipherChat: the AI
 * Assistant panel, settings cards, dialogs, the story tray, and the
 * floating bottom navigation. Wraps Haze for the actual blur effect
 * and layers CipherChat's own glass-edge highlight on top, since raw
 * blur alone reads as flat — a subtle gradient border (brighter at
 * the top-left, fading to transparent toward the bottom-right) is
 * what makes a glass surface look like it has physical depth,
 * mimicking how light catches the top edge of real frosted glass.
 * Every glass surface in the app uses the same light direction
 * (top-left) so the whole UI reads as lit from one consistent source.
 *
 * [hazeState] must be the same [HazeState] instance attached to the
 * background content this card should blur — see GlassBackdrop (the
 * companion component that wraps a screen's scrollable content and
 * supplies the HazeState every GlassCard on that screen blurs
 * against). Without a shared HazeState, this card has nothing to blur
 * and falls back to a flat tinted surface — a safe degrade, not a
 * crash or invisible content.
 *
 * @param blurRadius how much of the background shows through — higher
 *   values blur more aggressively. Defaults to a moderate value that
 *   keeps background content recognizable-but-soft, matching the
 *   spec's "frosted glass" rather than "opaque card" feel.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    shape: RoundedCornerShape = CcRadius.shapeLg,
    blurRadius: Dp = 20.dp,
    tintAlpha: Float = 0.55f,
    showEdgeHighlight: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = LocalCipherChatColorScheme.current
    val glassTint = colorScheme.glassTint.copy(alpha = tintAlpha)

    val surfaceModifier = Modifier
        .clip(shape)
        .let { base ->
            if (hazeState != null) {
                base.hazeChild(state = hazeState, shape = shape) {
                    this.blurRadius = blurRadius
                    this.tint = glassTint
                }
            } else {
                base.background(colorScheme.surface)
            }
        }
        .let { base ->
            if (showEdgeHighlight) {
                base.border(
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                colorScheme.onSurface.copy(alpha = 0.22f),
                                colorScheme.onSurface.copy(alpha = 0.06f),
                            ),
                        ),
                    ),
                    shape = shape,
                )
            } else {
                base
            }
        }

    Box(modifier = modifier.then(surfaceModifier)) {
        content()
    }
}
