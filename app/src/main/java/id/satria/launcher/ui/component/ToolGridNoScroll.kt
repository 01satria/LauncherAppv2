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
import id.satria.launcher.ui.theme.LocalAppTheme
import id.satria.launcher.ui.theme.SatriaColors
import java.util.concurrent.TimeUnit

private data class TE(
    val icon    : String,
    val label   : String,
    val badge   : String?,
    val onClick : () -> Unit,
)

@Composable
fun ToolGridNoScroll(
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
    // ── DAILY — hal-hal yang dicek tiap hari
    val daily = listOf(
        TE("🌤️", "Weather",    null,                                    onWeather),
        TE("🕌",  "Prayer",     "Daily salah times",                    onPrayer),
        TE("📝",  "To Do",      todoPending?.let { "$it pending" },     onTodo),
        TE("💪",  "Habits",     if (habitTotal > 0) "$habitDone / $habitTotal done" else "Track your streak", onHabits),
    )
    // ── TOOLS — alat bantu & utilitas
    val tools = listOf(
        TE("💱",  "Currency",   "Live exchange rates",                  onMoney),
        TE("⏳",  "Countdown",  cdFirst?.let { cdPrev(it) },            onCountdown),
        TE("🍅",  "Pomodoro",   "Focus timer",                          onPomodoro),
        TE("🧮",  "Calculator", null,                                   onCalculator),
        TE("📐",  "Converter",  "Length · Weight · Temp · Speed",       onConverter),
    )

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TSection(label = "DAILY", items = daily)
        TSection(label = "TOOLS", items = tools)
    }
}

// ── Section with label + card ─────────────────────────────────────────────────
@Composable
private fun TSection(label: String, items: List<TE>) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        // Section label
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 0.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                color      = SatriaColors.TextTertiary,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
            )
            // Thin decorative line
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(0.5.dp)
                    .padding(start = 10.dp)
                    .background(SatriaColors.Divider),
            )
        }

        // Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SatriaColors.CardBg),
        ) {
            items.forEachIndexed { i, item ->
                TRow(item)
                if (i < items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .padding(start = 56.dp)
                            .background(SatriaColors.Divider),
                    )
                }
            }
        }
    }
}

// ── Row ───────────────────────────────────────────────────────────────────────
@Composable
private fun TRow(t: TE) {
    val darkMode = LocalAppTheme.current.darkMode

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = t.onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (darkMode) Color(0xFF2C2C2E)
                    else Color(0xFFF2F2F7)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(t.icon, fontSize = 18.sp)
        }

        // Label + badge
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(t.label, color = SatriaColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (!t.badge.isNullOrEmpty())
                Text(t.badge, color = SatriaColors.TextTertiary, fontSize = 11.sp, lineHeight = 14.sp)
        }

        // Chevron
        Text(
            "›",
            color    = SatriaColors.TextTertiary,
            fontSize = 18.sp,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun cdPrev(item: CountdownItem): String = try {
    val days = TimeUnit.MILLISECONDS.toDays(
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .parse(item.targetDate.take(10))!!.time - System.currentTimeMillis()
    )
    when {
        days > 0L  -> "${item.name} · ${days}d left"
        days == 0L -> "${item.name} · Today"
        else       -> "${item.name} · ${-days}d ago"
    }
} catch (_: Exception) { item.name }
