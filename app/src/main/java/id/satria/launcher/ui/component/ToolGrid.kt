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
import id.satria.launcher.ui.theme.SatriaColors
import java.util.concurrent.TimeUnit

private val DividerColor = Color(0xFF1A1A1A)
private val CardBg       = Color(0xFF0D0D0D)

private data class Tool(
    val icon    : String,
    val label   : String,
    val badge   : String?,
    val onClick : () -> Unit,
)

@Composable
fun ToolGrid(
    todoPending : Int?,
    cdFirst     : CountdownItem?,
    onWeather   : () -> Unit,
    onMoney     : () -> Unit,
    onTodo      : () -> Unit,
    onCountdown : () -> Unit,
    onPomodoro  : () -> Unit,
) {
    val tools = listOf(
        Tool("🌤️", "Weather",       null,                                       onWeather),
        Tool("💱", "Exchange",      null,                                       onMoney),
        Tool("📝", "To Do",         todoPending?.let { "$it pending" },         onTodo),
        Tool("⏳", "Countdown",     cdFirst?.let { cdPreview(it) },             onCountdown),
        Tool("🍅", "Pomodoro",      "Focus mode",                               onPomodoro),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Label
        Text(
            "TOOLS",
            color         = SatriaColors.TextTertiary,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            modifier      = Modifier.padding(bottom = 12.dp),
        )

        // Single grouped card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg),
        ) {
            tools.forEachIndexed { index, tool ->
                ToolRow(tool = tool)
                if (index < tools.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(start = 52.dp) // align divider with text, skip icon area
                            .background(DividerColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolRow(tool: Tool) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = tool.onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Icon
        Text(tool.icon, fontSize = 22.sp, modifier = Modifier.width(28.dp))

        // Label + badge
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                tool.label,
                color      = SatriaColors.TextPrimary,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            if (tool.badge != null) {
                Text(
                    tool.badge,
                    color    = SatriaColors.TextTertiary,
                    fontSize = 12.sp,
                )
            }
        }

        // Chevron
        Text(
            "›",
            color    = SatriaColors.TextTertiary,
            fontSize = 20.sp,
        )
    }
}

private fun cdPreview(item: CountdownItem): String {
    return try {
        val now    = System.currentTimeMillis()
        val target = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .parse(item.targetDate.take(10))?.time ?: return item.name
        val days   = TimeUnit.MILLISECONDS.toDays(target - now)
        when {
            days > 0L  -> "${item.name} · ${days}d"
            days == 0L -> "${item.name} · Today"
            else       -> "${item.name} · ${-days}d ago"
        }
    } catch (e: Exception) { item.name }
}
