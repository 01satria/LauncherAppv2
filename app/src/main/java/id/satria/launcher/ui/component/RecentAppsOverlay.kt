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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import id.satria.launcher.data.AppData
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// RecentAppsOverlay — One Hand Operation+ Task Switcher Style
//
// Layout:
//   • Full-screen dark scrim
//   • Horizontal scrollable row of tall portrait cards (140×252dp)
//   • Each card: colored preview area + large icon + bottom name bar + X button
//   • Swipe up on card OR tap X = close single app
//   • Clear All pill button at very bottom
// ─────────────────────────────────────────────────────────────────────────────

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
    onDismissOne: (String) -> Unit = {},
) {
    val appMap      = remember(allApps) { allApps.associateBy { it.packageName } }
    val initialApps = remember(recentPackages, appMap) {
        recentPackages.mapNotNull { appMap[it] }.take(15)
    }

    // Per-card dismissed state
    val dismissed = remember { mutableStateListOf<String>() }
    val allDismissed by remember { derivedStateOf { initialApps.all { it.packageName in dismissed } } }

    // Scrim — tap outside panel → dismiss
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.BottomCenter,
    ) {

        // ── Panel ─────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures { /* absorb */ } }
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            when {
                !hasPermission -> {
                    Spacer(Modifier.height(40.dp))
                    PermissionPrompt(onRequestPermission)
                    Spacer(Modifier.height(40.dp))
                }

                initialApps.isEmpty() || allDismissed -> {
                    Spacer(Modifier.height(80.dp))
                    EmptyState()
                    Spacer(Modifier.height(80.dp))
                }

                else -> {
                    // ── Cards row ──────────────────────────────────────────────
                    LazyRow(
                        state = rememberLazyListState(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                    ) {
                        itemsIndexed(initialApps, key = { _, app -> app.packageName }) { index, app ->
                            AnimatedVisibility(
                                visible = app.packageName !in dismissed,
                                enter   = EnterTransition.None,
                                exit    = shrinkHorizontally(
                                    shrinkTowards = Alignment.CenterHorizontally,
                                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                                ) + fadeOut(tween(200)) + scaleOut(targetScale = 0.7f),
                            ) {
                                // Stagger entrance via delay
                                val enterAlpha by animateFloatAsState(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = 280,
                                        delayMillis    = (index * 40).coerceAtMost(300),
                                        easing         = FastOutSlowInEasing,
                                    ),
                                    label = "enter$index",
                                )
                                TaskCard(
                                    app       = app,
                                    enterAlpha = enterAlpha,
                                    onPress   = { onAppPress(app.packageName) },
                                    onLong    = { onAppLong(app.packageName) },
                                    onClose   = {
                                        dismissed.add(app.packageName)
                                        onDismissOne(app.packageName)
                                    },
                                )
                            }
                        }
                    }

                    // ── Clear All pill ─────────────────────────────────────────
                    Spacer(Modifier.height(4.dp))
                    ClearAllButton(onClick = {
                        val remaining = initialApps.map { it.packageName }
                            .filter { it !in dismissed }
                        dismissed.addAll(remaining)
                        onClearAll()
                    })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TaskCard — OHO+ style portrait card
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(
    app: AppData,
    enterAlpha: Float,
    onPress: () -> Unit,
    onLong: () -> Unit,
    onClose: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "ps",
    )

    // Swipe-up-to-dismiss
    var rawDragY by remember { mutableFloatStateOf(0f) }
    val animDragY by animateFloatAsState(
        targetValue = rawDragY,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "dy",
    )
    val dragProgress = (-animDragY.coerceAtMost(0f) / 220f).coerceIn(0f, 1f)
    val dragAlpha    = (1f - dragProgress * 0.7f)
    val dragScale    = (1f - dragProgress * 0.18f)

    val bitmap = remember(app.packageName) { iconCache.get(app.packageName) }

    // Derive a unique hue from package name for the preview gradient
    val cardHue = remember(app.packageName) {
        (abs(app.packageName.hashCode()) % 360).toFloat()
    }
    val previewTop    = Color.hsl(cardHue, 0.28f, 0.14f)
    val previewBottom = Color.hsl(cardHue, 0.22f, 0.10f)

    Box(
        modifier = Modifier
            .width(140.dp)
            .height(252.dp)
            .graphicsLayer {
                alpha        = enterAlpha * dragAlpha
                translationY = animDragY
                scaleX       = pressScale * dragScale
                scaleY       = pressScale * dragScale
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (rawDragY < -70f) onClose() else rawDragY = 0f
                    },
                    onDragCancel  = { rawDragY = 0f },
                    onVerticalDrag = { _, delta ->
                        if (rawDragY + delta < 0f)
                            rawDragY = (rawDragY + delta).coerceAtLeast(-300f)
                    },
                )
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onPress,
                onLongClick       = onLong,
            ),
    ) {
        // ── Card shell ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
        ) {
            // ── Preview area (top ~75%) ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(previewTop, previewBottom)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // App icon — large
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap           = bitmap,
                                contentDescription = app.label,
                                contentScale     = ContentScale.Fit,
                                filterQuality    = FilterQuality.High,
                                modifier         = Modifier.fillMaxSize().padding(6.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    // Fake UI skeleton lines — simulate app content
                    FakeUiLines(cardHue)
                }
            }

            // ── Bottom name bar ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.25f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF111114),
                                Color(0xFF0D0D10),
                            )
                        )
                    )
                    .padding(horizontal = 10.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Small icon
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap           = bitmap,
                            contentDescription = null,
                            contentScale     = ContentScale.Fit,
                            filterQuality    = FilterQuality.Medium,
                            modifier         = Modifier.fillMaxSize(),
                        )
                    }
                }
                Text(
                    text     = app.label,
                    color    = Color.White.copy(alpha = 0.88f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            // ── Divider line between preview and name bar ─────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .offset(y = (-63).dp)
                    .background(Color.White.copy(alpha = 0.07f))
            )
        }

        // ── X close button (overlaid, top-right) ──────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onClose,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(9.dp)) {
                val s = size
                // × shape
                drawLine(Color.White.copy(alpha = 0.9f), start = Offset(0f, 0f), end = Offset(s.width, s.height), strokeWidth = 1.6f, cap = StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.9f), start = Offset(s.width, 0f), end = Offset(0f, s.height), strokeWidth = 1.6f, cap = StrokeCap.Round)
            }
        }
    }
}

// ── Fake UI skeleton lines inside preview ─────────────────────────────────────
@Composable
private fun FakeUiLines(hue: Float) {
    val lineColor = Color.hsl(hue, 0.20f, 0.30f)
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.width(88.dp),
    ) {
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (i == 1) 0.65f else if (i == 2) 0.45f else 0.85f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(lineColor),
            )
        }
    }
}

// ── Clear All pill button ─────────────────────────────────────────────────────
@Composable
private fun ClearAllButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "cas",
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(0.8.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50.dp))
            .clickable(
                indication        = null,
                interactionSource = interactionSource,
                onClick           = onClick,
            )
            .padding(horizontal = 32.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(Modifier.size(14.dp)) {
                val c = center
                val r = size.minDimension * 0.42f
                drawCircle(Color.White.copy(alpha = 0.75f), radius = r, style = Stroke(width = 1.5f))
                // × inside circle
                val off = r * 0.52f
                drawLine(Color.White.copy(alpha = 0.75f), start = Offset(c.x - off, c.y - off), end = Offset(c.x + off, c.y + off), strokeWidth = 1.5f, cap = StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.75f), start = Offset(c.x + off, c.y - off), end = Offset(c.x - off, c.y + off), strokeWidth = 1.5f, cap = StrokeCap.Round)
            }
            Text(
                text       = "Clear All",
                color      = Color.White.copy(alpha = 0.82f),
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            )
        }
    }
}

// ── Permission Prompt ─────────────────────────────────────────────────────────
@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("🔒", fontSize = 32.sp)
        Text(
            "Usage Stats Required",
            color      = Color.White,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Allow access to Usage Statistics to enable the recent apps panel.",
            color     = Color.White.copy(alpha = 0.50f),
            fontSize  = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFF27AE60))
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onRequestPermission,
                )
                .padding(horizontal = 28.dp, vertical = 12.dp),
        ) {
            Text("Open Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("✓", color = Color.White.copy(alpha = 0.18f), fontSize = 44.sp)
        Text(
            "All caught up",
            color      = Color.White.copy(alpha = 0.30f),
            fontSize   = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            "No recent apps",
            color    = Color.White.copy(alpha = 0.18f),
            fontSize = 12.sp,
        )
    }
}
