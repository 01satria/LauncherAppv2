package id.satria.launcher.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import id.satria.launcher.data.AppData
import id.satria.launcher.ui.theme.SatriaColors
import androidx.compose.material3.Text

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Dock(
    dockApps: List<AppData>,
    avatarPath: String?,
    onAvatarClick: () -> Unit,
    onAppPress: (String) -> Unit,
    onAppLongPress: (String) -> Unit,
    onLongPressSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .background(SatriaColors.DockBg, RoundedCornerShape(26.dp))
                .border(1.dp, SatriaColors.BorderLight, RoundedCornerShape(26.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPressSettings,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Avatar — tap buka Dashboard
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SatriaColors.Surface)
                    .combinedClickable(onClick = onAvatarClick, onLongClick = onLongPressSettings),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarPath != null) {
                    AsyncImage(model = avatarPath, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Text("👤", fontSize = 24.sp)
                }
            }

            // Separator
            if (dockApps.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(SatriaColors.BorderLight)
                )
            }

            // Dock apps
            dockApps.forEach { app ->
                DockAppItem(
                    app         = app,
                    onPress     = onAppPress,
                    onLongPress = onAppLongPress,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockAppItem(
    app: AppData,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.8f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "dockScale",
    )

    val bitmap = remember(app.packageName) { app.icon.toBitmap(56, 56).asImageBitmap() }

    Image(
        bitmap = bitmap,
        contentDescription = app.label,
        modifier = Modifier
            .size(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onPress(app.packageName) },
                onLongClick = { onLongPress(app.packageName) },
            ),
    )
}
