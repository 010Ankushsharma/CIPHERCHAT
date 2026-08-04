package com.cipherchat.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * [CompositionLocal] carrying the current [CipherChatColorScheme].
 * Using `staticCompositionLocalOf` (not `compositionLocalOf`) because
 * the color scheme only changes on an explicit theme toggle, not on
 * every recomposition — `static` avoids the (small but real) overhead
 * of tracking reads for a value that's effectively constant for the
 * lifetime of a screen.
 *
 * Defaults to [CipherChatDarkColorScheme] rather than throwing if read
 * outside [CipherChatTheme] — a missing theme wrapper should degrade
 * to a sensible default during development, not crash a preview or a
 * test that forgot to wrap content in the theme.
 */
val LocalCipherChatColorScheme = staticCompositionLocalOf { CipherChatDarkColorScheme }

/**
 * Root theme wrapper. Every screen in CipherChat — and every Compose
 * Preview — should be wrapped in this exactly once, near the app's
 * root composable (see androidApp/iosApp/desktopApp MainActivity/
 * equivalent entry points).
 *
 * Bridges two systems simultaneously:
 *  1. CipherChat's own [CipherChatColorScheme] / [CcTextStyles] /
 *     [CcSpacing] / [CcRadius] — read directly by custom components
 *     like [com.cipherchat.core.designsystem.components.GlassCard].
 *  2. Material3's [MaterialTheme] — read by any stock Material
 *     component (TextField, Switch, etc.) we use as-is rather than
 *     building a fully custom replacement for. Both are populated
 *     from the SAME underlying token values, so there's never a
 *     visual seam between a custom CipherChat component sitting next
 *     to a stock Material one.
 *
 * @param themeMode controls light/dark/system selection — see
 *   Settings > Appearance, which writes this from user preference.
 */
@Composable
fun CipherChatTheme(
    themeMode: CcThemeMode = CcThemeMode.System,
    dynamicColorEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        CcThemeMode.Light -> false
        CcThemeMode.Dark -> true
        CcThemeMode.System -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) CipherChatDarkColorScheme else CipherChatLightColorScheme

    // NOTE: dynamicColorEnabled (Material You / wallpaper-derived color
    // on Android 12+) is accepted here as a forward-looking parameter
    // but intentionally not yet wired to a real dynamic scheme — doing
    // so requires platform-specific (androidMain) access to
    // dynamicDarkColorScheme()/dynamicLightColorScheme(), which lives
    // in an expect/actual pair we'll add alongside the Android app
    // shell, not in this commonMain file.
    val materialColorScheme = if (useDarkTheme) {
        androidx.compose.material3.darkColorScheme(
            primary = colorScheme.accent,
            onPrimary = colorScheme.onAccent,
            background = colorScheme.background,
            onBackground = colorScheme.onBackground,
            surface = colorScheme.surface,
            onSurface = colorScheme.onSurface,
            error = colorScheme.error,
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = colorScheme.accent,
            onPrimary = colorScheme.onAccent,
            background = colorScheme.background,
            onBackground = colorScheme.onBackground,
            surface = colorScheme.surface,
            onSurface = colorScheme.onSurface,
            error = colorScheme.error,
        )
    }

    CompositionLocalProvider(LocalCipherChatColorScheme provides colorScheme) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = CipherChatMaterialTypography,
            content = content,
        )
    }
}

enum class CcThemeMode { Light, Dark, System }
