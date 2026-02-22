package id.satria.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.ui.theme.SatriaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private fun fmtMs(ms: Long): String {
    val h  = ms / 3_600_000L
    val m  = (ms % 3_600_000L) / 60_000L
    val s  = (ms % 60_000L) / 1_000L
    val cs = (ms % 1_000L) / 10L
    return if (h > 0) "%d:%02d:%02d.%02d".format(h, m, s, cs)
    else              "%02d:%02d.%02d".format(m, s, cs)
}

private val GREEN  = Color(0xFF30D158)
private val ORANGE = Color(0xFFFF9F0A)
private val RED    = Color(0xFFFF453A)

private enum class SwState  { IDLE, RUNNING, PAUSED }
private enum class TmrState { IDLE, RUNNING, PAUSED, DONE }

@Composable
fun StopwatchTool() {
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(SatriaColors.ScreenBackground)) {
        // Tab bar
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Stopwatch", "Timer").forEachIndexed { idx, label ->
                val active = tab == idx
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(50.dp))
                    .background(if (active) SatriaColors.Accent else SatriaColors.SurfaceMid)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { tab = idx }
                    .padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = if (active) Color.White else SatriaColors.TextSecondary,
                        fontSize = 14.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        if (tab == 0) StopwatchPanel() else TimerPanel()
    }
}

@Composable
private fun StopwatchPanel() {
    var state   by remember { mutableStateOf(SwState.IDLE) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var baseMs  by remember { mutableLongStateOf(0L) }
    var startAt by remember { mutableLongStateOf(0L) }
    val laps    = remember { mutableStateListOf<Long>() }
    val listState = rememberLazyListState()

    // Auto-scroll ke lap terbaru saat lap ditambah
    LaunchedEffect(laps.size) {
        if (laps.isNotEmpty()) listState.animateScrollToItem(0)
    }

    LaunchedEffect(state) {
        if (state == SwState.RUNNING) {
            startAt = System.currentTimeMillis()
            while (isActive && state == SwState.RUNNING) {
                elapsed = baseMs + (System.currentTimeMillis() - startAt)
                delay(16L)
            }
        }
    }

    // Layout: lap list (scrollable, weight) → timer display → buttons
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

        // ── Lap list — di atas, scrollable, mengambil sisa ruang ──────────
        if (laps.isNotEmpty()) {
            LazyColumn(
                state          = listState,
                reverseLayout  = true,
                modifier       = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
            ) {
                itemsIndexed(laps) { i, lapMs ->
                    val delta = if (i == 0) lapMs else lapMs - laps[i - 1]
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Lap ${i + 1}", color = SatriaColors.TextSecondary, fontSize = 14.sp)
                        Text(fmtMs(delta), color = SatriaColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    if (i < laps.lastIndex)
                        Box(Modifier.fillMaxWidth().height(1.dp).background(SatriaColors.Divider))
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        // ── Timer display ─────────────────────────────────────────────────
        Spacer(Modifier.height(20.dp))

        Text(
            fmtMs(elapsed),
            color = SatriaColors.TextPrimary,
            fontSize = 52.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
        )

        if (laps.isNotEmpty()) {
            val lapDelta = elapsed - laps.last()
            Text(
                "Lap  +${fmtMs(lapDelta)}",
                color = SatriaColors.TextTertiary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(Modifier.height(32.dp))

        // ── Buttons ───────────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            when (state) {
                SwState.IDLE -> {
                    RoundBtn("Start", GREEN) { state = SwState.RUNNING }
                }
                SwState.RUNNING -> {
                    RoundBtn("Lap", SatriaColors.SurfaceMid) { laps.add(elapsed) }
                    RoundBtn("Pause", ORANGE) { baseMs = elapsed; state = SwState.PAUSED }
                }
                SwState.PAUSED -> {
                    RoundBtn("Reset", RED) { state = SwState.IDLE; elapsed = 0L; baseMs = 0L; laps.clear() }
                    RoundBtn("Resume", GREEN) { state = SwState.RUNNING }
                }
            }
        }

        // Margin bawah setinggi back button (48dp tombol + 10dp padding atas + 10dp bawah + nav bar ~20dp)
        Spacer(Modifier.height(88.dp))
    }
}

@Composable
private fun TimerPanel() {
    var state     by remember { mutableStateOf(TmrState.IDLE) }
    var totalMs   by remember { mutableLongStateOf(0L) }
    var remaining by remember { mutableLongStateOf(0L) }
    var pickH     by remember { mutableIntStateOf(0) }
    var pickM     by remember { mutableIntStateOf(5) }
    var pickS     by remember { mutableIntStateOf(0) }

    LaunchedEffect(state) {
        if (state == TmrState.RUNNING) {
            while (isActive && state == TmrState.RUNNING && remaining > 0L) {
                delay(100L); remaining = (remaining - 100L).coerceAtLeast(0L)
                if (remaining == 0L) { state = TmrState.DONE }
            }
        }
    }

    val progress = if (totalMs > 0L) 1f - (remaining.toFloat() / totalMs) else 0f

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))

        if (state == TmrState.IDLE) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TimerPicker("H", pickH, 0, 23) { pickH = it }
                Text(":", color = SatriaColors.TextTertiary, fontSize = 36.sp, fontWeight = FontWeight.Thin)
                TimerPicker("M", pickM, 0, 59) { pickM = it }
                Text(":", color = SatriaColors.TextTertiary, fontSize = 36.sp, fontWeight = FontWeight.Thin)
                TimerPicker("S", pickS, 0, 59) { pickS = it }
            }
        } else {
            val done = state == TmrState.DONE
            Text(fmtMs(remaining), color = if (done) GREEN else SatriaColors.TextPrimary,
                fontSize = 52.sp, fontWeight = FontWeight.Thin, letterSpacing = 2.sp)
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(SatriaColors.Divider)) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(if (done) GREEN else SatriaColors.Accent))
            }
            if (done) {
                Spacer(Modifier.height(8.dp))
                Text("✓ Done!", color = GREEN, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(40.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 32.dp)) {
            val canStart = pickH > 0 || pickM > 0 || pickS > 0
            when (state) {
                TmrState.IDLE -> {
                    RoundBtn("Start", if (canStart) GREEN else SatriaColors.SurfaceMid) {
                        if (!canStart) return@RoundBtn
                        totalMs = (pickH * 3600 + pickM * 60 + pickS) * 1000L
                        remaining = totalMs; state = TmrState.RUNNING
                    }
                }
                TmrState.RUNNING -> RoundBtn("Pause", ORANGE) { state = TmrState.PAUSED }
                TmrState.PAUSED  -> {
                    RoundBtn("Reset", RED) { state = TmrState.IDLE; remaining = totalMs }
                    RoundBtn("Resume", GREEN) { state = TmrState.RUNNING }
                }
                TmrState.DONE -> RoundBtn("Reset", SatriaColors.SurfaceMid) {
                    state = TmrState.IDLE; pickH = 0; pickM = 5; pickS = 0
                    totalMs = 5 * 60 * 1000L; remaining = 0L
                }
            }
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun TimerPicker(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = SatriaColors.TextTertiary, fontSize = 10.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(SatriaColors.SurfaceMid)
            .clickable(interactionSource = remember{MutableInteractionSource()}, indication = null) { if (value < max) onChange(value + 1) },
            contentAlignment = Alignment.Center) { Text("▲", color = SatriaColors.Accent, fontSize = 11.sp) }
        Spacer(Modifier.height(6.dp))
        Text("%02d".format(value), color = SatriaColors.TextPrimary, fontSize = 42.sp, fontWeight = FontWeight.Thin)
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(SatriaColors.SurfaceMid)
            .clickable(interactionSource = remember{MutableInteractionSource()}, indication = null) { if (value > min) onChange(value - 1) },
            contentAlignment = Alignment.Center) { Text("▼", color = SatriaColors.Accent, fontSize = 11.sp) }
    }
}

@Composable
private fun RoundBtn(label: String, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.height(52.dp).widthIn(min = 110.dp)
        .clip(RoundedCornerShape(50.dp)).background(color)
        .clickable(interactionSource = remember{MutableInteractionSource()}, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 22.dp))
    }
}
