package id.satria.launcher.recents

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import android.graphics.Color as AndroidColor

/**
 * RecentsActivity — Activity yang didaftarkan sebagai handler tombol Recent.
 *
 * Cara kerja:
 *  - Di AndroidManifest, Activity ini didaftarkan dengan intent-filter
 *    ACTION_MAIN + CATEGORY_RECENTS
 *  - Saat user tekan tombol Recent (hardware/gesture), sistem langsung
 *    memanggil Activity ini — TANPA Accessibility Service
 *  - Activity tampil transparan penuh (windowIsTranslucent = true) sehingga
 *    terlihat seperti overlay di atas app yang sedang berjalan
 *
 * Ini cara resmi yang dipakai Launcher3 (AOSP), Pixel Launcher, dan Nova Launcher.
 */
class RecentsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transparan + tampil di atas semua tanpa mengganggu task stack
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        enableEdgeToEdge()

        val pm = packageManager

        setContent {
            RecentsScreen(
                recentPackages = RecentAppsStore.recentPackages,
                packageManager = pm,
                onDismiss      = { finish() },
                onLaunch       = { pkg ->
                    finish()
                    pm.getLaunchIntentForPackage(pkg)?.let {
                        it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(it)
                    }
                },
                onClearAll     = {
                    RecentAppsStore.recentPackages.clear()
                    finish()
                },
            )
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Jika Activity sudah terbuka dan user tekan Recent lagi → tutup
        finish()
    }
}

// ── UI ────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentsScreen(
    recentPackages: List<RecentAppEntry>,
    packageManager: PackageManager,
    onDismiss: () -> Unit,
    onLaunch: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        // Scrim semi-transparan
        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(180)),
            exit    = fadeOut(tween(130)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.50f))
            )
        }

        // Panel slide dari bawah
        AnimatedVisibility(
            visible  = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter    = slideInVertically(
                initialOffsetY = { it },
                animationSpec  = tween(260, easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f))
            ),
            exit     = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(180, easing = CubicBezierEasing(0.32f, 0f, 0.67f, 0f))
            ),
        ) {
            RecentsPanel(
                recentPackages = recentPackages,
                packageManager = packageManager,
                onLaunch       = onLaunch,
                onClearAll     = onClearAll,
            )
        }
    }
}

@Composable
private fun RecentsPanel(
    recentPackages: List<RecentAppEntry>,
    packageManager: PackageManager,
    onLaunch: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xF01C1C1E),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            )
            .pointerInput(Unit) { detectTapGestures { /* block */ } }
            .padding(top = 14.dp, bottom = 34.dp),
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
                modifier         = Modifier.fillMaxWidth().height(110.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🕐", fontSize = 30.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Belum ada app yang dibuka",
                        color    = Color.White.copy(alpha = 0.40f),
                        fontSize = 13.sp,
                    )
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
                val d   = packageManager.getApplicationIcon(entry.packageName)
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
            text      = entry.label,
            color     = Color.White.copy(alpha = 0.80f),
            fontSize  = 11.sp,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
