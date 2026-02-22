package id.satria.launcher.ui.component

import android.app.Activity
import android.content.Context
import android.os.PowerManager
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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

@Composable
fun PomodoroScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    // ── State machine: SETUP → RUNNING → (confirm exit) ──────────────────
    var phase by remember { mutableStateOf(PomodoroPhase.SETUP) }
    var showExitConfirm by remember { mutableStateOf(false) }

    // Selector values
    var selectedHour by remember { mutableIntStateOf(0) }
    var selectedMin  by remember { mutableIntStateOf(25) }

    // Timer state (in seconds)
    var totalSeconds   by remember { mutableIntStateOf(0) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    // Current time for clock display
    var currentTime by remember { mutableStateOf(currentTimeString()) }

    // Keep screen on saat RUNNING
    DisposableEffect(phase) {
        if (phase == PomodoroPhase.RUNNING) activity?.keepScreenOn(true)
        onDispose { activity?.keepScreenOn(false) }
    }

    // Clock tick — hanya update setiap detik, ringan
    LaunchedEffect(phase) {
        while (isActive) {
            currentTime = currentTimeString()
            if (phase == PomodoroPhase.RUNNING) elapsedSeconds++
            delay(1000L)
        }
    }

    // Fullscreen hitam — tidak ada animasi berat
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (phase) {

            // ── SETUP: pilih jam & menit ──────────────────────────────────
            PomodoroPhase.SETUP -> {
                SetupPanel(
                    hour      = selectedHour,
                    minute    = selectedMin,
                    onHour    = { selectedHour = it },
                    onMinute  = { selectedMin = it },
                    onStart   = {
                        totalSeconds   = selectedHour * 3600 + selectedMin * 60
                        elapsedSeconds = 0
                        phase          = PomodoroPhase.RUNNING
                    },
                    onCancel  = onExit,
                )
            }

            // ── RUNNING: jam digital + progress ──────────────────────────
            PomodoroPhase.RUNNING -> {
                ClockPanel(
                    currentTime    = currentTime,
                    totalSeconds   = totalSeconds,
                    elapsedSeconds = elapsedSeconds,
                    onExitRequest  = { showExitConfirm = true },
                )
            }
        }

        // ── Exit confirm dialog ───────────────────────────────────────────
        if (showExitConfirm) {
            ExitConfirmOverlay(
                onConfirm = {
                    activity?.keepScreenOn(false)
                    onExit()
                },
                onDismiss = { showExitConfirm = false },
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "🍅  Pomodoro",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Set focus duration",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(48.dp))

        // ── Jam & Menit picker ─────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PickerColumn(
                label  = "HRS",
                value  = hour,
                range  = 0..5,
                onUp   = { if (hour < 5) onHour(hour + 1) },
                onDown = { if (hour > 0) onHour(hour - 1) },
            )

            Text(":", color = Color.White.copy(0.3f), fontSize = 40.sp, fontWeight = FontWeight.Thin)

            PickerColumn(
                label  = "MIN",
                value  = minute,
                range  = 0..59,
                onUp   = { if (minute < 59) onMinute(minute + 1) },
                onDown = { if (minute > 0)  onMinute(minute - 1) },
            )
        }

        Spacer(Modifier.height(56.dp))

        // Start button
        Button(
            onClick  = onStart,
            enabled  = hour > 0 || minute > 0,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = Color(0xFF27AE60),
                disabledContainerColor = Color.White.copy(alpha = 0.08f),
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                "Start Focus",
                color      = Color.White,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(14.dp))

        TextButton(onClick = onCancel) {
            Text("Cancel", color = Color.White.copy(alpha = 0.35f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun PickerColumn(
    label: String,
    value: Int,
    range: IntRange,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp, letterSpacing = 2.sp)

        // Up arrow
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onUp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("▲", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        }

        Text(
            "%02d".format(value),
            color      = Color.White,
            fontSize   = 52.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = 2.sp,
        )

        // Down arrow
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDown,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("▼", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CLOCK PANEL — hemat RAM ekstrem: tidak ada list, tidak ada image, tidak ada
// komponen berat. Hanya Text + LinearProgressIndicator + Box.
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

    // Format remaining time
    val remH  = remaining / 3600
    val remM  = (remaining % 3600) / 60
    val remS  = remaining % 60
    val remStr = if (remH > 0) "%d:%02d:%02d".format(remH, remM, remS)
                 else          "%02d:%02d".format(remM, remS)

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Exit button pojok kanan atas ─────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onExitRequest,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        }

        // ── Main clock content ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Label
            Text(
                if (isDone) "Done! 🎉" else "Focus",
                color     = Color.White.copy(alpha = 0.3f),
                fontSize  = 12.sp,
                letterSpacing = 3.sp,
            )

            Spacer(Modifier.height(24.dp))

            // Jam sekarang — besar
            Text(
                currentTime,
                color      = Color.White,
                fontSize   = 72.sp,
                fontWeight = FontWeight.Thin,
                letterSpacing = 4.sp,
                textAlign  = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            // Countdown remaining
            Text(
                remStr,
                color      = if (isDone) Color(0xFF27AE60) else Color.White.copy(alpha = 0.6f),
                fontSize   = 24.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(32.dp))

            // Progress bar tipis
            LinearProgressIndicator(
                progress    = { progress },
                modifier    = Modifier.fillMaxWidth().height(1.dp),
                color       = Color(0xFF27AE60),
                trackColor  = Color.White.copy(alpha = 0.06f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EXIT CONFIRM OVERLAY
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ExitConfirmOverlay(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C1E))
                .padding(28.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("End focus session?", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Text(
                "Screen timeout will be restored\nto its original setting.",
                color     = Color.White.copy(alpha = 0.45f),
                fontSize  = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                ) { Text("Stay") }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                ) { Text("Exit", color = Color.White, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
private enum class PomodoroPhase { SETUP, RUNNING }

private fun currentTimeString(): String {
    val c = Calendar.getInstance()
    return "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
}