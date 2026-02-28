package id.satria.launcher.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import id.satria.launcher.MainViewModel
import id.satria.launcher.data.AppData
import id.satria.launcher.ui.component.*
import id.satria.launcher.ui.theme.LocalAppTheme
import id.satria.launcher.ui.theme.SatriaColors
import kotlin.math.ceil
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen
//
// Pemicu Recent Apps:
//   • Swipe horizontal dari tepi KIRI layar (zona 0–48 dp)
//   • Tidak menggunakan accessibility service, tidak ada event bus,
//     tidak bergantung pada navigation gesture sistem
//
// RAM:
//   • derivedStateOf untuk semua turunan state
//   • beyondViewportPageCount=1 (hanya pre-render 1 halaman di luar viewport)
//   • iconCache LruCache 3 MB, icon 88px
//   • RecentAppsOverlay hanya muncul saat showRecents=true (tidak selalu di tree)
//   • refreshRecentApps() hanya dipanggil on-demand saat panel dibuka
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: MainViewModel, onRequestOverlayPermission: () -> Unit = {}) {
    val filteredApps      by vm.filteredApps.collectAsState()
    val dockApps          by vm.dockApps.collectAsState()
    val allApps           by vm.allApps.collectAsState()
    val layoutMode        by vm.layoutMode.collectAsState()
    val showNames         by vm.showNames.collectAsState()
    val avatarPath        by vm.avatarPath.collectAsState()
    val avatarVersion     by vm.avatarVersion.collectAsState()
    val iconSize          by vm.iconSize.collectAsState()
    val dockIconSize      by vm.dockIconSize.collectAsState()
    val hiddenPackages    by vm.hiddenPackages.collectAsState()
    val gridCols          by vm.gridCols.collectAsState()
    val gridRows          by vm.gridRows.collectAsState()
    val darkMode          by vm.darkMode.collectAsState()
    val recentAppsEnabled by vm.recentAppsEnabled.collectAsState()
    val recentPackages    by vm.recentApps.collectAsState()
    val hasUsagePermission by vm.hasUsagePermission.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showRecents  by remember { mutableStateOf(false) }
    var recentsOpenCount by remember { mutableIntStateOf(0) }
    var actionTarget by remember { mutableStateOf<String?>(null) }
    var dashboardScrollRequest by remember { mutableIntStateOf(0) }
    var dashboardVisible by remember { mutableStateOf(false) }
    var pomodoroActive by remember { mutableStateOf(false) }

    val overlayActive by remember {
        derivedStateOf { showSettings || actionTarget != null || pomodoroActive || showRecents }
    }

    BackHandler(enabled = overlayActive) {
        when {
            showRecents          -> showRecents  = false
            actionTarget != null -> actionTarget = null
            showSettings         -> showSettings = false
        }
    }

    LaunchedEffect(Unit) {
        vm.checkUsagePermission()
    }

    // Refresh recents every time the panel is opened
    LaunchedEffect(showRecents) {
        if (showRecents) {
            recentsOpenCount++
            vm.checkUsagePermission()
            vm.refreshRecentApps()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Left-edge swipe zone → Recent Apps ──────────────────────────────
        // Zona 56dp di tepi kiri: swipe kanan ≥80dp → buka recent apps
        if (!overlayActive && recentAppsEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(56.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        var startX = 0f
                        var startY = 0f
                        var triggered = false
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                startX = offset.x
                                startY = offset.y
                                triggered = false
                            },
                            onDragEnd = {},
                            onDragCancel = {},
                            onHorizontalDrag = { change, dragAmount ->
                                if (!triggered) {
                                    val totalX = change.position.x - startX
                                    val totalY = change.position.y - startY
                                    if (totalX > 80f && kotlin.math.abs(totalY) < 100f) {
                                        triggered = true
                                        if (!showRecents) showRecents = true
                                    }
                                }
                            },
                        )
                    },
            )
        }

        // ── App Content ──────────────────────────────────────────────────────
        HomeContent(
            filteredApps = filteredApps,
            layoutMode = layoutMode,
            showNames = showNames,
            iconSize = iconSize,
            gridCols = gridCols,
            gridRows = gridRows,
            darkMode = darkMode,
            overlayActive = overlayActive,
            onAppPress = { if (!overlayActive) vm.launchApp(it) },
            onAppLong  = { if (!overlayActive) actionTarget = it },
            onBgLongPress = { if (!overlayActive) showSettings = true },
            dashboardContent = { onClose ->
                DashboardScreen(
                    vm = vm,
                    onClose = onClose,
                    onPomodoroChanged = { pomodoroActive = it },
                )
            },
            dashboardScrollRequest = dashboardScrollRequest,
            onDashboardChanged = { dashboardVisible = it },
        )

        // ── Dock ─────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = !dashboardVisible,
            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it }),
            exit  = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Dock(
                dockApps = dockApps,
                avatarPath = avatarPath,
                avatarVersion = avatarVersion,
                dockIconSize = dockIconSize,
                onAvatarClick    = { if (!overlayActive) dashboardScrollRequest++ },
                onAppPress       = { if (!overlayActive) vm.launchApp(it) },
                onAppLongPress   = { if (!overlayActive) actionTarget = it },
                onLongPressSettings = { if (!overlayActive) showSettings = true },
            )
        }

        // ── Recent Apps Overlay ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = showRecents,
            enter = fadeIn(tween(160)) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(280, easing = FastOutSlowInEasing),
            ),
            exit = fadeOut(tween(180)) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(220, easing = FastOutSlowInEasing),
            ),
        ) {
            // key(recentsOpenCount) memaksa RecentAppsOverlay di-recompose dari awal
            // setiap kali overlay dibuka — dismissedPkgsList pun reset otomatis
            key(recentsOpenCount) {
            RecentAppsOverlay(
                recentPackages = recentPackages,
                allApps = allApps,
                hasPermission = hasUsagePermission,
                onAppPress = { pkg ->
                    showRecents = false
                    vm.launchApp(pkg)
                },
                onAppLong = { pkg ->
                    showRecents = false
                    actionTarget = pkg
                },
                onDismiss = { showRecents = false },
                onRequestPermission = {
                    vm.openUsagePermissionSettings()
                    showRecents = false
                },
                // Fix: clearAll benar-benar menghapus data, bukan hanya tutup overlay
                onClearAll = {
                    vm.clearRecentApps()
                    showRecents = false
                },
            )
            }
        }

        // ── Settings ─────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200)),
            exit  = fadeOut(tween(160)) + scaleOut(targetScale = 0.95f, animationSpec = tween(160)),
        ) { SettingsSheet(vm = vm, onClose = { showSettings = false }) }

        // ── App Action Sheet ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = actionTarget != null,
            enter = slideInVertically { it / 2 } + fadeIn(tween(180)),
            exit  = slideOutVertically { it / 2 } + fadeOut(tween(130)),
        ) {
            actionTarget?.let { pkg ->
                AppActionSheet(
                    pkg = pkg,
                    label = filteredApps.find { it.packageName == pkg }?.label
                        ?: dockApps.find { it.packageName == pkg }?.label
                        ?: pkg,
                    isHidden  = hiddenPackages.contains(pkg),
                    isDocked  = dockApps.any { it.packageName == pkg },
                    dockFull  = dockApps.size >= 5,
                    onClose   = { actionTarget = null },
                    onHide    = { vm.hideApp(pkg);    actionTarget = null },
                    onUnhide  = { vm.unhideApp(pkg);  actionTarget = null },
                    onDock    = { vm.toggleDock(pkg); actionTarget = null },
                    onUninstall = { vm.uninstallApp(pkg); actionTarget = null },
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// HomeContent
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    filteredApps: List<AppData>,
    layoutMode: String,
    showNames: Boolean,
    iconSize: Int,
    gridCols: Int,
    gridRows: Int,
    darkMode: Boolean,
    overlayActive: Boolean,
    onAppPress: (String) -> Unit,
    onAppLong: (String) -> Unit,
    onBgLongPress: () -> Unit,
    dashboardContent: @Composable (onClose: () -> Unit) -> Unit = {},
    dashboardScrollRequest: Int = 0,
    onDashboardChanged: (Boolean) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = { if (!overlayActive) onBgLongPress() },
            ),
    ) {
        if (layoutMode == "grid") {
            IosPagedGrid(
                apps = filteredApps,
                showNames = showNames,
                iconSize = iconSize,
                cols = gridCols,
                rows = gridRows,
                overlayActive = overlayActive,
                onPress = onAppPress,
                onLongPress = onAppLong,
                dashboardContent = dashboardContent,
                dashboardScrollRequest = dashboardScrollRequest,
                onDashboardVisibleChanged = onDashboardChanged,
            )
        } else {
            val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
            val scope = rememberCoroutineScope()

            BackHandler(enabled = pagerState.currentPage == 0) {
                scope.launch { pagerState.animateScrollToPage(1) }
            }

            LaunchedEffect(dashboardScrollRequest) {
                if (dashboardScrollRequest > 0) pagerState.animateScrollToPage(0)
            }

            LaunchedEffect(pagerState.currentPage) {
                onDashboardChanged(pagerState.currentPage == 0)
            }

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = !overlayActive,
                modifier = Modifier.fillMaxSize(),
                key = { it },
            ) { page ->
                if (page == 0) {
                    dashboardContent { scope.launch { pagerState.animateScrollToPage(1) } }
                } else {
                    IosListView(
                        apps = filteredApps,
                        iconSize = iconSize,
                        darkMode = darkMode,
                        overlayActive = overlayActive,
                        onPress = onAppPress,
                        onLongPress = onAppLong,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IosPagedGrid
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosPagedGrid(
    apps: List<AppData>,
    showNames: Boolean,
    iconSize: Int,
    cols: Int,
    rows: Int,
    overlayActive: Boolean,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit,
    dashboardContent: @Composable (onClose: () -> Unit) -> Unit,
    dashboardScrollRequest: Int,
    onDashboardVisibleChanged: (Boolean) -> Unit = {},
) {
    if (apps.isEmpty()) return

    val itemsPerPage by remember(cols, rows) { derivedStateOf { cols * rows } }
    val appPageCount by remember(apps.size, itemsPerPage) {
        derivedStateOf { ceil(apps.size / itemsPerPage.toFloat()).toInt().coerceAtLeast(1) }
    }
    val totalPageCount = appPageCount + 1

    val pagerState = rememberPagerState(initialPage = 1, pageCount = { totalPageCount })
    val scope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage == 0) {
        scope.launch { pagerState.animateScrollToPage(1) }
    }

    LaunchedEffect(dashboardScrollRequest) {
        if (dashboardScrollRequest > 0) pagerState.animateScrollToPage(0)
    }

    LaunchedEffect(pagerState.currentPage) {
        onDashboardVisibleChanged(pagerState.currentPage == 0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            userScrollEnabled = !overlayActive,
            modifier = Modifier.fillMaxSize(),
            key = { it },
        ) { page ->
            if (page == 0) {
                dashboardContent { scope.launch { pagerState.animateScrollToPage(1) } }
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 148.dp)) {
                    val appPage = page - 1
                    val from = appPage * itemsPerPage
                    val to   = minOf(from + itemsPerPage, apps.size)
                    val pageApps = remember(apps, from, to) { apps.subList(from, to) }
                    IosGridPage(
                        apps = pageApps,
                        showNames = showNames,
                        iconSize = iconSize,
                        cols = cols,
                        rows = rows,
                        overlayActive = overlayActive,
                        onPress = onPress,
                        onLongPress = onLongPress,
                    )
                }
            }
        }

        if (appPageCount > 1 && pagerState.currentPage > 0) {
            IosPageDots(
                pageCount = appPageCount,
                currentPage = pagerState.currentPage - 1,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 154.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IosGridPage
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IosGridPage(
    apps: List<AppData>,
    showNames: Boolean,
    iconSize: Int,
    cols: Int,
    rows: Int,
    overlayActive: Boolean,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 12.dp, bottom = 12.dp)) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (col in 0 until cols) {
                    val idx = row * cols + col
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (idx < apps.size) {
                            IosAppIcon(
                                app = apps[idx],
                                showName = showNames,
                                iconSizeDp = iconSize,
                                onPress    = { if (!overlayActive) onPress(it) },
                                onLongPress = { if (!overlayActive) onLongPress(it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IosAppIcon
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosAppIcon(
    app: AppData,
    showName: Boolean,
    iconSizeDp: Int,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "iconScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.70f else 1f,
        animationSpec = tween(50),
        label = "iconAlpha",
    )

    val bitmap = remember(app.packageName) { iconCache.get(app.packageName) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 3.dp, vertical = 5.dp)
            .scale(scale)
            .alpha(alpha)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick    = { onPress(app.packageName) },
                onLongClick = { onLongPress(app.packageName) },
            ),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier.size(iconSizeDp.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(iconSizeDp.dp)
                    .clip(RoundedCornerShape((iconSizeDp * 0.22f).dp))
                    .background(Color.White.copy(alpha = 0.12f)),
            )
        }

        if (showName) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = app.label,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = SatriaColors.TextPrimary.copy(alpha = 0.90f),
                modifier = Modifier.width((iconSizeDp + 14).dp),
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.30f),
                        offset = Offset(0f, 1f),
                        blurRadius = 3f,
                    ),
                    fontSize = 10.sp,
                ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IosPageDots
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IosPageDots(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    val darkMode = LocalAppTheme.current.darkMode
    val activeColor   = if (darkMode) Color.White else Color(0xFF1C1C1E)
    val inactiveColor = if (darkMode) Color(0x50FFFFFF) else Color(0x55000000)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { i ->
            val active = i == currentPage
            val dotW by animateDpAsState(
                targetValue = if (active) 18.dp else 7.dp,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "dw$i",
            )
            val dotColor by animateColorAsState(
                targetValue = if (active) activeColor else inactiveColor,
                animationSpec = tween(180),
                label = "dc$i",
            )
            Box(
                modifier = Modifier
                    .width(dotW).height(7.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IosListView
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IosListView(
    apps: List<AppData>,
    iconSize: Int,
    darkMode: Boolean,
    overlayActive: Boolean,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    val sorted by remember(apps) { derivedStateOf { apps.sortedBy { it.label.lowercase() } } }

    LazyColumn(
        contentPadding = PaddingValues(top = 56.dp, bottom = 148.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(sorted, key = { it.packageName }) { app ->
            IosListRow(
                app = app,
                iconSizeDp = iconSize,
                darkMode = darkMode,
                overlayActive = overlayActive,
                onPress = onPress,
                onLongPress = onLongPress,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosListRow(
    app: AppData,
    iconSizeDp: Int,
    darkMode: Boolean,
    overlayActive: Boolean,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bitmap = remember(app.packageName) { iconCache.get(app.packageName) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick    = { if (!overlayActive) onPress(app.packageName) },
                onLongClick = { if (!overlayActive) onLongPress(app.packageName) },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(modifier = Modifier.size(iconSizeDp.dp).clip(RoundedCornerShape((iconSizeDp * 0.22f).dp))) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.label,
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(if (darkMode) Color(0xFF3A3A3C) else Color(0xFFE5E5EA))
                    )
                }
            }
            Text(
                text = app.label,
                color = SatriaColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
