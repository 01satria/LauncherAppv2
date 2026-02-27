package id.satria.launcher.ui.overlay

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import id.satria.launcher.service.RecentAppEntry

@Composable
fun RecentAppsOverlay(
    recentPackages: List<RecentAppEntry>,
    onDismiss: () -> Unit,
    onLaunch: (String) -> Unit,
    onClearAll: () -> Unit,
    packageManager: PackageManager,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        // Scrim
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit  = fadeOut(tween(150)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }

        // Panel slide dari bawah
        AnimatedVisibility(
            visible  = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec  = tween(280, easing = EaseOutCubic)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200, easing = EaseInCubic)
            ),
        ) {
            RecentAppsPanel(
                recentPackages = recentPackages,
                onLaunch       = onLaunch,
                onClearAll     = onClearAll,
                packageManager = packageManager,
            )
        }
    }
}

@Composable
private fun RecentAppsPanel(
    recentPackages: List<RecentAppEntry>,
    onLaunch: (String) -> Unit,
    onClearAll: () -> Unit,
    packageManager: PackageManager,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xF01C1C1E),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            )
            .pointerInput(Unit) { detectTapGestures { /* block pass-through */ } }
            .padding(top = 14.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
        )

        Spacer(Modifier.height(16.dp))

        // Header
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text("Recent Apps", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (recentPackages.isNotEmpty()) {
                Text(
                    "Clear All",
                    color      = Color(0xFF27AE60),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                        ) { onClearAll() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (recentPackages.isEmpty()) {
            Box(
                modifier            = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment    = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🕐", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No recent apps", color = Color.White.copy(alpha = 0.45f), fontSize = 14.sp)
                }
            }
        } else {
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxWidth(),
            ) {
                items(recentPackages, key = { it.packageName }) { entry ->
                    RecentAppItem(
                        entry          = entry,
                        packageManager = packageManager,
                        onClick        = { onLaunch(entry.packageName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentAppItem(
    entry: RecentAppEntry,
    packageManager: PackageManager,
    onClick: () -> Unit,
) {
    val icon: ImageBitmap? by remember(entry.packageName) {
        derivedStateOf {
            runCatching {
                val drawable = packageManager.getApplicationIcon(entry.packageName)
                val bmp      = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
                drawable.setBounds(0, 0, 108, 108)
                drawable.draw(Canvas(bmp))
                bmp.asImageBitmap()
            }.getOrNull()
        }
    }

    val scale by animateFloatAsState(
        targetValue    = 1f,
        animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label          = "scale"
    )

    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
            ) { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Image(
                    bitmap             = icon!!,
                    contentDescription = entry.label,
                    modifier           = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Text(
                    entry.label.take(1).uppercase(),
                    color      = Color.White,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text     = entry.label,
            color    = Color.White.copy(alpha = 0.80f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val EaseInCubic  = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)
