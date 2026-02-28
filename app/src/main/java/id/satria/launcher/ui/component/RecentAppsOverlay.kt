package id.satria.launcher.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import id.satria.launcher.data.AppData

/**
 * RecentAppsOverlay — One Hand Operation+ style
 *
 * Panel dari bawah dengan kartu aplikasi portrait besar.
 * Swipe kartu ke atas → kartu terbang keluar & dihapus dari recents.
 * Tap kartu → launch app.
 */
@Composable
fun RecentAppsOverlay(
    recentPackages: List<String>,
    allApps: List<AppData>,
    hasPermission: Boolean,
    onAppPress: (String) -> Unit,
    onAppLong: (String) -> Unit,
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
    onClearAll: () -> Unit,
) {
    val appMap = remember(allApps) { allApps.associateBy { it.packageName } }
    val initialApps = remember(recentPackages, appMap) {
        recentPackages.mapNotNull { appMap[it] }.take(12)
    }

    // Packages that have been locally dismissed via swipe
    // Use a SnapshotStateList as a set to track dismissed packages with recomposition
    val dismissedPkgsList = remember { mutableStateListOf<String>() }
    val dismissedPkgs: Set<String> by remember { derivedStateOf { dismissedPkgsList.toSet() } }

    // Full-screen scrim — tap anywhere outside panel closes overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x000A0A0A),
                            Color(0xF00A0A0A),
                            Color(0xFF090909),
                        ),
                        startY = 0f,
                        endY = 380f,
                    )
                )
                .pointerInput(Unit) { detectTapGestures { /* absorb touches */ } }
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            // ── Handle ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp).height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                )
            }

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent Apps",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                )
                if (initialApps.isNotEmpty()) {
                    val interactionSrc = remember { MutableInteractionSource() }
                    Text(
                        text = "Close All",
                        color = Color.White.copy(alpha = 0.40f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                indication = null,
                                interactionSource = interactionSrc,
                            ) {
                                val toAdd = initialApps.map { a -> a.packageName }
                                    .filter { !dismissedPkgsList.contains(it) }
                                dismissedPkgsList.addAll(toAdd)
                                onClearAll()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            when {
                !hasPermission -> PermissionPrompt(onRequestPermission)
                initialApps.isEmpty() -> EmptyState()
                else -> {
                    LazyRow(
                        state = rememberLazyListState(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    ) {
                        items(initialApps, key = { it.packageName }) { app ->
                            // AnimatedVisibility keyed per-item so individual cards can exit
                            val dismissed = app.packageName in dismissedPkgs
                            AnimatedVisibility(
                                visible = !dismissed,
                                enter = scaleIn(
                                    initialScale = 0.85f,
                                    animationSpec = spring(Spring.DampingRatioMediumBouncy),
                                ) + fadeIn(tween(180)),
                                exit = slideOutVertically(
                                    targetOffsetY = { -it * 2 },
                                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                                ) + scaleOut(
                                    targetScale = 0.5f,
                                    animationSpec = tween(220),
                                ) + fadeOut(tween(200)),
                            ) {
                                OhoRecentCard(
                                    app = app,
                                    onPress = { onAppPress(app.packageName) },
                                    onLong  = { onAppLong(app.packageName) },
                                    onSwipeUp = {
                                        if (!dismissedPkgsList.contains(app.packageName))
                                            dismissedPkgsList.add(app.packageName)
                                    },
                                )
                            }
                        }
                    }

                    // Swipe hint text
                    Text(
                        text = "↑ Swipe card up to close",
                        color = Color.White.copy(alpha = 0.22f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}

// ── OHO+ Card ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OhoRecentCard(
    app: AppData,
    onPress: () -> Unit,
    onLong: () -> Unit,
    onSwipeUp: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "pressScale",
    )

    // Drag offset for swipe-up-to-dismiss
    var rawDragY by remember { mutableFloatStateOf(0f) }
    val animDragY by animateFloatAsState(
        targetValue = rawDragY,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "dragY",
    )

    // As card drags upward, it becomes smaller and more transparent
    val dragProgress = (-animDragY.coerceAtMost(0f) / 250f).coerceIn(0f, 1f)
    val dragAlpha    = (1f - dragProgress * 0.65f).coerceIn(0f, 1f)
    val dragScale    = (1f - dragProgress * 0.25f).coerceIn(0.6f, 1f)

    val bitmap = remember(app.packageName) { iconCache.get(app.packageName) }

    Box(
        modifier = Modifier
            .width(110.dp)
            .height(190.dp)
            .graphicsLayer {
                translationY = animDragY
                scaleX       = pressScale * dragScale
                scaleY       = pressScale * dragScale
                alpha        = dragAlpha
            }
            // Swipe-up-to-dismiss gesture (must be before combinedClickable)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (rawDragY < -75f) {
                            onSwipeUp()
                        } else {
                            rawDragY = 0f
                        }
                    },
                    onDragCancel = { rawDragY = 0f },
                    onVerticalDrag = { _, delta ->
                        val proposed = rawDragY + delta
                        if (proposed <= 0f) rawDragY = proposed.coerceAtLeast(-320f)
                    },
                )
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onPress,
                onLongClick       = onLong,
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Card background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF2C2C30), Color(0xFF1A1A1E))
                    )
                )
                .border(
                    width = 0.6.dp,
                    color = Color.White.copy(alpha = 0.09f),
                    shape = RoundedCornerShape(20.dp),
                ),
        )

        // Card content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp, bottom = 14.dp, start = 10.dp, end = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // App icon — large, centered
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.label,
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.High,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                    )
                }
            }

            // App label
            Text(
                text = app.label,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Swipe-up chevron indicator at top of card
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 7.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(10.dp)) {
                val w = size.width; val h = size.height
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                    width  = 1.8f,
                    cap    = StrokeCap.Round,
                    join   = StrokeJoin.Round,
                )
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.15f, h * 0.68f)
                        lineTo(w * 0.50f, h * 0.28f)
                        lineTo(w * 0.85f, h * 0.68f)
                    },
                    color = Color.White.copy(alpha = 0.50f),
                    style = stroke,
                )
            }
        }
    }
}

// ── Permission Prompt ─────────────────────────────────────────────────────────
@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "🔒 Permission Required",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Allow Usage Statistics access to load recent apps.",
            color = Color.White.copy(alpha = 0.50f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
        )
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(44.dp),
        ) {
            Text("Open Settings", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("✓", color = Color.White.copy(alpha = 0.20f), fontSize = 34.sp)
            Text(
                "No recent apps",
                color = Color.White.copy(alpha = 0.32f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
