package id.satria.launcher.ui.component

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
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
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@Composable
fun DashboardHeader(
        avatarPath: String?,
        assistantName: String,
        userName: String,
        onAvatarClick: () -> Unit,
) {
    val context = LocalContext.current
    val darkMode = id.satria.launcher.ui.theme.LocalAppTheme.current.darkMode

    var clockStr by remember { mutableStateOf(fmt("HH:mm")) }
    var dateStr by remember { mutableStateOf(fmt("EEEE, d MMMM")) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            clockStr = fmt("HH:mm")
            dateStr = fmt("EEEE, d MMMM")
        }
    }

    var message by remember(userName) { mutableStateOf(getAssistantMessage(userName)) }
    LaunchedEffect(userName) {
        while (true) {
            delay(300_000)
            message = getAssistantMessage(userName)
        }
    }

    // Battery — event-driven, zero polling
    var batteryPct by remember { mutableIntStateOf(getBatLevel(context)) }
    var isCharging by remember { mutableStateOf(getBatCharging(context)) }
    DisposableEffect(Unit) {
        val r =
                object : BroadcastReceiver() {
                    override fun onReceive(c: Context, i: Intent) {
                        val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        if (l >= 0 && s > 0) batteryPct = l * 100 / s
                        val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        isCharging =
                                st == BatteryManager.BATTERY_STATUS_CHARGING ||
                                        st == BatteryManager.BATTERY_STATUS_FULL
                    }
                }
        context.registerReceiver(r, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(r) }
    }
    val batText = if (isCharging) "$batteryPct% · charging" else "$batteryPct%"

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Avatar — full width square, no fractional screen dependency ───
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { onAvatarClick() }) {
            if (avatarPath != null) {
                AsyncImage(
                        model =
                                ImageRequest.Builder(context)
                                        .data(avatarPath)
                                        .crossfade(true)
                                        .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                        modifier = Modifier.fillMaxSize().background(SatriaColors.SurfaceMid),
                        contentAlignment = Alignment.Center
                ) { Text("👤", fontSize = 72.sp) }
            }

            // Fade — 12% dari bawah, smooth 3-stop gradient
            Box(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .fillMaxHeight(0.12f)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                            Brush.verticalGradient(
                                                    0f to Color.Transparent,
                                                    0.5f to
                                                            SatriaColors.ScreenBackground.copy(
                                                                    alpha = 0.5f
                                                            ),
                                                    1f to SatriaColors.ScreenBackground,
                                            )
                                    )
            )
        }

        // ── Nama assistant · Jam · Tanggal · Message ──────────────────────
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 12.dp, bottom = 16.dp),
        ) {
            Text(
                    assistantName,
                    color = SatriaColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                    clockStr,
                    color = SatriaColors.TextPrimary,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = 2.sp,
            )
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(dateStr, color = SatriaColors.TextSecondary, fontSize = 14.sp)
                Text("·", color = SatriaColors.TextTertiary, fontSize = 14.sp)
                Text(batText, color = SatriaColors.TextSecondary, fontSize = 14.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                    message,
                    color = SatriaColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
            )
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
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
}
