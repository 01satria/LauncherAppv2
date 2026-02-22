package id.satria.launcher.ui.component

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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

@Composable
fun DashboardHeader(
    avatarPath: String?,
    assistantName: String,
    userName: String,
    onAvatarClick: () -> Unit,
    onClose: () -> Unit,
) {
    val context         = LocalContext.current
    val screenHeightDp  = LocalConfiguration.current.screenHeightDp.dp
    val avatarHeightDp  = screenHeightDp / 3

    // ── Clock & Date ───────────────────────────────────────────────────────
    var clockStr by remember { mutableStateOf(getClockStr()) }
    var dateStr  by remember { mutableStateOf(getDateStr()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); clockStr = getClockStr(); dateStr = getDateStr() }
    }

    // ── Message ────────────────────────────────────────────────────────────
    var message by remember(userName) { mutableStateOf(getAssistantMessage(userName)) }
    LaunchedEffect(userName) {
        while (true) { delay(60_000); message = getAssistantMessage(userName) }
    }

    // ── Battery — zero polling via BroadcastReceiver ───────────────────────
    var batteryPct by remember { mutableIntStateOf(getBatteryLevel(context)) }
    var isCharging by remember { mutableStateOf(getBatteryCharging(context)) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val l = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val s = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (l >= 0 && s > 0) batteryPct = l * 100 / s
                val st = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = st == BatteryManager.BATTERY_STATUS_CHARGING ||
                             st == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    // battery text — hanya persen, tanpa simbol
    val batteryText = if (isCharging) "$batteryPct% ·  charging" else "$batteryPct%"

    // ── Scrollable column ──────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {

        // ── Avatar full-width, tinggi 1/3 layar ───────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(avatarHeightDp)
                .clickable { onAvatarClick() },
        ) {
            if (avatarPath != null) {
                AsyncImage(
                    model            = ImageRequest.Builder(context)
                        .data(avatarPath).crossfade(true).build(),
                    contentDescription = null,
                    contentScale     = ContentScale.Crop,
                    modifier         = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier         = Modifier.fillMaxSize()
                        .background(SatriaColors.Surface),
                    contentAlignment = Alignment.Center,
                ) { Text("👤", fontSize = 64.sp) }
            }

            // Fade gradient bawah berbatasan dengan jam
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, SatriaColors.ScreenBackground)
                        )
                    )
            )

            // Close button
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

            // Assistant name bawah avatar
            Text(
                text     = assistantName,
                color    = Color.White.copy(alpha = 0.80f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 12.dp),
            )
        }

        // ── Clock + date + battery ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 4.dp),
        ) {
            Text(
                text          = clockStr,
                color         = SatriaColors.TextPrimary,
                fontSize      = 56.sp,
                fontWeight    = FontWeight.Thin,
                letterSpacing = 2.sp,
            )
            // Date · battery% — font/warna sama persis
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(dateStr, color = SatriaColors.TextSecondary, fontSize = 14.sp)
                Text("·",    color = SatriaColors.TextTertiary,   fontSize = 14.sp)
                Text(batteryText, color = SatriaColors.TextSecondary, fontSize = 14.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(message, color = SatriaColors.TextTertiary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

private fun getClockStr() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
private fun getDateStr()  = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())

private fun getBatteryLevel(ctx: Context): Int {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val l = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val s = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    return if (l >= 0 && s > 0) l * 100 / s else 0
}
private fun getBatteryCharging(ctx: Context): Boolean {
    val i  = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
}
