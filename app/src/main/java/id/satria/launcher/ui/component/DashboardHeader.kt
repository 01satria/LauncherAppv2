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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

import androidx.compose.ui.platform.LocalConfiguration

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

    // ── Battery — BroadcastReceiver, zero polling ──────────────────────────
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

    val batteryIcon = if (isCharging) "⚡" else when {
        batteryPct <= 20 -> "🪫"
        else             -> "🔋"
    }

    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val avatarHeightDp = screenHeightDp / 3

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Avatar — full width, 1/3 screen height, fade bottom ───────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(avatarHeightDp)
                .clickable { onAvatarClick() },
        ) {
            if (avatarPath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            } else {
                // Placeholder jika belum ada avatar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SatriaColors.SurfaceMid),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("👤", fontSize = 64.sp)
                }
            }

            // Fade gradient di bagian bawah avatar — berbatasan dengan jam
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF000000)),
                        )
                    )
            )

            // Close button — pojok kanan atas
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            }

            // Assistant name di pojok kiri bawah avatar
            Text(
                text       = assistantName,
                color      = Color.White.copy(alpha = 0.80f),
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 12.dp),
            )
        }

        // ── Clock + date + battery ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 4.dp),
        ) {
            // Big clock
            Text(
                text          = clockStr,
                color         = SatriaColors.TextPrimary,
                fontSize      = 56.sp,
                fontWeight    = FontWeight.Thin,
                letterSpacing = 2.sp,
            )

            // Date + battery inline — font sama persis
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text       = dateStr,
                    color      = SatriaColors.TextSecondary,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Normal,
                )
                Text(
                    text       = "·",
                    color      = SatriaColors.TextTertiary,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Normal,
                )
                Text(
                    text       = "$batteryIcon $batteryPct%",
                    color      = SatriaColors.TextSecondary,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Normal,
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text       = message,
                color      = SatriaColors.TextTertiary,
                fontSize   = 12.sp,
                lineHeight  = 18.sp,
            )
        }
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
