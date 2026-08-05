package com.cipherchat.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cipherchat.core.designsystem.CcRadius
import com.cipherchat.core.designsystem.CcSpacing
import com.cipherchat.core.designsystem.CcTextStyles
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme

/**
 * The message bubble — rendered once per message in every chat
 * screen, so this is the single most performance-sensitive component
 * in the design system. Deliberately kept "dumb": it owns layout,
 * shape, color, and status iconography, but delegates the actual
 * message content rendering to [content], a slot lambda. This keeps
 * MessageBubble itself agnostic to the many
 * [com.cipherchat.core.domain.model.MessageContent] subtypes (text,
 * image, voice note, poll, code, ...) — feature:chat owns a separate
 * "which renderer for which content type" dispatch, not this module,
 * since core:designsystem must never depend on core:domain.
 *
 * @param isOwnMessage controls bubble alignment (right for own
 *   messages, left for others') and the asymmetric corner radius via
 *   [CcRadius.messageBubbleShape].
 * @param isEphemeral true for Whisper Messages / disappearing
 *   messages — applies [CipherChatColorScheme.ephemeralAccent] as a
 *   subtle border instead of the normal bubble background, so
 *   ephemeral content is visually distinguishable at a glance before
 *   the user even reads it (helps avoid accidentally screenshotting
 *   or forwarding something meant to disappear, since its different
 *   appearance is a constant visual reminder).
 * @param senderName shown above the bubble only in group chats for
 *   messages not from the current user; null elsewhere.
 */
@Composable
fun MessageBubble(
    isOwnMessage: Boolean,
    status: MessageBubbleStatus,
    modifier: Modifier = Modifier,
    isEphemeral: Boolean = false,
    senderName: String? = null,
    timestampLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = LocalCipherChatColorScheme.current
    val shape = CcRadius.messageBubbleShape(isOwnMessage)

    val bubbleColor = when {
        isOwnMessage -> colorScheme.accent.copy(alpha = 0.18f)
        else -> colorScheme.surfaceElevated
    }

    Column(
        modifier = modifier,
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start,
    ) {
        senderName?.let {
            Text(
                text = it,
                style = CcTextStyles.messageSenderName,
                color = colorScheme.accent,
                modifier = Modifier.padding(start = CcSpacing.sm, bottom = CcSpacing.xxs),
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .let { base ->
                    if (isEphemeral) {
                        base.then(
                            Modifier.background(colorScheme.ephemeralAccent.copy(alpha = 0.06f)),
                        )
                    } else base
                }
                .padding(horizontal = CcSpacing.md, vertical = CcSpacing.sm),
        ) {
            content()
        }

        Row(
            modifier = Modifier.padding(top = CcSpacing.xxs, start = CcSpacing.xs, end = CcSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(CcSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEphemeral) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = "Disappearing message",
                    tint = colorScheme.ephemeralAccent,
                )
            }
            timestampLabel?.let {
                Text(text = it, style = CcTextStyles.messageTimestamp, color = colorScheme.onSurfaceMuted)
            }
            if (isOwnMessage) {
                MessageStatusIcon(status = status, tint = colorScheme.onSurfaceMuted, accentTint = colorScheme.accent)
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(status: MessageBubbleStatus, tint: androidx.compose.ui.graphics.Color, accentTint: androidx.compose.ui.graphics.Color) {
    when (status) {
        MessageBubbleStatus.Sending -> Icon(Icons.Filled.Schedule, contentDescription = "Sending", tint = tint)
        MessageBubbleStatus.Sent -> Icon(Icons.Filled.Check, contentDescription = "Sent", tint = tint)
        MessageBubbleStatus.Delivered -> Icon(Icons.Filled.DoneAll, contentDescription = "Delivered", tint = tint)
        MessageBubbleStatus.Read -> Icon(Icons.Filled.DoneAll, contentDescription = "Read", tint = accentTint)
        MessageBubbleStatus.Failed -> Text("!", style = CcTextStyles.labelSmall, color = androidx.compose.ui.graphics.Color.Red)
    }
}

/**
 * Deliberately a SEPARATE enum from
 * [com.cipherchat.core.domain.model.MessageStatus] rather than reusing
 * it directly — core:designsystem cannot depend on core:domain (see
 * module dependency rules), so feature:chat maps the domain enum to
 * this one when calling MessageBubble. The duplication is the price
 * of keeping the design system framework/domain-agnostic and reusable.
 */
enum class MessageBubbleStatus { Sending, Sent, Delivered, Read, Failed }
