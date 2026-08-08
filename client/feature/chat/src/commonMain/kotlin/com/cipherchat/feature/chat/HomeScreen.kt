package com.cipherchat.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.cipherchat.core.designsystem.CcAvatarSize
import com.cipherchat.core.designsystem.CcIconSize
import com.cipherchat.core.designsystem.CcSpacing
import com.cipherchat.core.designsystem.CcTextStyles
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme
import com.cipherchat.core.designsystem.components.ChatListRow
import com.cipherchat.core.designsystem.components.GlassCard
import com.cipherchat.core.designsystem.components.PresenceIndicatorStyle
import com.cipherchat.core.domain.model.Chat
import com.cipherchat.core.domain.model.ChatKind
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The Home Screen — the first screen users see after authentication.
 * Contains: top bar (avatar + search + AI button + notifications),
 * bottom tab navigation (Chats / Groups / Channels / AI), and the
 * main scrollable chat list with a morphing FAB.
 *
 * The FAB hides when scrolling down (the list takes priority) and
 * reappears when scrolling up or at the top — a pattern that
 * maximises list visibility on small screens while keeping the primary
 * action always reachable within one scroll-up gesture.
 */
class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val colorScheme = LocalCipherChatColorScheme.current
        val hazeState = remember { HazeState() }

        var selectedTab by remember { mutableIntStateOf(0) }

        // TODO: replace with real ViewModel collecting from ChatRepository
        val chats = remember { listOf<Chat>() }
        val listState = rememberLazyListState()

        // FAB visibility: hide when scrolling down into the list.
        val isFabVisible by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex == 0 ||
                    listState.lastScrolledBackward
            }
        }

        Scaffold(
            containerColor = colorScheme.background,
            bottomBar = {
                HomeBottomNavigation(
                    hazeState = hazeState,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = isFabVisible,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut(),
                ) {
                    HomeFab(onClick = { /* navigator.push(NewChatScreen()) */ })
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    HomeTopBar(
                        hazeState = hazeState,
                        onSearchClick = { /* navigator.push(SearchScreen()) */ },
                        onAiClick = { /* navigator.push(AiAssistantScreen()) */ },
                        onNotificationsClick = { },
                        onAvatarClick = { /* navigator.push(ProfileScreen()) */ },
                    )

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = CcSpacing.xl),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (chats.isEmpty()) {
                            item {
                                EmptyChatListPlaceholder(
                                    tab = selectedTab,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
                            items(chats, key = { it.id.value }) { chat ->
                                ChatListRow(
                                    title = chat.title,
                                    previewText = chat.lastMessagePreview?.summary ?: "",
                                    timestampLabel = chat.lastMessagePreview?.sentAt
                                        ?.let { formatTimestamp(it) } ?: "",
                                    onClick = { navigator.push(ChatScreen(chatId = chat.id.value)) },
                                    unreadCount = chat.unreadCount,
                                    isMuted = chat.isMuted,
                                    isPinned = chat.isPinned,
                                    presenceIndicator = when (chat.kind) {
                                        is ChatKind.OneToOne -> PresenceIndicatorStyle.Online
                                        else -> PresenceIndicatorStyle.None
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    hazeState: HazeState,
    onSearchClick: () -> Unit,
    onAiClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAvatarClick: () -> Unit,
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
                .padding(horizontal = CcSpacing.md, vertical = CcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Avatar — tappable to open profile/settings
            Box(
                modifier = Modifier
                    .size(CcAvatarSize.sm)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceElevated),
            ) {
                // TODO: replace with actual current user avatar from ViewModel
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("J", style = CcTextStyles.titleMedium, color = colorScheme.accent)
                }
            }

            Text(
                text = "CipherChat",
                style = CcTextStyles.titleLarge,
                color = colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(horizontal = CcSpacing.md),
            )

            Row {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = colorScheme.onBackground)
                }
                IconButton(onClick = onAiClick) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Assistant", tint = colorScheme.accent)
                }
                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = colorScheme.onBackground)
                }
            }
        }
    }
}

@Composable
private fun HomeBottomNavigation(
    hazeState: HazeState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val colorScheme = LocalCipherChatColorScheme.current
    val tabs = listOf(
        Pair("Chats", Icons.Filled.ChatBubble),
        Pair("Groups", Icons.Filled.Groups),
        Pair("Silent", Icons.Filled.VolumeOff),
        Pair("AI", Icons.Filled.AutoAwesome),
    )

    GlassCard(
        hazeState = hazeState,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        NavigationBar(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            tabs.forEachIndexed { index, (label, icon) ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(CcIconSize.sm)) },
                    label = { Text(label, style = CcTextStyles.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colorScheme.accent,
                        selectedTextColor = colorScheme.accent,
                        unselectedIconColor = colorScheme.onSurfaceMuted,
                        unselectedTextColor = colorScheme.onSurfaceMuted,
                        indicatorColor = colorScheme.accent.copy(alpha = 0.12f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun HomeFab(onClick: () -> Unit) {
    val colorScheme = LocalCipherChatColorScheme.current
    FloatingActionButton(
        onClick = onClick,
        containerColor = colorScheme.accent,
        contentColor = colorScheme.onAccent,
        shape = CircleShape,
    ) {
        Icon(Icons.Filled.Add, contentDescription = "New Chat", modifier = Modifier.size(CcIconSize.md))
    }
}

@Composable
private fun EmptyChatListPlaceholder(tab: Int, modifier: Modifier = Modifier) {
    val colorScheme = LocalCipherChatColorScheme.current
    val message = when (tab) {
        0 -> "No conversations yet.\nTap + to start one."
        1 -> "No groups yet.\nTap + to create one."
        2 -> "Nothing muted."
        3 -> "Ask the AI assistant anything\nabout your conversations."
        else -> "Nothing here yet."
    }
    Box(modifier = modifier.padding(CcSpacing.xxl), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = CcTextStyles.bodyLarge,
            color = colorScheme.onSurfaceMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** Stub — ChatScreen built in next file. */
internal class ChatScreen(val chatId: String) : Screen {
    @Composable
    override fun Content() {
        androidx.compose.material3.Text("Chat screen — built next")
    }
}

/** Formats an Instant to a short human-readable timestamp for the chat list row. */
private fun formatTimestamp(instant: kotlinx.datetime.Instant): String {
    // TODO: implement proper relative formatting ("just now", "2m", "Mon", "12 Jan")
    // using kotlinx-datetime. Placeholder returns epoch seconds for now.
    return "${instant.epochSeconds / 3600}h"
}
