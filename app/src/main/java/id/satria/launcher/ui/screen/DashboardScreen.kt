package id.satria.launcher.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.satria.launcher.MainViewModel
import id.satria.launcher.ui.component.*
import id.satria.launcher.ui.theme.SatriaColors
import java.text.SimpleDateFormat
import java.util.*

private val BLACK = Color(0xFF000000)

// Tools that render fullscreen (extreme RAM saving — no composable stack underneath)
private val FULLSCREEN_TOOLS = setOf("calculator", "stopwatch")

@Composable
fun DashboardScreen(vm: MainViewModel, onClose: () -> Unit) {
    val context       = LocalContext.current
    val activity      = context as? Activity
    val userName      by vm.userName.collectAsState()
    val assistantName by vm.assistantName.collectAsState()
    val avatarPath    by vm.avatarPath.collectAsState()
    val todos         by vm.todos.collectAsState()
    val countdowns    by vm.countdowns.collectAsState()
    val notes         by vm.notes.collectAsState()
    val habits        by vm.habits.collectAsState()

    var activeTool   by remember { mutableStateOf<String?>(null) }
    var showChat     by remember { mutableStateOf(false) }
    var showPomodoro by remember { mutableStateOf(false) }

    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    DisposableEffect(showPomodoro) {
        activity?.requestedOrientation = if (showPomodoro)
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }

    BackHandler {
        when {
            showPomodoro                           -> { }
            showChat                               -> showChat = false
            activeTool != null                     -> activeTool = null
            else                                   -> onClose()
        }
    }

    if (showPomodoro) {
        PomodoroScreen(onExit = { showPomodoro = false })
        return
    }

    // Fullscreen tools — render exclusively to save RAM (no header/grid underneath)
    if (activeTool in FULLSCREEN_TOOLS) {
        Box(modifier = Modifier.fillMaxSize().background(BLACK).systemBarsPadding()) {
            when (activeTool) {
                "calculator" -> CalculatorTool()
                "stopwatch"  -> StopwatchTool()
            }
            // Minimal back button overlay at bottom
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color(0xCC000000)).padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding()) {
                Button(onClick = { activeTool = null }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.Surface)) {
                    Text("Back", color = SatriaColors.TextPrimary)
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(BLACK)
        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }) {
        if (showChat) {
            ChatScreen(vm = vm, onClose = { showChat = false })
        } else {
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {

                DashboardHeader(
                    avatarPath    = avatarPath,
                    assistantName = assistantName,
                    userName      = userName,
                    onAvatarClick = { showChat = true },
                    onClose       = onClose,
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SatriaColors.BorderLight))

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = activeTool,
                        transitionSpec = {
                            if (targetState != null)
                                (slideInHorizontally { it / 4 } + fadeIn(tween(200))).togetherWith(slideOutHorizontally { -it / 4 } + fadeOut(tween(150)))
                            else
                                (slideInHorizontally { -it / 4 } + fadeIn(tween(200))).togetherWith(slideOutHorizontally { it / 4 } + fadeOut(tween(150)))
                        },
                        label = "toolAnim",
                    ) { tool ->
                        when (tool) {
                            "weather"   -> WeatherTool(
                                savedLocations   = vm.weatherLocations.collectAsState().value,
                                onAddLocation    = { vm.addWeatherLocation(it) },
                                onRemoveLocation = { vm.removeWeatherLocation(it) })
                            "money"     -> MoneyTool()
                            "todo"      -> TodoTool(
                                todos    = todos,
                                onAdd    = { vm.addTodo(it) },
                                onToggle = { vm.toggleTodo(it) },
                                onRemove = { vm.removeTodo(it) })
                            "countdown" -> CountdownTool(
                                countdowns = countdowns,
                                onAdd      = { name, date -> vm.addCountdown(name, date) },
                                onRemove   = { vm.removeCountdown(it) })
                            "notes"     -> NotesTool(
                                notes    = notes,
                                onAdd    = { vm.addNote(it) },
                                onUpdate = { id, text -> vm.updateNote(id, text) },
                                onDelete = { vm.deleteNote(it) })
                            "converter" -> ConverterTool()
                            "habits"    -> HabitTool(
                                habits   = habits,
                                onAdd    = { name, emoji -> vm.addHabit(name, emoji) },
                                onToggle = { vm.checkHabit(it) },
                                onDelete = { vm.deleteHabit(it) })
                            else -> ToolGrid(
                                todoPending  = todos.count { !it.done }.takeIf { it > 0 },
                                cdFirst      = countdowns.firstOrNull(),
                                noteCount    = notes.size,
                                habitDone    = habits.count { it.doneToday(todayKey) },
                                habitTotal   = habits.size,
                                onWeather    = { activeTool = "weather" },
                                onMoney      = { activeTool = "money" },
                                onTodo       = { activeTool = "todo" },
                                onCountdown  = { activeTool = "countdown" },
                                onPomodoro   = { showPomodoro = true },
                                onCalculator = { activeTool = "calculator" },
                                onStopwatch  = { activeTool = "stopwatch" },
                                onNotes      = { activeTool = "notes" },
                                onConverter  = { activeTool = "converter" },
                                onHabits     = { activeTool = "habits" })
                        }
                    }
                }

                AnimatedVisibility(
                    visible = activeTool != null && activeTool !in FULLSCREEN_TOOLS,
                    enter   = slideInVertically { it } + fadeIn(tween(200)),
                    exit    = slideOutVertically { it } + fadeOut(tween(150)),
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SatriaColors.BorderLight))
                        Box(modifier = Modifier.fillMaxWidth().background(BLACK)
                            .padding(horizontal = 20.dp, vertical = 10.dp).navigationBarsPadding()) {
                            Button(onClick = { activeTool = null }, modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.Surface)) {
                                Text("Back", color = SatriaColors.TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
