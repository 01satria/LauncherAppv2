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

@Composable
fun ToolGrid(
    todoPending: Int?,
    cdFirst: CountdownItem?,
    onWeather: () -> Unit,
    onMoney: () -> Unit,
    onTodo: () -> Unit,
    onCountdown: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard(icon = "🌤️", label = "Weather",        preview = null,                      onClick = onWeather,   modifier = Modifier.weight(1f))
            ToolCard(icon = "💱", label = "Money Exchange", preview = null,                      onClick = onMoney,     modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard(icon = "📝", label = "To Do",      preview = todoPending?.let { "$it task${if (it > 1) "s" else ""} pending" }, onClick = onTodo,      modifier = Modifier.weight(1f))
            ToolCard(icon = "⏳", label = "Countdown",  preview = cdFirst?.let { cdPreview(it) },                                    onClick = onCountdown, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ToolCard(
    icon: String,
    label: String,
    preview: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SatriaColors.Surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
            .defaultMinSize(minHeight = 88.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.height(6.dp))
        Text(label, color = SatriaColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (preview != null) {
            Spacer(Modifier.height(4.dp))
            Text(preview, color = SatriaColors.TextTertiary, fontSize = 10.sp, lineHeight = 14.sp, maxLines = 2)
        }
    }
}

private fun cdPreview(item: CountdownItem): String {
    return try {
        val now    = System.currentTimeMillis()
        val target = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .parse(item.targetDate.take(10))?.time ?: return item.name
        val diff   = target - now
        val days   = TimeUnit.MILLISECONDS.toDays(diff)
        when {
            days > 0  -> "${item.name}: ${days}d left"
            days == 0L -> "${item.name}: Today!"
            else       -> "${item.name}: ${-days}d ago"
        }
    } catch (e: Exception) { item.name }
}
