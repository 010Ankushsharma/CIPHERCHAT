package com.cipherchat.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cipherchat.core.designsystem.CcAvatarSize
import com.cipherchat.core.designsystem.CcIconSize
import com.cipherchat.core.designsystem.CcSpacing
import com.cipherchat.core.designsystem.CcTextStyles
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme

/**
 * One row in the Home Screen's chat list (Chats / Groups / Channels /
 * Communities / Archive / Favorites / Unread tabs all reuse this same
 * row — they differ only in which underlying data stream feeds the
 * list, not in row appearance). Keeping ALL chat-list tabs visually
 * identical is deliberate: a user switching between "Chats" and
 * "Unread" should feel like they're filtering the same list, not
 * looking at a different UI each time.
 *
 * Like [MessageBubble], this stays domain-agnostic — it takes plain
 * primitive parameters rather than a
 * [com.cipherchat.core.domain.model.Chat] directly, since
 * core:designsystem cannot depend on core:domain. feature:chat's
 * ViewModel/screen layer maps the domain model into these parameters.
 */
@Composable
fun ChatListRow(
    title: String,
    previewText: String,
    timestampLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    unreadCount: Int = 0,
    isMuted: Boolean = false,
    isPinned: Boolean = false,
    isTyping: Boolean = false,
    presenceIndicator: PresenceIndicatorStyle = PresenceIndicatorStyle.None,
) {
    val colorScheme = LocalCipherChatColorScheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = CcSpacing.lg, vertical = CcSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(CcAvatarSize.md)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceElevated),
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(CcAvatarSize.md).clip(CircleShape),
                    )
                } else {
                    // Fallback: first letter of title, centered — avoids
                    // a broken-image icon flash for users without an
                    // avatar set, which is common (Anonymous Secure
                    // Session users, freshly created groups).
                    Box(
                        modifier = Modifier.size(CcAvatarSize.md),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = title.firstOrNull()?.uppercase() ?: "?",
                            style = CcTextStyles.titleLarge,
                            color = colorScheme.onSurfaceMuted,
                        )
                    }
                }
            }

            if (presenceIndicator != PresenceIndicatorStyle.None) {
                PresenceDot(
                    style = presenceIndicator,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = CcSpacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = CcTextStyles.titleMedium,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isPinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = colorScheme.onSurfaceMuted,
                        modifier = Modifier.size(CcIconSize.xs).padding(start = CcSpacing.xxs),
                    )
                }
            }

            Text(
                text = if (isTyping) "typing…" else previewText,
                style = CcTextStyles.bodyMedium,
                color = if (isTyping) colorScheme.accent else colorScheme.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(start = CcSpacing.sm),
        ) {
            Text(
                text = timestampLabel,
                style = CcTextStyles.messageTimestamp,
                color = if (unreadCount > 0) colorScheme.accent else colorScheme.onSurfaceMuted,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(CcSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = CcSpacing.xxs),
            ) {
                if (isMuted) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsOff,
                        contentDescription = "Muted",
                        tint = colorScheme.onSurfaceMuted,
                        modifier = Modifier.size(CcIconSize.xs),
                    )
                }
                if (unreadCount > 0) {
                    UnreadBadge(count = unreadCount)
                }
            }
        }
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    val colorScheme = LocalCipherChatColorScheme.current
    Box(
        modifier = Modifier
            .widthIn(min = 20.dp)
            .clip(CircleShape)
            .background(colorScheme.accent)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // 99+ cap rather than an ever-growing number — unbounded
            // unread counts look alarming and the precise number past
            // 99 is rarely actionable information for the user.
            text = if (count > 99) "99+" else count.toString(),
            style = CcTextStyles.labelSmall,
            color = colorScheme.onAccent,
        )
    }
}

@Composable
private fun PresenceDot(style: PresenceIndicatorStyle, modifier: Modifier = Modifier) {
    val colorScheme = LocalCipherChatColorScheme.current
    val dotColor = when (style) {
        PresenceIndicatorStyle.Online -> colorScheme.success
        PresenceIndicatorStyle.Custom -> colorScheme.info
        PresenceIndicatorStyle.None -> Color.Transparent
    }
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(colorScheme.background) // ring matching the screen background creates a "cutout" effect against the avatar
            .padding(2.dp)
            .clip(CircleShape)
            .background(dotColor),
    )
}

enum class PresenceIndicatorStyle { None, Online, Custom }
