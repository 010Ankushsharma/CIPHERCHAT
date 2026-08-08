package com.cipherchat.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cipherchat.core.designsystem.CcAvatarSize
import com.cipherchat.core.designsystem.CcIconSize
import com.cipherchat.core.designsystem.CcSpacing
import com.cipherchat.core.designsystem.CcTextStyles
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme
import com.cipherchat.core.designsystem.components.GlassCard
import com.cipherchat.core.designsystem.components.MessageBubble
import com.cipherchat.core.designsystem.components.MessageBubbleStatus
import com.cipherchat.core.domain.model.Message
import com.cipherchat.core.domain.model.MessageContent
import com.cipherchat.core.domain.model.MessageExpiration
import com.cipherchat.core.domain.model.MessageStatus
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * Individual chat screen. Three main areas:
 *
 *   Top bar     — avatar + name + call buttons + overflow menu.
 *                 Blurs the message list beneath it via Haze.
 *   Message list — LazyColumn, newest messages at the bottom,
 *                  pagination triggered as user scrolls up.
 *   Input bar   — text field + attachment + voice note + send.
 *                 Sticks above the keyboard (imePadding) and
 *                 blurs the list behind it via the same HazeState.
 *
 * Own messages are right-aligned with an accent tint; others are
 * left-aligned with a surface-elevated background — using our
 * MessageBubble component which handles both via [isOwnMessage].
 *
 * Typing indicator appears above the input bar when a remote
 * participant is typing, fading in/out via AnimatedVisibility.
 */
class ChatScreen(val chatId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val colorScheme = LocalCipherChatColorScheme.current
        val hazeState = remember { HazeState() }
        val listState = rememberLazyListState()

        var inputText by remember { mutableStateOf("") }
        var isRemoteTyping by remember { mutableStateOf(false) }

        // TODO: replace with ViewModel.messages.collectAsState()
        val messages = remember { listOf<Message>() }
        val currentUserId = "current_user_placeholder"

        // Scroll to bottom on new messages
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .haze(state = hazeState),
        ) {
            ChatTopBar(
                hazeState = hazeState,
                chatTitle = "Chat", // TODO: from ViewModel
                avatarUrl = null,
                onBackClick = { navigator.pop() },
                onVideoCallClick = { /* navigator.push(VideoCallScreen(chatId)) */ },
                onMoreClick = { /* show bottom sheet */ },
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = CcSpacing.md,
                    end = CcSpacing.md,
                    top = CcSpacing.sm,
                    bottom = CcSpacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(CcSpacing.xs),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(CcSpacing.xxl),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No messages yet.\nSay hello 👋",
                                style = CcTextStyles.bodyLarge,
                                color = colorScheme.onSurfaceMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }

                itemsIndexed(messages, key = { _, msg -> msg.id.value }) { _, message ->
                    val isOwn = message.senderId.value == currentUserId
                    val isEphemeral = message.expiration is MessageExpiration.ReadOnce

                    MessageBubble(
                        isOwnMessage = isOwn,
                        status = message.status.toUiStatus(),
                        isEphemeral = isEphemeral,
                        timestampLabel = formatTimestamp(message.sentAt),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (isOwn) CcSpacing.xxl else 0.dp,
                                end = if (isOwn) 0.dp else CcSpacing.xxl,
                            ),
                    ) {
                        MessageContentView(content = message.content)
                    }
                }
            }

            // Typing indicator
            AnimatedVisibility(
                visible = isRemoteTyping,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.padding(horizontal = CcSpacing.lg, vertical = CcSpacing.xxs),
            ) {
                Text(
                    text = "typing…",
                    style = CcTextStyles.bodyMedium,
                    color = colorScheme.accent,
                )
            }

            ChatInputBar(
                hazeState = hazeState,
                text = inputText,
                onTextChange = { inputText = it },
                onSendClick = {
                    if (inputText.isNotBlank()) {
                        // TODO: viewModel.sendMessage(chatId, MessageContent.Text(inputText))
                        inputText = ""
                    }
                },
                onAttachClick = { /* show attachment picker */ },
                onVoiceNoteClick = { /* start voice recording */ },
            )
        }
    }
}

@Composable
private fun ChatTopBar(
    hazeState: HazeState,
    chatTitle: String,
    avatarUrl: String?,
    onBackClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val colorScheme = LocalCipherChatColorScheme.current

    GlassCard(
        hazeState = hazeState,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
        showEdgeHighlight = false,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = CcSpacing.sm, vertical = CcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colorScheme.onBackground,
                )
            }

            Box(
                modifier = Modifier
                    .size(CcAvatarSize.sm)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceElevated),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = chatTitle.firstOrNull()?.uppercase() ?: "?",
                    style = CcTextStyles.titleMedium,
                    color = colorScheme.onSurfaceMuted,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = CcSpacing.sm),
            ) {
                Text(
                    text = chatTitle,
                    style = CcTextStyles.titleMedium,
                    color = colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Online",
                    style = CcTextStyles.labelSmall,
                    color = colorScheme.success,
                )
            }

            IconButton(onClick = onVideoCallClick) {
                Icon(
                    imageVector = Icons.Filled.VideoCall,
                    contentDescription = "Video call",
                    tint = colorScheme.onBackground,
                    modifier = Modifier.size(CcIconSize.md),
                )
            }
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    hazeState: HazeState,
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
    onVoiceNoteClick: () -> Unit,
) {
    val colorScheme = LocalCipherChatColorScheme.current
    val canSend = text.isNotBlank()

    GlassCard(
        hazeState = hazeState,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 20.dp, topEnd = 20.dp,
            bottomStart = 0.dp, bottomEnd = 0.dp,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = CcSpacing.md, vertical = CcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onAttachClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.AttachFile,
                    contentDescription = "Attach",
                    tint = colorScheme.onSurfaceMuted,
                    modifier = Modifier.size(CcIconSize.md),
                )
            }

            com.cipherchat.core.designsystem.components.CcTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = "Message",
                singleLine = false,
                modifier = Modifier.weight(1f).padding(horizontal = CcSpacing.xs),
            )

            IconButton(
                onClick = if (canSend) onSendClick else onVoiceNoteClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (canSend) colorScheme.accent else colorScheme.surfaceElevated),
            ) {
                Icon(
                    imageVector = if (canSend) Icons.AutoMirrored.Filled.Send else Icons.Filled.Mic,
                    contentDescription = if (canSend) "Send" else "Voice note",
                    tint = if (canSend) colorScheme.onAccent else colorScheme.onSurfaceMuted,
                    modifier = Modifier.size(CcIconSize.sm),
                )
            }
        }
    }
}

@Composable
private fun MessageContentView(content: MessageContent) {
    val colorScheme = LocalCipherChatColorScheme.current
    when (content) {
        is MessageContent.Text -> Text(
            text = content.body,
            style = CcTextStyles.messageBody,
            color = colorScheme.onSurface,
        )
        is MessageContent.Image -> Text(
            text = "📷 Photo",
            style = CcTextStyles.messageBody,
            color = colorScheme.onSurfaceMuted,
        )
        is MessageContent.VoiceNote -> Text(
            text = "🎤 Voice note (${content.durationMs / 1000}s)",
            style = CcTextStyles.messageBody,
            color = colorScheme.onSurfaceMuted,
        )
        is MessageContent.Document -> Text(
            text = "📎 ${content.fileName}",
            style = CcTextStyles.messageBody,
            color = colorScheme.onSurfaceMuted,
        )
        is MessageContent.Code -> Text(
            text = content.source,
            style = CcTextStyles.monospaceBody,
            color = colorScheme.onSurface,
        )
        is MessageContent.Location -> Text(
            text = if (content.isLive) "📍 Live Location" else "📍 Location",
            style = CcTextStyles.messageBody,
            color = colorScheme.onSurfaceMuted,
        )
        is MessageContent.SystemEvent -> Text(
            text = content.description,
            style = CcTextStyles.labelSmall,
            color = colorScheme.onSurfaceMuted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        else -> Text(
            text = "📦 Unsupported content",
            style = CcTextStyles.bodyMedium,
            color = colorScheme.onSurfaceMuted,
        )
    }
}

private fun MessageStatus.toUiStatus(): MessageBubbleStatus = when (this) {
    MessageStatus.Sending -> MessageBubbleStatus.Sending
    MessageStatus.Sent -> MessageBubbleStatus.Sent
    MessageStatus.Delivered -> MessageBubbleStatus.Delivered
    MessageStatus.Read -> MessageBubbleStatus.Read
    MessageStatus.Failed -> MessageBubbleStatus.Failed
}
