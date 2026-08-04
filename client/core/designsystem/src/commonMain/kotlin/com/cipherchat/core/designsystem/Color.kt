package com.cipherchat.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Raw color values for CipherChat's design language: Black + Warm
 * White as the base, Soft Gold as the singular accent — deliberately
 * ONE accent color, not a rainbow of brand colors, so gold always
 * reads as "this is interactive / this is important" rather than
 * competing with itself across the UI (an Apple-esque restraint
 * principle: a single, confident accent rather than many).
 *
 * Naming convention: raw swatches here are named by what they ARE
 * (CcGold500, CcInk900), not what they're FOR (don't name a swatch
 * "ButtonBackground" here — that semantic mapping lives in
 * [CipherChatColorScheme] below, so the same raw swatch can be reused
 * for different purposes without renaming).
 */
object CcColors {
    // Warm white family — intentionally NOT pure #FFFFFF; a slightly
    // warm-shifted white feels softer under glassmorphism/blur layers
    // and reduces eye strain in the light theme.
    val WarmWhite50 = Color(0xFFFFFDF9)
    val WarmWhite100 = Color(0xFFFAF7F0)
    val WarmWhite200 = Color(0xFFF2EDE2)
    val WarmWhite300 = Color(0xFFE6DECB)

    // Ink/black family — a true near-black with a faint warm undertone,
    // not a cold blue-black, to stay consistent with WarmWhite's tone.
    val Ink900 = Color(0xFF14120F)
    val Ink800 = Color(0xFF1E1B17)
    val Ink700 = Color(0xFF2A2620)
    val Ink600 = Color(0xFF3A352D)
    val Ink500 = Color(0xFF52493D)

    // Soft Gold accent — the ONE accent color in the entire system.
    val Gold300 = Color(0xFFE8C77A)
    val Gold500 = Color(0xFFCBA052) // primary accent — buttons, active states, FAB
    val Gold700 = Color(0xFF9C7A38) // pressed/emphasis state, text-on-light accent use

    // Functional colors — kept deliberately muted/desaturated so they
    // don't compete visually with Gold500 as a second "accent."
    val Success500 = Color(0xFF5C8A6B)
    val Warning500 = Color(0xFFB8924A)
    val Error500 = Color(0xFFC1574B)
    val Info500 = Color(0xFF5E7A94)

    // Glassmorphism overlay tints — used by Haze blur layers, alpha
    // applied at the component level, not baked into these values.
    val GlassLightTint = WarmWhite50
    val GlassDarkTint = Ink900
}

/**
 * Semantic color scheme — what every screen and component should
 * actually reference. Maps meaning ("surface", "onSurface", "accent")
 * to a raw [CcColors] swatch. If CipherChat ever rebrands the accent
 * from gold to something else, ONLY this mapping changes — no screen
 * code does.
 */
data class CipherChatColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentMuted: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val border: Color,
    val glassTint: Color,
    /** Used by Whisper Messages / Ghost Mode UI — a slightly desaturated overlay signaling "ephemeral/private state." */
    val ephemeralAccent: Color,
)

val CipherChatDarkColorScheme = CipherChatColorScheme(
    background = CcColors.Ink900,
    surface = CcColors.Ink800,
    surfaceElevated = CcColors.Ink700,
    onBackground = CcColors.WarmWhite100,
    onSurface = CcColors.WarmWhite100,
    onSurfaceMuted = CcColors.Ink500,
    accent = CcColors.Gold500,
    onAccent = CcColors.Ink900,
    accentMuted = CcColors.Gold700,
    success = CcColors.Success500,
    warning = CcColors.Warning500,
    error = CcColors.Error500,
    info = CcColors.Info500,
    border = CcColors.Ink600,
    glassTint = CcColors.GlassDarkTint,
    ephemeralAccent = CcColors.Gold300,
)

val CipherChatLightColorScheme = CipherChatColorScheme(
    background = CcColors.WarmWhite50,
    surface = CcColors.WarmWhite100,
    surfaceElevated = CcColors.WarmWhite50,
    onBackground = CcColors.Ink900,
    onSurface = CcColors.Ink900,
    onSurfaceMuted = CcColors.Ink500,
    accent = CcColors.Gold700,
    onAccent = CcColors.WarmWhite50,
    accentMuted = CcColors.Gold500,
    success = CcColors.Success500,
    warning = CcColors.Warning500,
    error = CcColors.Error500,
    info = CcColors.Info500,
    border = CcColors.WarmWhite300,
    glassTint = CcColors.GlassLightTint,
    ephemeralAccent = CcColors.Gold700,
)
