package id.satria.launcher.ui.component

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import id.satria.launcher.data.AppData
import id.satria.launcher.ui.theme.SatriaColors

/**
 * RecentAppsOverlay
 *
 * Panel full-screen yang menampilkan daftar app yang terakhir digunakan.
 * Dipicu oleh swipe-up panjang dari tepi bawah layar (mirip gesture
 * "Overview" pada Android 10+ gestural navigation).
 *
 * @param recentPackages  Urutan packageName dari yang paling baru
 * @param allApps         Semua app terpasang (untuk resolve label & icon)
 * @param hasPermission   Apakah PACKAGE_USAGE_STATS permission sudah diberikan
 * @param onAppPress      Callback saat app ditekan
 * @param onAppLong       Callback long-press (show action sheet)
 * @param onDismiss       Callback untuk tutup overlay
 * @param onRequestPermission  Callback untuk buka Settings grant permission
 * @param onClearAll      Callback untuk clear semua recent (opsional)
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
    onClearAll: (() -> Unit)? = null,
) {
    // Build ordered list of AppData dari recentPackages
    val appMap = remember(allApps) { allApps.associateBy { it.packageName } }
    val recentApps = remember(recentPackages, appMap) {
        recentPackages.mapNotNull { appMap[it] }.take(10)
    }

    // Background scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Panel — bottom sheet style, tidak intercept tap ke luar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)),
                        startY = 0f,
                        endY = 300f,
                    )
                )
                .pointerInput(Unit) { detectTapGestures { /* block passthrough */ } }
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // ── Handle drag indicator ──────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f)),
                )
            }

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Recent Apps",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (onClearAll != null && recentApps.isNotEmpty()) {
                    Text(
                        "Clear All",
                        color = Color.White.copy(alpha = 0.55f),
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

            // ── Content ───────────────────────────────────────────────────
            if (!hasPermission) {
                // Belum ada permission — tampilkan prompt
                PermissionPrompt(onRequestPermission = onRequestPermission)
            } else if (recentApps.isEmpty()) {
                // Sudah punya permission tapi belum ada usage data
                EmptyState()
            } else {
                // Tampilkan card carousel recent apps
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    itemsIndexed(recentApps, key = { _, app -> app.packageName }) { index, app ->
                        RecentAppCard(
                            app = app,
                            onPress = { onAppPress(app.packageName) },
                            onLong = { onAppLong(app.packageName) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Card per app ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentAppCard(
    app: AppData,
    onPress: () -> Unit,
    onLong: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "cardScale",
    )

    val bitmap = remember(app.packageName) { iconCache.get(app.packageName) }
    val iconSize = 64

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onPress,
                onLongClick = onLong,
            ),
    ) {
        // App icon
        Box(
            modifier = Modifier
                .size(iconSize.dp)
                .clip(RoundedCornerShape((iconSize * 0.22f).dp))
                .background(Color.White.copy(alpha = 0.08f)),
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

        Spacer(Modifier.height(8.dp))

        // App label
        Text(
            text = app.label,
            color = Color.White.copy(alpha = 0.90f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
        Text(
            "🔒 Izin Diperlukan",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Untuk menampilkan recent apps, izinkan akses Usage Statistics di Settings.",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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

// ── Empty state ────────────────────────────────────────────────────────────────
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Belum ada app yang digunakan",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 14.sp,
        )
    }
}
