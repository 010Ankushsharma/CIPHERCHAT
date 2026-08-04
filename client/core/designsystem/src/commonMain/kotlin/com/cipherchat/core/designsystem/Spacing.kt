package com.cipherchat.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale, based on an 8dp grid with 4dp half-steps for fine
 * adjustments. Using a fixed scale (rather than arbitrary dp values
 * scattered through screens) is what makes a UI feel intentionally
 * designed rather than ad-hoc — every gap in the app is one of these
 * eleven values, never a one-off "13.dp" picked by eye on some screen.
 */
object CcSpacing {
    val none: Dp = 0.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
    val huge: Dp = 64.dp
    val massive: Dp = 96.dp
}

/**
 * Corner radius scale. CipherChat's design language calls for
 * generously rounded cards and floating elements — these radii skew
 * larger than typical Material defaults (which top out around 16dp
 * for "large") because the spec's "Nothing inspired" aesthetic reads
 * as more premium with rounder, softer shapes, especially under
 * glassmorphism blur where sharp corners look visually harsh against
 * a soft blurred background.
 */
object CcRadius {
    val none: Dp = 0.dp
    val xs: Dp = 6.dp
    val sm: Dp = 12.dp
    val md: Dp = 18.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    /** Fully rounded — avatars, FABs, pill-shaped chips/buttons. */
    val full: Dp = 999.dp

    val shapeXs = RoundedCornerShape(xs)
    val shapeSm = RoundedCornerShape(sm)
    val shapeMd = RoundedCornerShape(md)
    val shapeLg = RoundedCornerShape(lg)
    val shapeXl = RoundedCornerShape(xl)
    val shapeFull = RoundedCornerShape(full)

    /** Message bubble shape: asymmetric rounding (the "tail" corner is sharper) — see MessageBubble component for usage. */
    fun messageBubbleShape(isOwnMessage: Boolean): RoundedCornerShape = if (isOwnMessage) {
        RoundedCornerShape(topStart = lg, topEnd = lg, bottomStart = lg, bottomEnd = xs)
    } else {
        RoundedCornerShape(topStart = lg, topEnd = lg, bottomStart = xs, bottomEnd = lg)
    }
}

/**
 * Elevation scale — used for soft-shadow depth on floating elements
 * (FAB, dialogs, the AI Assistant panel). Kept LOW relative to typical
 * Material elevation values; the spec calls for "soft shadows," and
 * heavy Material-style elevation shadows read as dated/skeuomorphic
 * against a glassmorphism-forward aesthetic — the blur itself (via
 * Haze) does most of the "this is floating above other content" work,
 * with elevation shadow as a light supporting cue, not the primary one.
 */
object CcElevation {
    val flat: Dp = 0.dp
    val low: Dp = 2.dp
    val medium: Dp = 6.dp
    val high: Dp = 12.dp
    val floating: Dp = 16.dp // FAB, modal sheets
}

/** Standard icon sizes, kept consistent across the whole app rather than per-screen guesses. */
object CcIconSize {
    val xs: Dp = 16.dp
    val sm: Dp = 20.dp
    val md: Dp = 24.dp
    val lg: Dp = 32.dp
    val xl: Dp = 48.dp
}

/** Avatar sizes — small (chat list row), medium (chat header), large (profile screen). */
object CcAvatarSize {
    val sm: Dp = 36.dp
    val md: Dp = 48.dp
    val lg: Dp = 96.dp
    val xl: Dp = 128.dp
}
