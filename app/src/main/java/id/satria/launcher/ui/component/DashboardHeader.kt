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
    avatarPath    : String?,
    assistantName : String,
    userName      : String,
    onAvatarClick : () -> Unit,
    onClose       : () -> Unit,
) {
    val context        = LocalContext.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val avatarHeightDp = screenHeightDp / 3

    var clockStr by remember { mutableStateOf(fmt("HH:mm")) }
    var dateStr  by remember { mutableStateOf(fmt("EEEE, d MMMM")) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); clockStr = fmt("HH:mm"); dateStr = fmt("EEEE, d MMMM") }
    }

    var message by remember(userName) { mutableStateOf(getAssistantMessage(userName)) }
    LaunchedEffect(userName) { while (true) { delay(60_000); message = getAssistantMessage(userName) } }

    // Battery — zero polling
    var batteryPct by remember { mutableIntStateOf(getBatLevel(context)) }
    var isCharging by remember { mutableStateOf(getBatCharging(context)) }
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (l >= 0 && s > 0) batteryPct = l * 100 / s
                val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(r, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(r) }
    }
    val batText = if (isCharging) "$batteryPct% · charging" else "$batteryPct%"

    // ── Layout: clock & message first, then avatar below ──────────────────
    Column(modifier = Modifier.fillMaxWidth()) {

        // Close button row
        Box(modifier = Modifier.fillMaxWidth().padding(end = 16.dp, top = 12.dp)) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).size(30.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f)).clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) { Text("✕", color = SatriaColors.TextSecondary, fontSize = 12.sp) }
        }

        // Clock + date + battery + message (ABOVE avatar, no fade overlay)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 16.dp),
        ) {
            Text(clockStr, color = SatriaColors.TextPrimary, fontSize = 56.sp,
                fontWeight = FontWeight.Thin, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(dateStr, color = SatriaColors.TextSecondary, fontSize = 14.sp)
                Text("·",    color = SatriaColors.TextTertiary,   fontSize = 14.sp)
                Text(batText, color = SatriaColors.TextSecondary, fontSize = 14.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(message, color = SatriaColors.TextTertiary, fontSize = 12.sp, lineHeight = 18.sp)
        }

        // Avatar — full width, 1/3 layar, fade hanya di dalam kotak avatar
        Box(modifier = Modifier.fillMaxWidth().height(avatarHeightDp).clickable { onAvatarClick() }) {
            if (avatarPath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(avatarPath).crossfade(true).build(),
                    contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(SatriaColors.SurfaceMid),
                    contentAlignment = Alignment.Center) { Text("👤", fontSize = 56.sp) }
            }
            // Fade hanya di DALAM kotak avatar (atas → transparan, bawah → bg)
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.45f).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, SatriaColors.ScreenBackground))))

            // Assistant name overlay dalam avatar
            Text(assistantName, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 10.dp))
        }
    }
}

private fun fmt(pattern: String) = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())

private fun getBatLevel(ctx: Context): Int {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val l = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val s = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    return if (l >= 0 && s > 0) l * 100 / s else 0
}
private fun getBatCharging(ctx: Context): Boolean {
    val i  = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
}
