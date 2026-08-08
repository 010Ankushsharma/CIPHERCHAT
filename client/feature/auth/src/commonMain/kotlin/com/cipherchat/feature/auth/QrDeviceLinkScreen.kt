package com.cipherchat.feature.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cipherchat.core.designsystem.CcRadius
import com.cipherchat.core.designsystem.CcSpacing
import com.cipherchat.core.designsystem.CcTextStyles
import com.cipherchat.core.designsystem.LocalCipherChatColorScheme
import com.cipherchat.core.designsystem.components.GlassCard

/**
 * QR device linking screen — two roles in one screen:
 *
 *  [QrLinkMode.ShowCode] — the EXISTING authenticated device generates
 *    and displays a QR code containing a short-lived token from
 *    [com.cipherchat.core.network.CipherChatApiClient.generateDeviceLinkQrToken].
 *    The NEW device scans it to complete [completeDeviceLinkFromQr].
 *
 *  [QrLinkMode.ScanCode] — the NEW device activates its camera to
 *    scan the code displayed by the existing device. This mode also
 *    handles the case where a user arrives from [AuthChoiceScreen]
 *    on a fresh install (no session) and wants to link by scanning
 *    a code shown on a phone they're already logged in on.
 *
 * Security note (visible in this UI layer, not just backend): the QR
 * token is short-lived (backend sets expiry — see [QrLinkTokenDto])
 * AND single-use (the backend marks it consumed on first use). Even
 * if someone photographs the screen, a used or expired token is
 * worthless. This is documented here rather than buried in core:network
 * because it affects the UX decision to show a "Code expires in Xs"
 * countdown — users should understand why they need to scan promptly,
 * not just see a mysterious timer.
 */
class QrDeviceLinkScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val colorScheme = LocalCipherChatColorScheme.current
        var mode by remember { mutableStateOf(QrLinkMode.ShowCode) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CcSpacing.md, vertical = CcSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.onBackground,
                    )
                }
                Text(
                    text = "Link a Device",
                    style = CcTextStyles.titleLarge,
                    color = colorScheme.onBackground,
                    modifier = Modifier.padding(start = CcSpacing.sm),
                )
            }

            TabRow(
                selectedTabIndex = mode.ordinal,
                containerColor = colorScheme.surface,
                contentColor = colorScheme.accent,
            ) {
                Tab(
                    selected = mode == QrLinkMode.ShowCode,
                    onClick = { mode = QrLinkMode.ShowCode },
                    text = { Text("Show Code", style = CcTextStyles.labelLarge) },
                    icon = { Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(20.dp)) },
                )
                Tab(
                    selected = mode == QrLinkMode.ScanCode,
                    onClick = { mode = QrLinkMode.ScanCode },
                    text = { Text("Scan Code", style = CcTextStyles.labelLarge) },
                    icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp)) },
                )
            }

            when (mode) {
                QrLinkMode.ShowCode -> ShowQrCodePanel()
                QrLinkMode.ScanCode -> ScanQrCodePanel()
            }
        }
    }
}

@Composable
private fun ShowQrCodePanel() {
    val colorScheme = LocalCipherChatColorScheme.current
    var expiresInSeconds by remember { mutableStateOf(60) }
    var qrToken by remember { mutableStateOf<String?>(null) }

    // Fetch QR token and start expiry countdown.
    // TODO wire to ViewModel: apiClient.generateDeviceLinkQrToken()
    // -> set qrToken; on expiry auto-refresh.
    LaunchedEffect(Unit) {
        qrToken = "placeholder_token_for_demo"
        while (expiresInSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            expiresInSeconds--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(CcSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Show this to the device you want to link",
            style = CcTextStyles.bodyLarge,
            color = colorScheme.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = CcSpacing.xl),
        )

        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f),
            shape = CcRadius.shapeLg,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CcSpacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                if (qrToken != null) {
                    // Placeholder QR visual — in production, replace with
                    // a real QR code library (e.g. zxing-kotlin or a
                    // platform-native QR renderer via expect/actual).
                    // The QR encodes the token as a cipherchat://link/TOKEN
                    // deep link so scanning with any camera app on the new
                    // device opens CipherChat and hands off the token.
                    QrCodePlaceholder(token = qrToken!!)
                } else {
                    Text("Generating code...", style = CcTextStyles.bodyMedium, color = colorScheme.onSurfaceMuted)
                }
            }
        }

        val expiryColor = if (expiresInSeconds <= 10) colorScheme.error else colorScheme.onSurfaceMuted
        Text(
            text = if (expiresInSeconds > 0) "Code expires in ${expiresInSeconds}s" else "Code expired — refreshing…",
            style = CcTextStyles.labelLarge,
            color = expiryColor,
            modifier = Modifier.padding(top = CcSpacing.xl),
        )

        Text(
            text = "Scan once — this code can't be reused.",
            style = CcTextStyles.bodyMedium,
            color = colorScheme.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = CcSpacing.sm),
        )
    }
}

@Composable
private fun ScanQrCodePanel() {
    val colorScheme = LocalCipherChatColorScheme.current

    // Scanning line animation — gives the impression the scanner is
    // actively looking even before a QR enters frame.
    val transition = rememberInfiniteTransition(label = "scanLine")
    val scanLineY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanLineY",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(CcSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Point at the QR code on your other device",
            style = CcTextStyles.bodyLarge,
            color = colorScheme.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = CcSpacing.xl),
        )

        // Camera viewfinder placeholder — in production, wire to a
        // platform camera API via expect/actual (CameraX on Android,
        // AVFoundation on iOS). The viewfinder Box below is sized and
        // shaped consistently with real scanner UX patterns.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.80f)
                .aspectRatio(1f)
                .clip(CcRadius.shapeLg)
                .background(colorScheme.surface)
                .border(width = 2.dp, color = colorScheme.accent, shape = CcRadius.shapeLg),
        ) {
            // Animated scan line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(colorScheme.accent.copy(alpha = 0.7f))
                    .graphicsLayer { translationY = scanLineY * (size.height - 2.dp.toPx()) },
            )

            // Corner guide marks — the four-corner bracket pattern
            // universally signals "align QR code here."
            ScannerCornerBrackets()
        }

        Text(
            text = "Camera access required — used only for scanning.",
            style = CcTextStyles.labelSmall,
            color = colorScheme.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = CcSpacing.lg),
        )
    }
}

@Composable
private fun QrCodePlaceholder(token: String) {
    val colorScheme = LocalCipherChatColorScheme.current
    // Visual stand-in for a real QR code — a grid of gold squares that
    // reads as "QR-like" at a glance without being a functional barcode.
    // Replace with a real QR library before shipping.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.QrCode,
            contentDescription = null,
            tint = colorScheme.onSurface,
            modifier = Modifier.fillMaxSize(0.7f),
        )
    }
}

@Composable
private fun ScannerCornerBrackets() {
    val colorScheme = LocalCipherChatColorScheme.current
    val bracketSize = 24.dp
    val bracketWidth = 3.dp
    val bracketColor = colorScheme.accent

    val bracketShape = RoundedCornerShape(CcRadius.xs)

    // Top-left
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left horizontal
        Box(modifier = Modifier
            .size(bracketSize, bracketWidth)
            .clip(bracketShape)
            .background(bracketColor)
            .align(Alignment.TopStart))
        // Top-left vertical
        Box(modifier = Modifier
            .size(bracketWidth, bracketSize)
            .clip(bracketShape)
            .background(bracketColor)
            .align(Alignment.TopStart))
        // Top-right horizontal
        Box(modifier = Modifier
            .size(bracketSize, bracketWidth)
            .clip(bracketShape)
            .background(bracketColor)
            .align(Alignment.TopEnd))
        // Top-right vertical
        Box(modifier = Modifier
            .size(bracketWidth, bracketSize)
            .clip(bracketShape)
            .background(bracketColor)
            .align(Alignment.TopEnd))
        // Bottom-left horizontal
        Box(modifier = Modifier
            .size(bracketSize, bracketWidth)
            .clip(bracketShape)
            .background(bracketColor)
            .align(Alignment.BottomStart))
        // Bottom-left vertical
        Box(modifier = Modifier
            .size(bracketWidth, bracketSize)
            .clip(bracketShape)
            .background(bracketColor)
            .align(Alignment.BottomStart))
        // Bottom-right horizontal
        Box(modifier = Modifier
            .size(bracketSize, bracketWidth)
            .clip(bracketShape)
            .background(bracketColor)
            .align(Alignment.BottomEnd))
        // Bottom-right vertical
        Box(modifier = Modifier
            .size(bracketWidth, bracketSize)
            .clip(bracketShape)
            .background(bracketColor)
            .align(Alignment.BottomEnd))
    }
}

private enum class QrLinkMode { ShowCode, ScanCode }
