package com.cipherchat.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cipherchat.core.designsystem.CcRadius
import com.cipherchat.core.designsystem.CcSpacing
import com.cipherchat.core.designsystem.CcTextStyles
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme

/**
 * The design system's text input primitive. Built on [BasicTextField]
 * rather than Material3's TextField, because Material's filled/
 * outlined variants carry visual assumptions (the floating label
 * animation, the underline-to-outline treatment) that don't match
 * CipherChat's glass-card-and-border aesthetic — wrapping BasicTextField
 * directly gives full control over that without fighting Material's
 * built-in styling at every call site.
 *
 * Error state is communicated through BOTH the border color AND
 * explicit [errorText] copy below the field — never color alone, in
 * keeping with the spec's Color Blind Support accessibility
 * requirement (same reasoning as the onboarding progress dots using
 * shape, not just color, to indicate the active page).
 */
@Composable
fun CcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val colorScheme = LocalCipherChatColorScheme.current
    var isFocused by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val isError = errorText != null
    val borderColor = when {
        isError -> colorScheme.error
        isFocused -> colorScheme.accent
        else -> colorScheme.border
    }

    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = CcTextStyles.labelLarge,
                color = colorScheme.onSurfaceMuted,
                modifier = Modifier.padding(bottom = CcSpacing.xxs, start = CcSpacing.xxs),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(CcRadius.shapeMd)
                .background(colorScheme.surface)
                .border(width = if (isFocused || isError) 1.5.dp else 1.dp, color = borderColor, shape = CcRadius.shapeMd)
                .padding(horizontal = CcSpacing.md, vertical = CcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let {
                it()
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(CcSpacing.sm))
            }

            Column(modifier = Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = CcTextStyles.bodyLarge,
                        color = colorScheme.onSurfaceMuted.copy(alpha = 0.6f),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = CcTextStyles.bodyLarge.copy(color = colorScheme.onSurface),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colorScheme.accent),
                    visualTransformation = if (isPassword && !isPasswordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChangedCompat { isFocused = it },
                )
            }

            if (isPassword) {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = colorScheme.onSurfaceMuted,
                    )
                }
            }
        }

        val captionText = errorText ?: helperText
        captionText?.let {
            Text(
                text = it,
                style = CcTextStyles.labelSmall,
                color = if (isError) colorScheme.error else colorScheme.onSurfaceMuted,
                modifier = Modifier.padding(top = CcSpacing.xxs, start = CcSpacing.xxs),
            )
        }
    }
}

/**
 * Thin wrapper around Modifier.onFocusChanged to keep the import list
 * in the main function focused — separated mainly so the verbose
 * FocusState -> Boolean mapping doesn't clutter the primary call site.
 */
private fun Modifier.onFocusChangedCompat(onChange: (Boolean) -> Unit): Modifier =
    this.then(
        androidx.compose.ui.focus.onFocusChanged { focusState -> onChange(focusState.isFocused) },
    )
