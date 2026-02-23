package id.satria.launcher.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.data.AppData
import id.satria.launcher.ui.theme.SatriaColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppData,
    showName: Boolean,
    iconSizeDp: Int = 50,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "listScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = { onPress(app.packageName) },
                onLongClick       = { onLongPress(app.packageName) },
            )
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Pakai 96px sama seperti AppGridItem — share cache, tidak realokasi
        val bitmap = remember(app.packageName) { iconCache.get(app.packageName) }
        if (bitmap != null) {
            Image(
                bitmap             = bitmap,
                contentDescription = app.label,
                contentScale       = ContentScale.Fit,
                filterQuality      = FilterQuality.Medium,
                modifier           = Modifier.size(iconSizeDp.dp).clip(RoundedCornerShape((iconSizeDp * 0.24f).dp)),
            )
        } else {
            Box(modifier = Modifier.size(iconSizeDp.dp))
        }
        if (showName) {
            Text(
                text       = app.label,
                color      = SatriaColors.TextPrimary,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.padding(start = 16.dp),
            )
        }
    }
}