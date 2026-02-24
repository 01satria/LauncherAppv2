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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.data.CountdownItem
import id.satria.launcher.ui.theme.LocalAppTheme
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
    onHabits       : () -> Unit,
    onPrayer       : () -> Unit,
    onMoneyManager : () -> Unit,
) {
    val daily = listOf(
        ToolEntry("🌤️", "Weather",    null,                                    onWeather),
        ToolEntry("🕌",  "Prayer",     "Daily salah times",                    onPrayer),
        ToolEntry("📝",  "To Do",      todoPending?.let { "$it pending" },     onTodo),
        ToolEntry("💪",  "Habits",     if (habitTotal > 0) "$habitDone / $habitTotal done" else "Track your streak", onHabits),
        ToolEntry("💰",  "Money",      "Budget & expense tracker",   onMoneyManager),
    )
    val tools = listOf(
        ToolEntry("💱",  "Currency",   "Live exchange rates",                  onMoney),
        ToolEntry("⏳",  "Countdown",  cdFirst?.let { cdPreview(it) },         onCountdown),
        ToolEntry("🍅",  "Pomodoro",   "Focus timer",                          onPomodoro),
        ToolEntry("🧮",  "Calculator", null,                                   onCalculator),
        ToolEntry("📐",  "Converter",  "Length · Weight · Temp · Speed",       onConverter),
    )

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        GSection("DAILY", daily)
        GSection("TOOLS",  tools)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun GSection(label: String, tools: List<ToolEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(start = 4.dp, top = 0.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = SatriaColors.TextTertiary, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Box(Modifier.weight(1f).height(0.5.dp).padding(start = 10.dp).background(SatriaColors.Divider))
        }
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SatriaColors.CardBg)) {
            tools.forEachIndexed { i, tool ->
                GRow(tool)
                if (i < tools.lastIndex)
                    Box(Modifier.fillMaxWidth().height(0.5.dp).padding(start = 56.dp).background(SatriaColors.Divider))
            }
        }
    }
}

@Composable
private fun GRow(tool: ToolEntry) {
    val darkMode = LocalAppTheme.current.darkMode
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = tool.onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(if (darkMode) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)),
            contentAlignment = Alignment.Center,
        ) { Text(tool.icon, fontSize = 18.sp) }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(tool.label, color = SatriaColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (!tool.badge.isNullOrEmpty())
                Text(tool.badge, color = SatriaColors.TextTertiary, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Text("›", color = SatriaColors.TextTertiary, fontSize = 18.sp)
    }
}

private fun cdPreview(item: CountdownItem): String = try {
    val days = TimeUnit.MILLISECONDS.toDays(
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .parse(item.targetDate.take(10))!!.time - System.currentTimeMillis()
    )
    when { days > 0L -> "${item.name} · ${days}d left"; days == 0L -> "${item.name} · Today"; else -> "${item.name} · ${-days}d ago" }
} catch (_: Exception) { item.name }
