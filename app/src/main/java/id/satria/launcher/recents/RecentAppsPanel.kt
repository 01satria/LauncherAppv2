package id.satria.launcher.recents

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*

/**
 * RecentAppsPanel — Composable yang ditampilkan di dalam HomeScreen.
 *
 * Cara kerja:
 *  - Ditampilkan DARI DALAM launcher (bukan overlay/service terpisah)
 *  - Data diambil dari UsageStatsManager (recent apps sistem yang real)
 *  - Dipanggil ketika launcher mendapat fokus kembali (onResume)
 *    dan user memilih untuk membuka recent apps
 *
 * Trigger yang bisa dipakai (pilih salah satu sesuai kebutuhan):
 *  A) Long press tombol Home (launcher sudah intercept via onLongClick)
 *  B) Double tap background launcher
 *  C) Tombol/gesture di dalam UI launcher
 *
 * Panel ini SUDAH BERFUNGSI karena data dari UsageStatsManager,
 * tidak bergantung pada tracking manual yang bisa kosong.
 */
@Composable
fun RecentAppsPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    onLaunch: (String) -> Unit,
) {
    val context = LocalContext.current
    val hasPermission = remember { RecentAppsManager.hasPermission(context) }

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(150)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec  = tween(250, easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f))
        ),
        exit    = fadeOut(tween(120)) + slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = tween(180, easing = CubicBezierEasing(0.32f, 0f, 0.67f, 0f))
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.50f))
            )

            // Panel
            Box(
                modifier         = Modifier.align(Alignment.BottomCenter),
                contentAlignment = Alignment.BottomCenter,
            ) {
                if (!hasPermission) {
                    PermissionPrompt(context = context, onDismiss = onDismiss)
                } else {
                    RecentsList(
                        context   = context,
                        onDismiss = onDismiss,
                        onLaunch  = onLaunch,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentsList(
    context: Context,
    onDismiss: () -> Unit,
    onLaunch: (String) -> Unit,
) {
    // Load dari UsageStatsManager setiap kali panel dibuka
    val recents = remember {
        RecentAppsManager.getRecents(context, limit = 8)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xF01C1C1E),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            )
            .pointerInput(Unit) { detectTapGestures { /* block */ } }
            .padding(top = 14.dp, bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .width(40.dp).height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
        )

        Spacer(Modifier.height(16.dp))

        // Header
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                "Recent Apps",
                color      = Color.White,
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "✕ Tutup",
                color      = Color.White.copy(alpha = 0.45f),
                fontSize   = 12.sp,
                modifier   = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) { onDismiss() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        if (recents.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Belum ada app yang baru dibuka",
                    color    = Color.White.copy(alpha = 0.38f),
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.fillMaxWidth(),
            ) {
                items(recents, key = { it.packageName }) { app ->
                    RecentAppItem(
                        app     = app,
                        context = context,
                        onClick = { onLaunch(app.packageName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionPrompt(context: Context, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xF01C1C1E),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            )
            .pointerInput(Unit) { detectTapGestures { /* block */ } }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("🔐", fontSize = 32.sp)
        Text(
            "Izin Usage Access Diperlukan",
            color      = Color.White,
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Untuk menampilkan recent apps, launcher perlu izin melihat penggunaan aplikasi. Tap tombol di bawah untuk membuka pengaturan.",
            color     = Color.White.copy(alpha = 0.60f),
            fontSize  = 13.sp,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2C2C2E))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) { onDismiss() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Batal", color = Color.White.copy(alpha = 0.60f), fontSize = 14.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF27AE60))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) {
                        context.startActivity(
                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Buka Pengaturan", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun RecentAppItem(
    app: RecentAppsManager.RecentApp,
    context: Context,
    onClick: () -> Unit,
) {
    val icon: ImageBitmap? by remember(app.packageName) {
        derivedStateOf {
            runCatching {
                val d   = context.packageManager.getApplicationIcon(app.packageName)
                val bmp = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
                d.setBounds(0, 0, 108, 108)
                d.draw(Canvas(bmp))
                bmp.asImageBitmap()
            }.getOrNull()
        }
    }

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
            modifier         = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Image(
                    bitmap             = icon!!,
                    contentDescription = app.label,
                    modifier           = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Text(
                    app.label.take(1).uppercase(),
                    color      = Color.White,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text      = app.label,
            color     = Color.White.copy(alpha = 0.80f),
            fontSize  = 11.sp,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
