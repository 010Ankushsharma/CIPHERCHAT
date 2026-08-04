package com.cipherchat.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Type scale for CipherChat. Two font families, used deliberately:
 *
 *  - [CcFontFamily.Display]: a higher-contrast, slightly geometric
 *    sans for large text (onboarding headlines, chat titles in the
 *    header) — where personality and "premium feel" matter most.
 *  - [CcFontFamily.Body]: a calmer, highly legible sans for message
 *    bodies and dense UI — where reading comfort over long sessions
 *    matters more than visual flair.
 *
 * This mirrors how Apple uses SF Pro Display vs SF Pro Text at
 * different sizes, and avoids the common mistake of using one
 * "personality" font at every size, which reads as showy in dense
 * UI and flat in hero moments.
 *
 * NOTE: actual font files are bundled as Compose resources under
 * client/core/designsystem/src/commonMain/composeResources/font/ —
 * see the font-loading setup file (next in this module) for how
 * [CcFontFamily] resolves to real Font objects per platform.
 */
object CcFontFamily {
    // Resolved lazily via expect/actual + Compose resource loading;
    // declared here as the stable reference point every TextStyle uses.
    val Display: FontFamily = FontFamily.Default // placeholder until custom font files are added — see font-loading setup file
    val Body: FontFamily = FontFamily.Default
    val Mono: FontFamily = FontFamily.Monospace // for Code message content + Safety Number display
}

/**
 * Letter-spacing scale: large display text gets slightly TIGHTER
 * spacing (a common premium-feel technique — big text with default
 * spacing looks loose), while small label text gets slightly WIDER
 * spacing for legibility at tiny sizes. This is the kind of small
 * deliberate choice that separates a "looks like every other Compose
 * app" UI from one that reads as intentionally designed.
 */
private object CcLetterSpacing {
    val tight: TextUnit = (-0.5).sp
    val normal: TextUnit = 0.sp
    val wide: TextUnit = 0.4.sp
}

object CcTextStyles {
    val displayLarge = TextStyle(
        fontFamily = CcFontFamily.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = CcLetterSpacing.tight,
    )

    val displayMedium = TextStyle(
        fontFamily = CcFontFamily.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = CcLetterSpacing.tight,
    )

    val headlineLarge = TextStyle(
        fontFamily = CcFontFamily.Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = CcLetterSpacing.normal,
    )

    val headlineSmall = TextStyle(
        fontFamily = CcFontFamily.Display,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    )

    val titleLarge = TextStyle(
        fontFamily = CcFontFamily.Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )

    val titleMedium = TextStyle(
        fontFamily = CcFontFamily.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )

    val bodyLarge = TextStyle(
        fontFamily = CcFontFamily.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )

    val bodyMedium = TextStyle(
        fontFamily = CcFontFamily.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    val labelLarge = TextStyle(
        fontFamily = CcFontFamily.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = CcLetterSpacing.wide,
    )

    val labelSmall = TextStyle(
        fontFamily = CcFontFamily.Body,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = CcLetterSpacing.wide,
    )

    // --- Chat-specific styles, not covered by generic Material scale ---

    /** The actual message bubble text. Slightly larger line-height than bodyMedium for comfortable long-message reading. */
    val messageBody = TextStyle(
        fontFamily = CcFontFamily.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    )

    /** Sender name above a message bubble in group chats. */
    val messageSenderName = TextStyle(
        fontFamily = CcFontFamily.Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    )

    /** Timestamp under/beside a message bubble — deliberately small and low-emphasis. */
    val messageTimestamp = TextStyle(
        fontFamily = CcFontFamily.Body,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = CcLetterSpacing.wide,
    )

    /** Code message content + Safety Number digit display. */
    val monospaceBody = TextStyle(
        fontFamily = CcFontFamily.Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
}

/** Material3 Typography object built from the same styles, so Material components (TextField, Button labels) stay visually consistent with custom CipherChat components. */
val CipherChatMaterialTypography = Typography(
    displayLarge = CcTextStyles.displayLarge,
    displayMedium = CcTextStyles.displayMedium,
    headlineLarge = CcTextStyles.headlineLarge,
    headlineSmall = CcTextStyles.headlineSmall,
    titleLarge = CcTextStyles.titleLarge,
    titleMedium = CcTextStyles.titleMedium,
    bodyLarge = CcTextStyles.bodyLarge,
    bodyMedium = CcTextStyles.bodyMedium,
    labelLarge = CcTextStyles.labelLarge,
    labelSmall = CcTextStyles.labelSmall,
)
