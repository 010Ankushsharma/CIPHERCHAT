package com.cipherchat.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cipherchat.core.designsystem.CcRadius
import com.cipherchat.core.designsystem.CcSpacing
import com.cipherchat.core.designsystem.CcTextStyles
import com.cipherchat.core.designsystem.CcThemeMode
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme
import com.cipherchat.core.designsystem.components.CcGhostButton
import com.cipherchat.core.designsystem.components.CcPrimaryButton

/**
 * Page 6 of 7: Customization. Unlike pages 2-5, this one collects a
 * real decision (theme preference) rather than just explaining a
 * concept — so it intentionally breaks from [OnboardingPageScaffold]'s
 * static illustration-slot layout rather than forcing this content
 * into a shape that doesn't quite fit it. Forcing every page into one
 * scaffold "for consistency" would be the wrong kind of consistency
 * here — the scaffold is for explanatory pages, not decision pages.
 *
 * The selected theme is held in local state here and is expected to
 * be persisted (and applied app-wide via [CipherChatTheme]) by the
 * caller once feature:onboarding is wired to a real settings
 * repository — this screen's job is collecting the choice, not
 * owning where it's stored long-term.
 */
class CustomizationScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val colorScheme = LocalCipherChatColorScheme.current
        var selectedMode by remember { mutableStateOf(CcThemeMode.System) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = CcSpacing.xl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = CcSpacing.md),
                horizontalArrangement = Arrangement.End,
            ) {
                CcGhostButton(text = "Skip", onClick = { navigator.push(FinishScreen()) })
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = "Make it yours.",
                    style = CcTextStyles.headlineLarge,
                    color = colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Pick a look to start with — you can change this anytime in Settings.",
                    style = CcTextStyles.bodyLarge,
                    color = colorScheme.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = CcSpacing.sm),
                )

                Column(
                    modifier = Modifier.padding(top = CcSpacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(CcSpacing.md),
                ) {
                    ThemeOptionCard(
                        label = "Match my device",
                        description = "Switches automatically with your system setting",
                        icon = Icons.Filled.SettingsSuggest,
                        isSelected = selectedMode == CcThemeMode.System,
                        onClick = { selectedMode = CcThemeMode.System },
                    )
                    ThemeOptionCard(
                        label = "Light",
                        description = "Warm white, always on",
                        icon = Icons.Filled.LightMode,
                        isSelected = selectedMode == CcThemeMode.Light,
                        onClick = { selectedMode = CcThemeMode.Light },
                    )
                    ThemeOptionCard(
                        label = "Dark",
                        description = "Deep ink black, always on",
                        icon = Icons.Filled.DarkMode,
                        isSelected = selectedMode == CcThemeMode.Dark,
                        onClick = { selectedMode = CcThemeMode.Dark },
                    )
                }
            }

            OnboardingProgressDotsStandalone(pageIndex = 5, totalPages = TOTAL_ONBOARDING_PAGES)

            CcPrimaryButton(
                text = "Next",
                onClick = {
                    // TODO once feature:onboarding is wired to Koin/a
                    // settings repository: persist `selectedMode` here
                    // before navigating, e.g.
                    // settingsRepository.setThemeMode(selectedMode)
                    navigator.push(FinishScreen())
                },
                modifier = Modifier.padding(top = CcSpacing.lg, bottom = CcSpacing.xl),
            )
        }
    }
}

@Composable
private fun ThemeOptionCard(
    label: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = LocalCipherChatColorScheme.current
    val borderColor = if (isSelected) colorScheme.accent else colorScheme.border

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CcRadius.shapeMd)
            .background(colorScheme.surface)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = CcRadius.shapeMd)
            .clickable(onClick = onClick)
            .padding(CcSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colorScheme.accent)

        Column(modifier = Modifier.weight(1f).padding(start = CcSpacing.md)) {
            Text(text = label, style = CcTextStyles.titleMedium, color = colorScheme.onSurface)
            Text(text = description, style = CcTextStyles.bodyMedium, color = colorScheme.onSurfaceMuted)
        }

        if (isSelected) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .height(24.dp)
                    .clip(CircleShape)
                    .background(colorScheme.accent)
                    .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = colorScheme.onAccent,
                )
            }
        }
    }
}

/**
 * Standalone variant of the progress dots from [OnboardingPageScaffold]
 * (which is private to that file) — duplicated rather than refactored
 * into a shared internal function purely because this screen's custom
 * layout doesn't otherwise share OnboardingPageScaffold's structure;
 * see TODO note for promoting this to a shared file if a third
 * non-scaffold page is ever added.
 */
@Composable
private fun OnboardingProgressDotsStandalone(pageIndex: Int, totalPages: Int) {
    val colorScheme = LocalCipherChatColorScheme.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(CcSpacing.xs),
        modifier = Modifier.fillMaxWidth().padding(bottom = CcSpacing.sm),
    ) {
        repeat(totalPages) { index ->
            val isActive = index == pageIndex
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (isActive) 24.dp else 8.dp)
                    .clip(CcRadius.shapeFull)
                    .background(if (isActive) colorScheme.accent else colorScheme.border),
            )
        }
    }
}
