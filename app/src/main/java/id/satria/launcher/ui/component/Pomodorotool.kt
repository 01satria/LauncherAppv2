package id.satria.launcher.ui.component

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

// ── Pomodoro fixed dark palette — always night mode ─────────────────────────
private val POM_BG = androidx.compose.ui.graphics.Color.Black
private val POM_SURFACE  = androidx.compose.ui.graphics.Color(0xFF1C1C1E)
private val POM_ACCENT   = androidx.compose.ui.graphics.Color(0xFF27AE60)
private val POM_DIM40    = androidx.compose.ui.graphics.Color(0x66FFFFFF)
private val POM_DIM12    = androidx.compose.ui.graphics.Color(0x1FFFFFFF)
private val POM_DIM08    = androidx.compose.ui.graphics.Color(0x14FFFFFF)
private val POM_WHITE    = androidx.compose.ui.graphics.Color.White
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar

// ── Screen keep-alive helper ──────────────────────────────────────────────────
private fun Activity.keepScreenOn(on: Boolean) {
    if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    else    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

// ── Color constants — allocated once, never recreated ────────────────────────

private val DangerRed = Color(0xFFFF453A)

@Composable
fun PomodoroScreen(onExit: () -> Unit) {
    val context  = LocalContext.current
    val activity = context as? Activity

    var phase          by remember { mutableStateOf(PomodoroPhase.SETUP) }
    var showExitDialog by remember { mutableStateOf(false) }
    var selectedHour   by remember { mutableIntStateOf(0) }
    var selectedMin    by remember { mutableIntStateOf(25) }
    var totalSeconds   by remember { mutableIntStateOf(0) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var currentTime    by remember { mutableStateOf(clockStr()) }

    // Keep screen on only while RUNNING
    DisposableEffect(phase) {
        if (phase == PomodoroPhase.RUNNING) activity?.keepScreenOn(true)
        onDispose { activity?.keepScreenOn(false) }
    }

    // One coroutine: clock tick + elapsed counter
    LaunchedEffect(phase) {
        while (isActive) {
            delay(1_000L)
            currentTime = clockStr()
            if (phase == PomodoroPhase.RUNNING && elapsedSeconds < totalSeconds) {
                elapsedSeconds++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(POM_BG),
    ) {
        when (phase) {
            PomodoroPhase.SETUP -> SetupPanel(
                hour     = selectedHour,
                minute   = selectedMin,
                onHour   = { selectedHour = it },
                onMinute = { selectedMin = it },
                onStart  = {
                    totalSeconds   = selectedHour * 3600 + selectedMin * 60
                    elapsedSeconds = 0
                    phase          = PomodoroPhase.RUNNING
                },
                onCancel = onExit,
            )
            PomodoroPhase.RUNNING -> ClockPanel(
                currentTime    = currentTime,
                totalSeconds   = totalSeconds,
                elapsedSeconds = elapsedSeconds,
                onExitRequest  = { showExitDialog = true },
            )
        }

        if (showExitDialog) {
            ExitConfirmOverlay(
                onConfirm = { activity?.keepScreenOn(false); onExit() },
                onDismiss = { showExitDialog = false },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SETUP PANEL
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SetupPanel(
    hour: Int, minute: Int,
    onHour: (Int) -> Unit, onMinute: (Int) -> Unit,
    onStart: () -> Unit, onCancel: () -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SetupTitle()
                TimePickerRow(hour, minute, onHour, onMinute)
            }
            Column(
                modifier = Modifier.weight(0.65f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StartButton(enabled = hour > 0 || minute > 0, onClick = onStart)
                CancelButton(onClick = onCancel)
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SetupTitle()
            Spacer(Modifier.height(40.dp))
            TimePickerRow(hour, minute, onHour, onMinute)
            Spacer(Modifier.height(52.dp))
            StartButton(enabled = hour > 0 || minute > 0, onClick = onStart)
            Spacer(Modifier.height(10.dp))
            CancelButton(onClick = onCancel)
        }
    }
}

@Composable
private fun SetupTitle() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🍅  Pomodoro", color = POM_WHITE, fontSize = 22.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        Text("Set focus duration", color = POM_DIM40, fontSize = 13.sp)
    }
}

@Composable
private fun TimePickerRow(
    hour: Int, minute: Int,
    onHour: (Int) -> Unit, onMinute: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PickerCard(label = "HRS", value = hour,
            onUp = { if (hour < 5)  onHour(hour + 1) },
            onDown = { if (hour > 0) onHour(hour - 1) })
        Spacer(Modifier.width(8.dp))
        PickerCard(label = "MIN", value = minute,
            onUp = { if (minute < 59) onMinute(minute + 1) },
            onDown = { if (minute > 0)  onMinute(minute - 1) })
    }
}

// ── Card-style picker with bigger buttons ────────────────────────────────────
@Composable
private fun PickerCard(label: String, value: Int, onUp: () -> Unit, onDown: () -> Unit) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(POM_SURFACE)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = POM_DIM40, fontSize = 11.sp, letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))

        // UP — large touch target (54dp height)
        Box(
            modifier = Modifier.fillMaxWidth().height(54.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onUp),
            contentAlignment = Alignment.Center,
        ) {
            Text("▲", color = POM_ACCENT, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Value
        Text("%02d".format(value), color = POM_WHITE, fontSize = 54.sp,
            fontWeight = FontWeight.Thin, letterSpacing = 2.sp)

        // DOWN — large touch target (54dp height)
        Box(
            modifier = Modifier.fillMaxWidth().height(54.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onDown),
            contentAlignment = Alignment.Center,
        ) {
            Text("▼", color = POM_ACCENT, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StartButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = POM_ACCENT,
            disabledContainerColor = POM_DIM08,
        ),
        shape = RoundedCornerShape(16.dp),
    ) { Text("Start Focus", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun CancelButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text("Cancel", color = POM_DIM40, fontSize = 14.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CLOCK PANEL — extreme RAM savings:
//   No LazyList · No Image · No Coil · No Canvas · No Animation objects
//   Only Text + LinearProgressIndicator + Box
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ClockPanel(
    currentTime: String,
    totalSeconds: Int,
    elapsedSeconds: Int,
    onExitRequest: () -> Unit,
) {
    val remaining = (totalSeconds - elapsedSeconds).coerceAtLeast(0)
    val progress  = if (totalSeconds > 0) elapsedSeconds.toFloat() / totalSeconds else 0f
    val isDone    = remaining == 0

    val remH   = remaining / 3600
    val remM   = (remaining % 3600) / 60
    val remS   = remaining % 60
    val remStr = if (remH > 0) "%d:%02d:%02d".format(remH, remM, remS)
                 else          "%02d:%02d".format(remM, remS)

    val isLandscape = LocalConfiguration.current.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {

        // Exit button — top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(POM_DIM08)
                .clickable(interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onExitRequest),
            contentAlignment = Alignment.Center,
        ) { Text("✕", color = POM_DIM40, fontSize = 14.sp) }

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isDone) "Done! 🎉" else "Focus",
                        color = POM_DIM40, fontSize = 12.sp, letterSpacing = 3.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(currentTime, color = POM_WHITE, fontSize = 64.sp,
                        fontWeight = FontWeight.Thin, letterSpacing = 4.sp, textAlign = TextAlign.Center)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text(remStr, color = if (isDone) POM_ACCENT else POM_DIM40,
                        fontSize = 28.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
                    Spacer(Modifier.height(20.dp))
                    LinearProgressIndicator(
                        progress   = { progress },
                        modifier   = Modifier.fillMaxWidth().height(1.dp),
                        color = POM_ACCENT,
                        trackColor = POM_DIM08,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(if (isDone) "Done! 🎉" else "Focus",
                    color = POM_DIM40, fontSize = 12.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(20.dp))
                Text(currentTime, color = Color.White, fontSize = 72.sp,
                    fontWeight = FontWeight.Thin, letterSpacing = 4.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(28.dp))
                Text(remStr, color = if (isDone) POM_ACCENT else POM_DIM40,
                    fontSize = 24.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
                Spacer(Modifier.height(28.dp))
                LinearProgressIndicator(
                    progress   = { progress },
                    modifier   = Modifier.fillMaxWidth().height(1.dp),
                    color = POM_ACCENT,
                    trackColor = POM_DIM08,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EXIT CONFIRM — minimal, no animations
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ExitConfirmOverlay(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xE8000000))
            .clickable(interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(POM_SURFACE)
                .padding(28.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = {}),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("End focus session?", color = POM_WHITE, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Text(
                "Screen timeout will be restored\nto its original setting.",
                color = POM_DIM40, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = POM_DIM40),
                    border = androidx.compose.foundation.BorderStroke(1.dp, POM_DIM12),
                ) { Text("Stay") }
                Button(
                    onClick = onConfirm, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                ) { Text("Exit", color = Color.White, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
private enum class PomodoroPhase { SETUP, RUNNING }
private fun clockStr(): String {
    val c = Calendar.getInstance()
    return "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
}
