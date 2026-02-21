package id.satria.launcher.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import id.satria.launcher.data.AppData
import id.satria.launcher.ui.theme.SatriaColors
import androidx.compose.foundation.Image

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: AppData,
    showName: Boolean,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "scale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .scale(scale)
            .combinedClickable(
                onClick = { onPress(app.packageName) },
                onLongClick = { onLongPress(app.packageName) },
            ),
    ) {
        // Icon — toBitmap di-cache oleh key packageName, tidak re-alokasi
        val bitmap = remember(app.packageName) {
            app.icon.toBitmap(55, 55).asImageBitmap()
        }
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier
                .size(55.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        if (showName) {
            Text(
                text = app.label,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = SatriaColors.TextPrimary.copy(alpha = 0.9f),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(70.dp),
            )
        }
    }
}
