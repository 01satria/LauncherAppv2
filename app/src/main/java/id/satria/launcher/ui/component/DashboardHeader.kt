package id.satria.launcher.ui.component

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import id.satria.launcher.ui.theme.SatriaColors
import id.satria.launcher.utils.getAssistantMessage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Pill height constant — all three pills share the same height
private val PILL_HEIGHT = 36.dp

@Composable
fun DashboardHeader(
    avatarPath: String?,
    assistantName: String,
    userName: String,
    onAvatarClick: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    // ── Clock & Date ───────────────────────────────────────────────────────
    var clockStr by remember { mutableStateOf(getClockStr()) }
    var dateStr  by remember { mutableStateOf(getDateStr()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            clockStr = getClockStr()
            dateStr  = getDateStr()
        }
    }

    // ── Message ────────────────────────────────────────────────────────────
    var message by remember(userName) { mutableStateOf(getAssistantMessage(userName)) }
    LaunchedEffect(userName) {
        while (true) { delay(60_000); message = getAssistantMessage(userName) }
    }

    // ── Battery via BroadcastReceiver (zero polling) ───────────────────────
    var batteryPct by remember { mutableIntStateOf(getBatteryLevel(context)) }
    var isCharging by remember { mutableStateOf(getBatteryCharging(context)) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) batteryPct = level * 100 / scale
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                             status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    val batteryColor = when {
        isCharging       -> Color(0xFF30D158)
        batteryPct <= 20 -> Color(0xFFFF453A)
        batteryPct <= 50 -> Color(0xFFFFB74D)
        else             -> SatriaColors.TextSecondary
    }
    val batteryIcon = if (isCharging) "⚡" else when {
        batteryPct <= 20 -> "🪫"
        else             -> "🔋"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        // ── Top bar: three equal-height pills ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PILL_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Pill 1 — Avatar + assistant name
            Row(
                modifier = Modifier
                    .height(PILL_HEIGHT)
                    .clip(RoundedCornerShape(50.dp))
                    .background(SatriaColors.SurfaceMid)
                    .clickable { onAvatarClick() }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(SatriaColors.Surface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatarPath != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(avatarPath).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Text("🤖", fontSize = 12.sp)
                    }
                }
                Text(
                    assistantName,
                    color      = SatriaColors.TextPrimary,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Right side pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Pill 2 — Battery
                Row(
                    modifier = Modifier
                        .height(PILL_HEIGHT)
                        .clip(RoundedCornerShape(50.dp))
                        .background(SatriaColors.SurfaceMid)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(batteryIcon, fontSize = 12.sp)
                    Text(
                        "$batteryPct%",
                        color      = batteryColor,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Pill 3 — Close (same height as other pills, consistent shape)
                Box(
                    modifier = Modifier
                        .height(PILL_HEIGHT)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(SatriaColors.SurfaceMid)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = SatriaColors.TextSecondary, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Big clock ──────────────────────────────────────────────────────
        Text(
            text          = clockStr,
            color         = SatriaColors.TextPrimary,
            fontSize      = 56.sp,
            fontWeight    = FontWeight.Thin,
            letterSpacing = 2.sp,
        )

        Text(
            text       = dateStr,
            color      = SatriaColors.TextSecondary,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Normal,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text       = message,
            color      = SatriaColors.TextTertiary,
            fontSize   = 12.sp,
            lineHeight  = 18.sp,
        )
    }
}

private fun getClockStr(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun getDateStr(): String =
    SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())

private fun getBatteryLevel(context: Context): Int {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level  = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale  = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    return if (level >= 0 && scale > 0) level * 100 / scale else 0
}

private fun getBatteryCharging(context: Context): Boolean {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return status == BatteryManager.BATTERY_STATUS_CHARGING ||
           status == BatteryManager.BATTERY_STATUS_FULL
}
