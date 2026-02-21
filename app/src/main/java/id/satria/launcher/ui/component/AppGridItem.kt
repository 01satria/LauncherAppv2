package id.satria.launcher.ui.component

import android.graphics.Bitmap
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
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import id.satria.launcher.data.AppData
import id.satria.launcher.ui.theme.SatriaColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: AppData,
    showName: Boolean,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow,
        ),
        label = "iconScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.7f else 1f,
        animationSpec = tween(60),
        label = "iconAlpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(6.dp)
            .scale(scale)
            .alpha(alpha)
            .combinedClickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = { onPress(app.packageName) },
                onLongClick       = { onLongPress(app.packageName) },
            ),
    ) {
        // ── RAM FIX: ukuran 96px (turun dari 120px) — masih tajam di xxhdpi ──
        // 96px icon: 96*96*4 = 36 KB vs 120px: 120*120*4 = 56 KB (~36% hemat)
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
            filterQuality      = FilterQuality.Medium, // turun dari High — tidak kentara bedanya
            modifier           = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(13.dp)),
        )

        if (showName) {
            Text(
                text      = app.label,
                fontSize  = 11.sp,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color     = SatriaColors.TextPrimary.copy(alpha = 0.88f),
                modifier  = Modifier
                    .padding(top = 5.dp)
                    .width(66.dp),
            )
        }
    }
}