package id.satria.launcher.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.ui.theme.SatriaColors
import kotlin.math.roundToInt

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
    // Drag state — tidak pakai Animated karena finishedListener lebih reliable
    var dragY by remember { mutableStateOf(0f) }
    val closing = remember { mutableStateOf(false) }

    val animY by animateFloatAsState(
        targetValue = dragY,
        animationSpec = if (closing.value)
            tween(durationMillis = 280, easing = FastOutLinearInEasing)
        else
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "sheetY",
        finishedListener = { if (closing.value) onClose() }
    )

    fun dismiss() {
        if (!closing.value) {
            closing.value = true
            dragY = 900f
        }
    }

    val dimAlpha = (0.65f * (1f - (dragY / 600f).coerceIn(0f, 1f)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Dim scrim — tap untuk tutup
            .background(Color.Black.copy(alpha = dimAlpha))
            // PENTING: pointerInput ini menangkap semua sentuhan di layer ini
            // Tidak ada sentuhan yang bisa "tembus" ke bawah (HomeScreen)
            .pointerInput(Unit) {
                detectTapGestures { dismiss() }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, animY.roundToInt().coerceAtLeast(0)) }
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(SatriaColors.Surface)
                .navigationBarsPadding()
                // PENTING: consume tap agar tidak trigger dismiss() di scrim
                .pointerInput(Unit) {
                    detectTapGestures { /* consume — jangan dismiss */ }
                }
                // Drag ke bawah untuk tutup
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, delta ->
                            if (!closing.value)
                                dragY = (dragY + delta).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (dragY > 120f) dismiss() else dragY = 0f
                        },
                        onDragCancel = { dragY = 0f },
                    )
                }
                .padding(bottom = 10.dp),
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SatriaColors.SurfaceHigh)
            )

            // App label
            Text(
                text = label,
                color = SatriaColors.TextPrimary,
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            )

            HorizontalDivider(color = SatriaColors.Border, thickness = 0.5.dp)

            // Dock
            val dockLabel = when {
                isDocked -> "📌  Unpin from Dock"
                dockFull -> "📌  Dock is full (max 4)"
                else     -> "📌  Pin to Dock"
            }
            SheetButton(dockLabel, enabled = isDocked || !dockFull) { onDock(); dismiss() }

            HorizontalDivider(color = SatriaColors.Border, thickness = 0.5.dp)

            // Hide / Unhide
            SheetButton(if (isHidden) "👁  Show App" else "🙈  Hide App") {
                if (isHidden) onUnhide() else onHide(); dismiss()
            }

            HorizontalDivider(color = SatriaColors.Border, thickness = 0.5.dp)

            // Uninstall
            SheetButton("🗑  Uninstall", color = SatriaColors.Danger) { onUninstall(); dismiss() }

            HorizontalDivider(color = SatriaColors.Border, thickness = 0.5.dp)

            // Cancel
            SheetButton("Cancel", color = SatriaColors.TextSecondary) { dismiss() }
        }
    }
}

@Composable
private fun SheetButton(
    text: String,
    color: Color = SatriaColors.TextPrimary,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    ) {
        Text(
            text = text,
            color = if (enabled) color else SatriaColors.TextTertiary,
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    }
}