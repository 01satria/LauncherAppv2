package id.satria.launcher.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
 * RecentAppsOverlay
 *
 * Panel bottom-sheet recent apps.
 * Dibuka via swipe dari tepi kiri layar (diatur di HomeScreen).
 *
 * RAM: recentApps dihitung sekali dengan remember(key), LazyRow hanya
 * compose item yang visible, iconCache sudah LruCache.
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
    // appMap di-remember — hanya rebuild jika allApps berubah
    val appMap = remember(allApps) { allApps.associateBy { it.packageName } }
    // recentApps di-remember — hanya rebuild jika packages atau map berubah
    val recentApps = remember(recentPackages, appMap) {
        recentPackages.mapNotNull { appMap[it] }.take(10)
    }

    // Full-screen scrim — tap di luar panel = dismiss
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.60f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Panel — blok tap agar tidak menutup overlay saat klik isi panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0D0D0D)),
                        startY = 0f,
                        endY = 280f,
                    )
                )
                .pointerInput(Unit) { detectTapGestures { /* blok passthrough */ } }
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // ── Drag handle ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp).height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.30f)),
                )
            }

            // ── Header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Recent Apps",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                // Clear All — selalu tampil jika ada item (fixed: sebelumnya tidak
                // benar-benar clear, sekarang onClearAll memanggil clearAll() di VM)
                if (recentApps.isNotEmpty()) {
                    Text(
                        "Clear All",
                        color = Color.White.copy(alpha = 0.50f),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onClearAll,
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // ── Content ──────────────────────────────────────────────────
            when {
                !hasPermission -> PermissionPrompt(onRequestPermission)
                recentApps.isEmpty() -> EmptyState()
                else -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Key by packageName — LazyRow tidak re-compose card yang tidak berubah
                        items(recentApps, key = { it.packageName }) { app ->
                            RecentAppCard(
                                app = app,
                                onPress = { onAppPress(app.packageName) },
                                onLong  = { onAppLong(app.packageName) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Card ──────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentAppCard(app: AppData, onPress: () -> Unit, onLong: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "scale",
    )

    // Icon di-pull dari LruCache — tidak re-load saat recompose
    val bitmap = remember(app.packageName) { iconCache.get(app.packageName) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onPress,
                onLongClick = onLong,
            ),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = app.label,
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.Medium,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = app.label,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Permission prompt ─────────────────────────────────────────────────────────
@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("🔒 Izin Diperlukan", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Izinkan akses Usage Statistics agar recent apps bisa dimuat dari luar launcher.",
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
        )
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Buka Settings", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Belum ada app yang digunakan",
            color = Color.White.copy(alpha = 0.40f),
            fontSize = 14.sp,
        )
    }
}
