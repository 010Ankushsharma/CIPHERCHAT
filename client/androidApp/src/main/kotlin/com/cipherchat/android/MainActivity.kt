package com.cipherchat.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.cipherchat.core.designsystem.CcThemeMode
import com.cipherchat.core.designsystem.CipherChatTheme
import com.cipherchat.feature.auth.AuthChoiceScreen
import com.cipherchat.feature.onboarding.WelcomeScreen

/**
 * The single Android Activity for CipherChat. Uses the single-activity
 * pattern — all navigation happens inside Voyager's [Navigator], not
 * by launching new Activities. This is the standard approach for
 * modern Compose apps and matches the KMP architecture (iOS and
 * desktop don't have Activities at all).
 *
 * Responsibilities:
 *  - Install splash screen
 *  - Enable edge-to-edge rendering (so Compose owns the full window)
 *  - Check first-launch state and route accordingly
 *  - Wrap everything in [CipherChatTheme]
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CipherChatApp()
        }
    }
}

@Composable
private fun CipherChatApp() {
    // TODO: read persisted theme preference from DataStore/settings
    // repository via a ViewModel; using System default for now.
    CipherChatTheme(themeMode = CcThemeMode.System) {
        val isFirstLaunch = rememberIsFirstLaunch()
        val startScreen = remember(isFirstLaunch) {
            if (isFirstLaunch) WelcomeScreen() else AuthChoiceScreen()
        }

        Navigator(screen = startScreen) { navigator ->
            // SlideTransition gives the horizontal slide-in/out
            // animation between screens that matches platform conventions
            // and the spec's "Smooth Motion" requirement, with no
            // additional setup per screen.
            SlideTransition(navigator)
        }
    }
}

/**
 * Checks whether this is the first time the app has been launched —
 * determines whether to show onboarding or skip straight to auth.
 *
 * TODO: replace with a real DataStore-backed check. Using a hardcoded
 * `true` here so onboarding always shows during development and the
 * full flow is exercisable without clearing app data. The real
 * implementation writes a "onboarding_complete" flag after
 * [FinishScreen]'s onContinue fires and checks it here on launch.
 */
@Composable
private fun rememberIsFirstLaunch(): Boolean = remember { true }
