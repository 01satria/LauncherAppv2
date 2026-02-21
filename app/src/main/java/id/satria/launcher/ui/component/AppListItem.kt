package id.satria.launcher.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import id.satria.launcher.data.AppData
import id.satria.launcher.ui.theme.SatriaColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppData,
    showName: Boolean,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "scale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = { onPress(app.packageName) },
                onLongClick = { onLongPress(app.packageName) },
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val bitmap = remember(app.packageName) { app.icon.toBitmap(52, 52).asImageBitmap() }
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(11.dp)),
        )
        if (showName) {
            Text(
                text = app.label,
                color = SatriaColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}
