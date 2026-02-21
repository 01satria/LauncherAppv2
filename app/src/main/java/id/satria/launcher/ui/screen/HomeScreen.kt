package id.satria.launcher.ui.screen

import androidx.activity.compose.BackHandler
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
    val userName       by vm.userName.collectAsState()
    val assistantName  by vm.assistantName.collectAsState()
    val hiddenPackages by vm.hiddenPackages.collectAsState()

    var showDashboard by remember { mutableStateOf(false) }
    var showSettings  by remember { mutableStateOf(false) }
    var actionTarget  by remember { mutableStateOf<String?>(null) }

    // Hardware back — tutup overlay jika ada yang terbuka
    BackHandler(enabled = showDashboard || showSettings || actionTarget != null) {
        when {
            actionTarget != null  -> actionTarget = null
            showSettings          -> showSettings = false
            showDashboard         -> showDashboard = false
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
                        app         = app,
                        showName    = showNames,
                        onPress     = { vm.launchApp(it) },
                        onLongPress = { actionTarget = it },
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
                        app         = app,
                        showName    = showNames,
                        onPress     = { vm.launchApp(it) },
                        onLongPress = { actionTarget = it },
                    )
                }
            }
        }

        // ── Dock ───────────────────────────────────────────────────────────
        Dock(
            dockApps            = dockApps,
            avatarPath          = avatarPath,
            onAvatarClick       = { showDashboard = true },
            onAppPress          = { vm.launchApp(it) },
            onAppLongPress      = { actionTarget = it },
            onLongPressSettings = { showSettings = true },
            modifier            = Modifier.align(Alignment.BottomCenter),
        )

        // ── Overlays — hanya mount saat dibutuhkan ─────────────────────────
        if (showDashboard) {
            DashboardScreen(
                vm      = vm,
                onClose = { showDashboard = false },
            )
        }

        if (showSettings) {
            SettingsSheet(
                vm      = vm,
                onClose = { showSettings = false },
            )
        }

        actionTarget?.let { pkg ->
            AppActionSheet(
                pkg        = pkg,
                label      = filteredApps.find { it.packageName == pkg }?.label
                             ?: dockApps.find { it.packageName == pkg }?.label
                             ?: pkg,
                isHidden   = hiddenPackages.contains(pkg),
                isDocked   = dockApps.any { it.packageName == pkg },
                dockFull   = dockApps.size >= 5,
                onClose    = { actionTarget = null },
                onHide     = { vm.hideApp(pkg);    actionTarget = null },
                onUnhide   = { vm.unhideApp(pkg);  actionTarget = null },
                onDock     = { vm.toggleDock(pkg); actionTarget = null },
                onUninstall = { vm.uninstallApp(pkg); actionTarget = null },
            )
        }
    }
}
