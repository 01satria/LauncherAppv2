package id.satria.launcher.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import id.satria.launcher.data.AppData
import id.satria.launcher.ui.theme.SatriaColors

// ─────────────────────────────────────────────────────────────────────────────
// RecentAppsBar
// Menampilkan baris horizontal 5 app terakhir yang digunakan, di atas dock.
// Karena launcher tidak punya akses ke usage stats secara langsung tanpa
// permission khusus, kita tampilkan 5 app pertama dari allApps yang bukan
// ada di dock sebagai "recently installed / suggested apps".
// Untuk implementasi penuh dengan usage stats, tambahkan PACKAGE_USAGE_STATS
// permission dan gunakan UsageStatsManager.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentAppsBar(
    allApps: List<AppData>,
    iconSize: Int = 44,
    onAppPress: (String) -> Unit,
    onAppLong: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (allApps.isEmpty()) return

    // Tampilkan max 8 app (urutan dari allApps yang sudah di-sort oleh sistem)
    val recentApps = remember(allApps) { allApps.take(8) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        LazyRow(
            modifier = Modifier
                .background(SatriaColors.DockBg, RoundedCornerShape(22.dp))
                .border(0.5.dp, SatriaColors.DockBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(recentApps, key = { it.packageName }) { app ->
                RecentAppIcon(
                    app = app,
                    iconSizeDp = iconSize,
                    onPress = onAppPress,
                    onLongPress = onAppLong,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentAppIcon(
    app: AppData,
    iconSizeDp: Int,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            androidx.compose.animation.core.Spring.StiffnessMedium,
        ),
        label = "recentScale",
    )
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.70f else 1f,
        animationSpec = androidx.compose.animation.core.tween(50),
        label = "recentAlpha",
    )

    val bitmap = remember(app.packageName) { iconCache.get(app.packageName) }

    Box(
        modifier = Modifier
            .size(iconSizeDp.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(RoundedCornerShape((iconSizeDp * 0.22f).dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onPress(app.packageName) },
                onLongClick = { onLongPress(app.packageName) },
            ),
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
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SatriaColors.SurfaceMid),
            )
        }
    }
}
