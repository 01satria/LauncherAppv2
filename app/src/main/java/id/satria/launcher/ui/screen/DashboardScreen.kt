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

// Calculator is the only fullscreen tool now
private val FULLSCREEN_TOOLS = setOf("calculator")

@Composable
fun DashboardScreen(vm: MainViewModel, onClose: () -> Unit) {
    val context       = LocalContext.current
    val activity      = context as? Activity
    val userName      by vm.userName.collectAsState()
    val assistantName by vm.assistantName.collectAsState()
    val avatarPath    by vm.avatarPath.collectAsState()
    val todos         by vm.todos.collectAsState()
    val countdowns    by vm.countdowns.collectAsState()
    val habits        by vm.habits.collectAsState()

    // derivedStateOf: hanya re-compute ketika todos/habits berubah,
    // bukan setiap recompose — mencegah alokasi Int baru setiap frame
    val todoPending by remember { derivedStateOf { todos.count { !it.done }.takeIf { it > 0 } } }
    val habitDone   by remember { derivedStateOf { habits.count { it.doneToday(todayKey) } } }
    val habitTotal  by remember { derivedStateOf { habits.size } }
    var activeTool   by remember { mutableStateOf<String?>(null) }
    var showChat     by remember { mutableStateOf(false) }
    var showPomodoro by remember { mutableStateOf(false) }

    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    DisposableEffect(showPomodoro) {
        activity?.requestedOrientation = if (showPomodoro) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }

    BackHandler {
        when {
            showPomodoro       -> {}
            showChat           -> showChat = false
            activeTool != null -> activeTool = null
            else               -> onClose()
        }
    }

    if (showPomodoro) { PomodoroScreen(onExit = { showPomodoro = false }); return }

    // Fullscreen tools
    if (activeTool in FULLSCREEN_TOOLS) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SatriaColors.ScreenBackground)
                .navigationBarsPadding()
        ) {
            when (activeTool) {
                "calculator" -> CalculatorTool()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(SatriaColors.ScreenBackground.copy(alpha = 0.95f))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Button(
                    onClick = { activeTool = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.Surface)
                ) { Text("Back", color = SatriaColors.TextPrimary) }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SatriaColors.ScreenBackground)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
    ) {
        if (showChat) {
            ChatScreen(vm = vm, onClose = { showChat = false })
        } else {
            Column(modifier = Modifier.fillMaxSize()) {

                if (activeTool == null) {
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        DashboardHeader(
                            avatarPath    = avatarPath,
                            assistantName = assistantName,
                            userName      = userName,
                            onAvatarClick = { showChat = true },
                            onClose       = onClose,
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(SatriaColors.BorderLight))
                        ToolGridNoScroll(
                            todoPending  = todoPending,
                            cdFirst      = countdowns.firstOrNull(),
                            habitDone    = habitDone,
                            habitTotal   = habitTotal,
                            onWeather    = { activeTool = "weather" },
                            onMoney      = { activeTool = "money" },
                            onTodo       = { activeTool = "todo" },
                            onCountdown  = { activeTool = "countdown" },
                            onPomodoro   = { showPomodoro = true },
                            onCalculator = { activeTool = "calculator" },
                            onConverter  = { activeTool = "converter" },
                            onHabits     = { activeTool = "habits" },
                            onPrayer     = { activeTool = "prayer" },
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                } else {
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
                                "money"     -> CurrencyTool()
                                "todo"      -> TodoTool(todos = todos,
                                    onAdd = { vm.addTodo(it) }, onToggle = { vm.toggleTodo(it) }, onRemove = { vm.removeTodo(it) })
                                "countdown" -> CountdownTool(countdowns = countdowns,
                                    onAdd = { n, d -> vm.addCountdown(n, d) }, onRemove = { vm.removeCountdown(it) })
                                "converter" -> ConverterTool()
                                "prayer"    -> PrayerTool(
                                    savedCities   = vm.prayerCities.collectAsState().value,
                                    savedCacheJson = vm.prayerCache.collectAsState().value,
                                    onAddCity     = { vm.addPrayerCity(it) },
                                    onRemoveCity  = { vm.removePrayerCity(it) },
                                    onUpdateCache = { city, json -> vm.updatePrayerCache(city, json) })
                                "habits"    -> HabitTool(habits = habits,
                                    onAdd = { n, e -> vm.addHabit(n, e) }, onToggle = { vm.checkHabit(it) }, onDelete = { vm.deleteHabit(it) })
                                else        -> {}
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SatriaColors.ScreenBackground)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .navigationBarsPadding()
                    ) {
                        Button(
                            onClick = { activeTool = null },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.Surface)
                        ) { Text("Back", color = SatriaColors.TextPrimary) }
                    }
                }
            }
        }
    }
}
