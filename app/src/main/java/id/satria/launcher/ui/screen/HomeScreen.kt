package id.satria.launcher.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import id.satria.launcher.MainViewModel
import id.satria.launcher.ui.component.*

@Composable
fun HomeScreen(vm: MainViewModel) {
    val filteredApps   by vm.filteredApps.collectAsState()
    val dockApps       by vm.dockApps.collectAsState()
    val layoutMode     by vm.layoutMode.collectAsState()
    val showNames      by vm.showNames.collectAsState()
    val avatarPath     by vm.avatarPath.collectAsState()
    val hiddenPackages by vm.hiddenPackages.collectAsState()

    var showDashboard by remember { mutableStateOf(false) }
    var showSettings  by remember { mutableStateOf(false) }
    var actionTarget  by remember { mutableStateOf<String?>(null) }

    val overlayActive = showDashboard || showSettings || actionTarget != null

    BackHandler(enabled = overlayActive) {
        when {
            actionTarget != null -> actionTarget = null
            showSettings         -> showSettings = false
            showDashboard        -> showDashboard = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── App list ───────────────────────────────────────────────────────
        if (layoutMode == "grid") {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(top = 56.dp, bottom = 140.dp, start = 8.dp, end = 8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppGridItem(
                        app = app, showName = showNames,
                        onPress = { if (!overlayActive) vm.launchApp(it) },
                        onLongPress = { if (!overlayActive) actionTarget = it },
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 56.dp, bottom = 140.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppListItem(
                        app = app, showName = showNames,
                        onPress = { if (!overlayActive) vm.launchApp(it) },
                        onLongPress = { if (!overlayActive) actionTarget = it },
                    )
                }
            }
        }

        // ── Dock ───────────────────────────────────────────────────────────
        Dock(
            dockApps            = dockApps,
            avatarPath          = avatarPath,
            onAvatarClick       = { if (!overlayActive) showDashboard = true },
            onAppPress          = { if (!overlayActive) vm.launchApp(it) },
            onAppLongPress      = { if (!overlayActive) actionTarget = it },
            onLongPressSettings = { if (!overlayActive) showSettings = true },
            modifier            = Modifier.align(Alignment.BottomCenter),
        )

        // ── Dashboard — slide in from bottom ───────────────────────────────
        AnimatedVisibility(
            visible = showDashboard,
            enter   = slideInVertically { it } + fadeIn(tween(250)),
            exit    = slideOutVertically { it } + fadeOut(tween(200)),
        ) {
            DashboardScreen(vm = vm, onClose = { showDashboard = false })
        }

        // ── Settings — fade + scale ────────────────────────────────────────
        AnimatedVisibility(
            visible = showSettings,
            enter   = fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)),
            exit    = fadeOut(tween(180)) + scaleOut(targetScale = 0.92f, animationSpec = tween(180)),
        ) {
            SettingsSheet(vm = vm, onClose = { showSettings = false })
        }

        // ── Action sheet ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = actionTarget != null,
            enter   = slideInVertically { it / 2 } + fadeIn(tween(200)),
            exit    = slideOutVertically { it / 2 } + fadeOut(tween(150)),
        ) {
            actionTarget?.let { pkg ->
                AppActionSheet(
                    pkg      = pkg,
                    label    = filteredApps.find { it.packageName == pkg }?.label
                               ?: dockApps.find { it.packageName == pkg }?.label ?: pkg,
                    isHidden = hiddenPackages.contains(pkg),
                    isDocked = dockApps.any { it.packageName == pkg },
                    dockFull = dockApps.size >= 4,
                    onClose      = { actionTarget = null },
                    onHide       = { vm.hideApp(pkg);    actionTarget = null },
                    onUnhide     = { vm.unhideApp(pkg);  actionTarget = null },
                    onDock       = { vm.toggleDock(pkg); actionTarget = null },
                    onUninstall  = { vm.uninstallApp(pkg); actionTarget = null },
                )
            }
        }
    }
}