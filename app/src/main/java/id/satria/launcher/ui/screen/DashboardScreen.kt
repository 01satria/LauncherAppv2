package id.satria.launcher.ui.screen

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
import androidx.compose.ui.unit.dp
import id.satria.launcher.MainViewModel
import id.satria.launcher.ui.component.*
import id.satria.launcher.ui.theme.SatriaColors

@Composable
fun DashboardScreen(vm: MainViewModel, onClose: () -> Unit) {
    val userName      by vm.userName.collectAsState()
    val avatarPath    by vm.avatarPath.collectAsState()
    val todos         by vm.todos.collectAsState()
    val countdowns    by vm.countdowns.collectAsState()

    var activeTool by remember { mutableStateOf<String?>(null) }
    var showChat   by remember { mutableStateOf(false) }

    BackHandler {
        when {
            showChat           -> showChat = false
            activeTool != null -> activeTool = null
            else               -> onClose()
        }
    }

    // ── Block semua sentuhan — fullscreen opaque ──────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SatriaColors.ScreenBackground)
            // Intercept semua sentuhan agar tidak tembus ke HomeScreen
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Block */ },
    ) {
        if (showChat) {
            ChatScreen(vm = vm, onClose = { showChat = false })
        } else {
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                DashboardHeader(
                    avatarPath    = avatarPath,
                    userName      = userName,
                    onAvatarClick = { showChat = true },
                    onClose       = onClose,
                )
                HorizontalDivider(color = SatriaColors.Border, thickness = 1.dp)

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = activeTool,
                        transitionSpec = {
                            if (targetState != null) {
                                (slideInHorizontally { it / 4 } + fadeIn(tween(200))).togetherWith(
                                    slideOutHorizontally { -it / 4 } + fadeOut(tween(150)))
                            } else {
                                (slideInHorizontally { -it / 4 } + fadeIn(tween(200))).togetherWith(
                                    slideOutHorizontally { it / 4 } + fadeOut(tween(150)))
                            }
                        },
                        label = "toolAnim",
                    ) { tool ->
                        when (tool) {
                            "weather"   -> WeatherTool(
                                savedLocations   = vm.weatherLocations.collectAsState().value,
                                onAddLocation    = { vm.addWeatherLocation(it) },
                                onRemoveLocation = { vm.removeWeatherLocation(it) },
                            )
                            "money"     -> MoneyTool()
                            "todo"      -> TodoTool(
                                todos    = todos,
                                onAdd    = { vm.addTodo(it) },
                                onToggle = { vm.toggleTodo(it) },
                                onRemove = { vm.removeTodo(it) },
                            )
                            "countdown" -> CountdownTool(
                                countdowns = countdowns,
                                onAdd      = { name, date -> vm.addCountdown(name, date) },
                                onRemove   = { vm.removeCountdown(it) },
                            )
                            else -> ToolGrid(
                                todoPending = todos.count { !it.done }.takeIf { it > 0 },
                                cdFirst     = countdowns.firstOrNull(),
                                onWeather   = { activeTool = "weather" },
                                onMoney     = { activeTool = "money" },
                                onTodo      = { activeTool = "todo" },
                                onCountdown = { activeTool = "countdown" },
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = activeTool != null,
                    enter   = slideInVertically { it } + fadeIn(tween(200)),
                    exit    = slideOutVertically { it } + fadeOut(tween(150)),
                ) {
                    Column {
                        HorizontalDivider(color = SatriaColors.Border)
                        Box(modifier = Modifier.fillMaxWidth().background(SatriaColors.ScreenBackground)
                            .padding(horizontal = 20.dp, vertical = 10.dp).navigationBarsPadding()) {
                            Button(
                                onClick = { activeTool = null },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.Surface),
                            ) { Text("← Back", color = SatriaColors.TextPrimary) }
                        }
                    }
                }
            }
        }
    }
}