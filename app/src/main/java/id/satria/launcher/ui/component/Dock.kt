package id.satria.launcher.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import id.satria.launcher.data.AppData
import id.satria.launcher.ui.theme.SatriaColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Dock(
    dockApps: List<AppData>,
    avatarPath: String?,
    avatarVersion: Int = 0,
    dockIconSize: Int = 56,
    onAvatarClick: () -> Unit,
    onAppPress: (String) -> Unit,
    onAppLongPress: (String) -> Unit,
    onLongPressSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .background(SatriaColors.DockBg, RoundedCornerShape(28.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            dockApps.forEach { app ->
                DockIcon(
                    app          = app,
                    dockIconSize = dockIconSize,
                    onPress      = onAppPress,
                    onLongPress  = onAppLongPress,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockIcon(
    app: AppData,
    dockIconSize: Int = 56,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.75f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow,
        ),
        label = "dockScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.65f else 1f,
        animationSpec = tween(60),
        label = "dockAlpha",
    )

    val bitmap = remember(app.packageName) {
        iconCache.get(app.packageName) ?: run {
            val bmp = app.icon.toBitmap(96, 96, Bitmap.Config.ARGB_8888).asImageBitmap()
            iconCache.put(app.packageName, bmp)
            bmp
        }
    }

    Image(
        bitmap             = bitmap,
        contentDescription = app.label,
        contentScale       = ContentScale.Fit,
        filterQuality      = FilterQuality.Medium,
        modifier           = Modifier
            .size(dockIconSize.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(RoundedCornerShape((dockIconSize * 0.25f).dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = { onPress(app.packageName) },
                onLongClick       = { onLongPress(app.packageName) },
            ),
    )
}
