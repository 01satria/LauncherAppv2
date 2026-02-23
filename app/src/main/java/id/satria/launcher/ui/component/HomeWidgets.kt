package id.satria.launcher.ui.component

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import id.satria.launcher.ui.theme.SatriaColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

const val WIDGET_CLOCK   = "widget_clock"
const val WIDGET_DATE    = "widget_date"
const val WIDGET_BATTERY = "widget_battery"

data class WidgetInfo(val type: String, val label: String, val icon: String)

val AVAILABLE_WIDGETS = listOf(
    WidgetInfo(WIDGET_CLOCK,   "Clock",   "🕐"),
    WidgetInfo(WIDGET_DATE,    "Date",    "📅"),
    WidgetInfo(WIDGET_BATTERY, "Battery", "🔋"),
)

@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
    var time by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    var date by remember { mutableStateOf(SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            date = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SatriaColors.WidgetBg, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column {
            Text(
                text       = time,
                color      = SatriaColors.TextPrimary,
                fontSize   = 52.sp,
                fontWeight = FontWeight.Thin,
                letterSpacing = 1.sp,
            )
            Text(
                text     = date,
                color    = SatriaColors.TextSecondary,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
fun DateWidget(modifier: Modifier = Modifier) {
    val now = remember { Date() }
    val dayName = remember { SimpleDateFormat("EEEE", Locale.getDefault()).format(now) }
    val dayNum  = remember { SimpleDateFormat("d",    Locale.getDefault()).format(now) }
    val month   = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SatriaColors.WidgetBg, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(SatriaColors.Accent, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(dayNum, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(dayName, color = SatriaColors.TextPrimary,   fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(month,   color = SatriaColors.TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun BatteryWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var pct       by remember { mutableIntStateOf(getHomeBatteryLevel(context)) }
    var charging  by remember { mutableStateOf(getHomeBatteryCharging(context)) }

    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val lv = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val sc = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (lv >= 0 && sc > 0) pct = lv * 100 / sc
                val st = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(r, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(r) }
    }

    val barColor = when {
        charging   -> SatriaColors.Accent
        pct <= 20  -> Color(0xFFFF453A)
        pct <= 50  -> Color(0xFFFFB74D)
        else       -> SatriaColors.Accent
    }
    val icon = if (charging) "⚡" else when { pct <= 20 -> "🪫"; else -> "🔋" }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SatriaColors.WidgetBg, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(icon, fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(if (charging) "Charging" else "Battery", color = SatriaColors.TextSecondary, fontSize = 12.sp)
                    Text("$pct%", color = barColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(SatriaColors.SurfaceHigh, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pct / 100f)
                            .height(6.dp)
                            .background(barColor, RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun SystemWidget(type: String, modifier: Modifier = Modifier) {
    when (type) {
        WIDGET_CLOCK   -> ClockWidget(modifier)
        WIDGET_DATE    -> DateWidget(modifier)
        WIDGET_BATTERY -> BatteryWidget(modifier)
        else           -> Unit
    }
}

private fun getHomeBatteryLevel(ctx: Context): Int {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val l = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val s = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    return if (l >= 0 && s > 0) l * 100 / s else 0
}
private fun getHomeBatteryCharging(ctx: Context): Boolean {
    val i  = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
}
