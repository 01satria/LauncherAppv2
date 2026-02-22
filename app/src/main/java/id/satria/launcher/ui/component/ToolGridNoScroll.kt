package id.satria.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.data.CountdownItem
import id.satria.launcher.ui.theme.SatriaColors
import java.util.concurrent.TimeUnit

private val DIVIDER @Composable get() = SatriaColors.Border
private val CARD_BG = Color(0xFF0D0D0D)

private data class TE(val icon: String, val label: String, val badge: String?, val onClick: () -> Unit)

// Identik dengan ToolGrid tapi TANPA verticalScroll — dipakai saat parent sudah scroll
@Composable
fun ToolGridNoScroll(
    todoPending  : Int?,
    cdFirst      : CountdownItem?,
    noteCount    : Int,
    habitDone    : Int,
    habitTotal   : Int,
    onWeather    : () -> Unit,
    onMoney      : () -> Unit,
    onTodo       : () -> Unit,
    onCountdown  : () -> Unit,
    onPomodoro   : () -> Unit,
    onCalculator : () -> Unit,
    onStopwatch  : () -> Unit,
    onNotes      : () -> Unit,
    onConverter  : () -> Unit,
    onHabits     : () -> Unit,
) {
    val g1 = listOf(
        TE("🌤️", "Weather",   null,                                   onWeather),
        TE("💱",  "Exchange",  null,                                   onMoney),
        TE("📝",  "To Do",     todoPending?.let { "$it pending" },     onTodo),
        TE("⏳",  "Countdown", cdFirst?.let { cdPrev(it) },           onCountdown),
        TE("🍅",  "Pomodoro",  "Focus timer",                         onPomodoro),
    )
    val g2 = listOf(
        TE("🧮",  "Calculator","",                                    onCalculator),
        TE("⏱️",  "Stopwatch", "& Timer",                            onStopwatch),
        TE("🗒️",  "Notes",     if (noteCount > 0) "$noteCount notes" else null, onNotes),
        TE("📐",  "Converter", "Length · Weight · Temp…",             onConverter),
        TE("💪",  "Habits",    if (habitTotal > 0) "$habitDone/$habitTotal today" else null, onHabits),
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TGroup("ESSENTIALS", g1)
        TGroup("UTILITIES",  g2)
    }
}

@Composable
private fun TGroup(label: String, tools: List<TE>) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(label, color = SatriaColors.TextTertiary, fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 8.dp))
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CARD_BG)) {
            tools.forEachIndexed { i, t ->
                TRow(t)
                if (i < tools.lastIndex) Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(start = 52.dp).background(DIVIDER))
            }
        }
    }
}

@Composable
private fun TRow(t: TE) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = t.onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(t.icon, fontSize = 22.sp, modifier = Modifier.width(28.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(t.label, color = SatriaColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (!t.badge.isNullOrEmpty()) Text(t.badge, color = SatriaColors.TextTertiary, fontSize = 12.sp)
        }
        Text("›", color = SatriaColors.TextTertiary, fontSize = 20.sp)
    }
}

private fun cdPrev(item: CountdownItem): String = try {
    val days = TimeUnit.MILLISECONDS.toDays(
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .parse(item.targetDate.take(10))!!.time - System.currentTimeMillis())
    when { days > 0L -> "${item.name} · ${days}d"; days == 0L -> "${item.name} · Today"; else -> "${item.name} · ${-days}d ago" }
} catch (_: Exception) { item.name }
