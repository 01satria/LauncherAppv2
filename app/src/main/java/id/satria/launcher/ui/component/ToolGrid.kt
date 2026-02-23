package id.satria.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.data.CountdownItem
import id.satria.launcher.ui.theme.SatriaColors
import java.util.concurrent.TimeUnit

private data class ToolEntry(val icon: String, val label: String, val badge: String?, val onClick: () -> Unit)

@Composable
fun ToolGrid(
    todoPending  : Int?,
    cdFirst      : CountdownItem?,
    habitDone    : Int,
    habitTotal   : Int,
    onWeather    : () -> Unit,
    onMoney      : () -> Unit,
    onTodo       : () -> Unit,
    onCountdown  : () -> Unit,
    onPomodoro   : () -> Unit,
    onCalculator : () -> Unit,
    onConverter  : () -> Unit,
    onHabits     : () -> Unit,
    onPrayer     : () -> Unit,
) {
    val group1 = listOf(
        ToolEntry("🌤️", "Weather",   null,                                   onWeather),
        ToolEntry("💱",  "Currency",  null,                                   onMoney),
        ToolEntry("📝",  "To Do",     todoPending?.let { "$it pending" },     onTodo),
        ToolEntry("⏳",  "Countdown", cdFirst?.let { cdPreview(it) },         onCountdown),
        ToolEntry("🍅",  "Pomodoro",  "Focus timer",                          onPomodoro),
    )
    val group2 = listOf(
        ToolEntry("🕌",  "Prayer",    "Waktu Sholat",                         onPrayer),
        ToolEntry("🧮",  "Calculator","",                                     onCalculator),
        ToolEntry("📐",  "Converter", "Length · Weight · Temp…",              onConverter),
        ToolEntry("💪",  "Habits",    if (habitTotal > 0) "$habitDone/$habitTotal today" else null, onHabits),
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ToolGroup(label = "ESSENTIALS", tools = group1)
        ToolGroup(label = "UTILITIES",  tools = group2)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ToolGroup(label: String, tools: List<ToolEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(label, color = SatriaColors.TextTertiary, fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp))
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SatriaColors.CardBg)) {
            tools.forEachIndexed { i, tool ->
                ToolRow(tool)
                if (i < tools.lastIndex)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(start = 52.dp).background(SatriaColors.Divider))
            }
        }
    }
}

@Composable
private fun ToolRow(tool: ToolEntry) {
    Row(modifier = Modifier.fillMaxWidth()
        .clickable(onClick = tool.onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(tool.icon, fontSize = 22.sp, modifier = Modifier.width(28.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tool.label, color = SatriaColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (!tool.badge.isNullOrEmpty())
                Text(tool.badge, color = SatriaColors.TextTertiary, fontSize = 12.sp)
        }
        Text("›", color = SatriaColors.TextTertiary, fontSize = 20.sp)
    }
}

private fun cdPreview(item: CountdownItem): String {
    return try {
        val days = TimeUnit.MILLISECONDS.toDays(
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .parse(item.targetDate.take(10))!!.time - System.currentTimeMillis())
        when { days > 0L -> "${item.name} · ${days}d"; days == 0L -> "${item.name} · Today"; else -> "${item.name} · ${-days}d ago" }
    } catch (_: Exception) { item.name }
}
