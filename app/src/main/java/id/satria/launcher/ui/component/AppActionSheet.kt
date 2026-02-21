package id.satria.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.ui.theme.SatriaColors

@Composable
fun AppActionSheet(
    pkg: String,
    label: String,
    isHidden: Boolean,
    isDocked: Boolean,
    dockFull: Boolean,
    onClose: () -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
    onDock: () -> Unit,
    onUninstall: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SatriaColors.Surface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(SatriaColors.SurfaceHigh, RoundedCornerShape(2.dp))
            )

            // App name
            Text(
                text = label,
                color = SatriaColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )

            HorizontalDivider(color = SatriaColors.Border)

            // Dock action
            val dockLabel = when {
                isDocked         -> "📌 Unpin from Dock"
                dockFull         -> "📌 Dock is full (max 5)"
                else             -> "📌 Pin to Dock"
            }
            ActionSheetButton(
                text    = dockLabel,
                enabled = isDocked || !dockFull,
                onClick = { onDock(); onClose() },
            )

            HorizontalDivider(color = SatriaColors.Border)

            // Hide / Unhide
            ActionSheetButton(
                text    = if (isHidden) "👁 Show App" else "🙈 Hide App",
                onClick = { if (isHidden) onUnhide() else onHide(); onClose() },
            )

            HorizontalDivider(color = SatriaColors.Border)

            // Uninstall
            ActionSheetButton(
                text    = "🗑 Uninstall",
                color   = SatriaColors.Danger,
                onClick = { onUninstall(); onClose() },
            )

            HorizontalDivider(color = SatriaColors.Border)

            // Cancel
            ActionSheetButton(
                text    = "Cancel",
                color   = SatriaColors.TextSecondary,
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun ActionSheetButton(
    text: String,
    color: Color = SatriaColors.TextPrimary,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        Text(
            text = text,
            color = if (enabled) color else SatriaColors.TextTertiary,
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}
