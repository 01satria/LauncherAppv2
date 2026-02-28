package id.satria.launcher.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val filteredApps by vm.filteredApps.collectAsState()
    val dockApps by vm.dockApps.collectAsState()
    val layoutMode by vm.layoutMode.collectAsState()
    val showNames by vm.showNames.collectAsState()
    val avatarPath by vm.avatarPath.collectAsState()
    val avatarVersion by vm.avatarVersion.collectAsState()
    val iconSize by vm.iconSize.collectAsState()
    val dockIconSize by vm.dockIconSize.collectAsState()
    val hiddenPackages by vm.hiddenPackages.collectAsState()
    val gridCols by vm.gridCols.collectAsState()
    val gridRows by vm.gridRows.collectAsState()
    val darkMode by vm.darkMode.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<String?>(null) }
    var dashboardScrollRequest by remember { mutableIntStateOf(0) }
    var dashboardVisible by remember { mutableStateOf(false) }
    var pomodoroActive by remember { mutableStateOf(false) }

    val overlayActive by remember {
        derivedStateOf { showSettings || actionTarget != null || pomodoroActive }
    }

    BackHandler(enabled = overlayActive) {
        when {
            actionTarget != null -> actionTarget = null
            showSettings -> showSettings = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeContent(
                filteredApps = filteredApps,
                dockApps = dockApps,
                layoutMode = layoutMode,
                showNames = showNames,
                iconSize = iconSize,
                gridCols = gridCols,
                gridRows = gridRows,
                darkMode = darkMode,
                overlayActive = overlayActive,
                onAppPress = { if (!overlayActive) vm.launchApp(it) },
                onAppLong = { if (!overlayActive) actionTarget = it },
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

        // ── Dock — HANYA muncul di mode grid ─────────────────────────────
        if (layoutMode == "grid") {
            AnimatedVisibility(
                    visible = !dashboardVisible,
                    enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it }),
                    modifier =
                            Modifier.align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .imePadding(),
            ) {
                Dock(
                        dockApps = dockApps,
                        avatarPath = avatarPath,
                        avatarVersion = avatarVersion,
                        dockIconSize = dockIconSize,
                        onAvatarClick = { if (!overlayActive) dashboardScrollRequest++ },
                        onAppPress = { if (!overlayActive) vm.launchApp(it) },
                        onAppLongPress = { if (!overlayActive) actionTarget = it },
                        onLongPressSettings = { if (!overlayActive) showSettings = true },
                )
            }
        }

        // ── Settings ─────────────────────────────────────────────────────
        AnimatedVisibility(
                visible = showSettings,
                enter =
                        fadeIn(tween(200)) +
                                scaleIn(initialScale = 0.95f, animationSpec = tween(200)),
                exit =
                        fadeOut(tween(160)) +
                                scaleOut(targetScale = 0.95f, animationSpec = tween(160)),
        ) { SettingsSheet(vm = vm, onClose = { showSettings = false }) }

        // ── App Action Sheet ─────────────────────────────────────────────
        AnimatedVisibility(
                visible = actionTarget != null,
                enter = slideInVertically { it / 2 } + fadeIn(tween(180)),
                exit = slideOutVertically { it / 2 } + fadeOut(tween(130)),
        ) {
            actionTarget?.let { pkg ->
                AppActionSheet(
                        pkg = pkg,
                        label = filteredApps.find { it.packageName == pkg }?.label
                                        ?: dockApps.find { it.packageName == pkg }?.label ?: pkg,
                        isHidden = hiddenPackages.contains(pkg),
                        isDocked = dockApps.any { it.packageName == pkg },
                        dockFull = dockApps.size >= 5,
                        onClose = { actionTarget = null },
                        onHide = {
                            vm.hideApp(pkg)
                            actionTarget = null
                        },
                        onUnhide = {
                            vm.unhideApp(pkg)
                            actionTarget = null
                        },
                        onDock = {
                            vm.toggleDock(pkg)
                            actionTarget = null
                        },
                        onUninstall = {
                            vm.uninstallApp(pkg)
                            actionTarget = null
                        },
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
        dockApps: List<AppData>,
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
            modifier =
                    Modifier.fillMaxSize()
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
            // ── Niagara list mode ─────────────────────────────────────────
            val allVisible =
                    remember(filteredApps, dockApps) {
                        (filteredApps + dockApps).distinctBy { it.packageName }.sortedBy {
                            it.label.lowercase()
                        }
                    }
            NiagaraListPager(
                    dockApps = dockApps,
                    allVisibleApps = allVisible,
                    darkMode = darkMode,
                    overlayActive = overlayActive,
                    onAppPress = onAppPress,
                    onAppLong = onAppLong,
                    onBgLongPress = onBgLongPress,
                    dashboardContent = dashboardContent,
                    dashboardScrollRequest = dashboardScrollRequest,
                    onDashboardChanged = onDashboardChanged,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NiagaraListPager
// Page 0 = Dashboard (swipe kanan)
// Page 1 = Niagara Home (favorit apps)
// Overlay = NiagaraAppDrawer (swipe atas)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NiagaraListPager(
        dockApps: List<AppData>,
        allVisibleApps: List<AppData>,
        darkMode: Boolean,
        overlayActive: Boolean,
        onAppPress: (String) -> Unit,
        onAppLong: (String) -> Unit,
        onBgLongPress: () -> Unit,
        dashboardContent: @Composable (onClose: () -> Unit) -> Unit,
        dashboardScrollRequest: Int,
        onDashboardChanged: (Boolean) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var showDrawer by remember { mutableStateOf(false) }

    BackHandler(enabled = pagerState.currentPage == 0 && !showDrawer) {
        scope.launch { pagerState.animateScrollToPage(1) }
    }
    BackHandler(enabled = showDrawer) { showDrawer = false }

    LaunchedEffect(dashboardScrollRequest) {
        if (dashboardScrollRequest > 0) {
            showDrawer = false
            pagerState.animateScrollToPage(0)
        }
    }
    LaunchedEffect(pagerState.currentPage) { onDashboardChanged(pagerState.currentPage == 0) }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = !overlayActive && !showDrawer,
                modifier = Modifier.fillMaxSize(),
                key = { it },
        ) { page ->
            if (page == 0) {
                dashboardContent { scope.launch { pagerState.animateScrollToPage(1) } }
            } else {
                NiagaraHomePage(
                        dockApps = dockApps,
                        darkMode = darkMode,
                        overlayActive = overlayActive,
                        onAppPress = onAppPress,
                        onAppLong = onAppLong,
                        onBgLongPress = onBgLongPress,
                        onSwipeUp = { if (!overlayActive) showDrawer = true },
                )
            }
        }

        // ── App Drawer — slide dari bawah ke atas ────────────────────────
        AnimatedVisibility(
                visible = showDrawer,
                enter =
                        slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(320, easing = FastOutSlowInEasing),
                        ) + fadeIn(tween(220)),
                exit =
                        slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(260, easing = FastOutLinearInEasing),
                        ) + fadeOut(tween(180)),
        ) {
            NiagaraAppDrawer(
                    apps = allVisibleApps,
                    darkMode = darkMode,
                    overlayActive = overlayActive,
                    onAppPress = onAppPress,
                    onAppLong = onAppLong,
                    onClose = { showDrawer = false },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NiagaraHomePage
// Tampilan home screen mode Niagara:
//   - Dock apps sebagai list besar di tengah layar
//   - Swipe ke atas → buka app drawer
//   - Long press background → settings
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NiagaraHomePage(
        dockApps: List<AppData>,
        darkMode: Boolean,
        overlayActive: Boolean,
        onAppPress: (String) -> Unit,
        onAppLong: (String) -> Unit,
        onBgLongPress: () -> Unit,
        onSwipeUp: () -> Unit,
) {
    val theme = LocalAppTheme.current
    var totalDragY by remember { mutableStateOf(0f) }

    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .pointerInput(overlayActive) {
                                if (overlayActive) return@pointerInput
                                detectVerticalDragGestures(
                                        onDragStart = { totalDragY = 0f },
                                        onDragCancel = { totalDragY = 0f },
                                        onDragEnd = {
                                            if (totalDragY < -120f) onSwipeUp()
                                            totalDragY = 0f
                                        },
                                        onVerticalDrag = { change, dragAmount ->
                                            totalDragY += dragAmount
                                            if (dragAmount < 0f) change.consume()
                                        },
                                )
                            }
                            .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {},
                                    onLongClick = { if (!overlayActive) onBgLongPress() },
                            ),
    ) {
        // ── Favorit apps di tengah ────────────────────────────────────────
        Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp).statusBarsPadding(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
        ) {
            if (dockApps.isEmpty()) {
                Text(
                        text = "Long press an app\nto add to favorites",
                        color = theme.textSecondary(),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Start,
                        lineHeight = 24.sp,
                )
            } else {
                dockApps.forEach { app ->
                    NiagaraFavoriteRow(
                            app = app,
                            darkMode = darkMode,
                            overlayActive = overlayActive,
                            onPress = onAppPress,
                            onLong = onAppLong,
                    )
                }
            }
        }

        // ── Hint bawah ─────────────────────────────────────────────────
        Column(
                modifier =
                        Modifier.align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("⌃", color = theme.textTertiary(), fontSize = 18.sp)
            Text(
                    text = "All apps",
                    color = theme.textTertiary(),
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NiagaraFavoriteRow — satu baris favorit di home screen
// Icon kecil + nama besar, gaya Niagara
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NiagaraFavoriteRow(
        app: AppData,
        darkMode: Boolean,
        overlayActive: Boolean,
        onPress: (String) -> Unit,
        onLong: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val theme = LocalAppTheme.current

    val alpha by
            animateFloatAsState(
                    targetValue = if (isPressed) 0.45f else 1f,
                    animationSpec = tween(70),
                    label = "favAlpha",
            )
    val offsetX by
            animateDpAsState(
                    targetValue = if (isPressed) 6.dp else 0.dp,
                    animationSpec =
                            spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                    label = "favOff",
            )

    val bitmap = remember(app.packageName) { iconCache.get(app.packageName) }

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .alpha(alpha)
                            .offset(x = offsetX)
                            .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { if (!overlayActive) onPress(app.packageName) },
                                    onLongClick = { if (!overlayActive) onLong(app.packageName) },
                            )
                            .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp))) {
            if (bitmap != null) {
                Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(
                                                if (darkMode) Color(0xFF3A3A3C)
                                                else Color(0xFFE5E5EA)
                                        )
                )
            }
        }

        Text(
                text = app.label,
                color = theme.fontColor(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NiagaraAppDrawer — full screen, slide dari bawah
// Daftar semua app dengan A-Z sidebar di kanan
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NiagaraAppDrawer(
        apps: List<AppData>,
        darkMode: Boolean,
        overlayActive: Boolean,
        onAppPress: (String) -> Unit,
        onAppLong: (String) -> Unit,
        onClose: () -> Unit,
) {
    val theme = LocalAppTheme.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Build flat list: DrawerItem.Header + DrawerItem.App
    val items by
            remember(apps) {
                derivedStateOf {
                    buildList {
                        val grouped =
                                apps.groupBy { app ->
                                    val c = app.label.firstOrNull()?.uppercaseChar() ?: '#'
                                    if (c in 'A'..'Z') c else '#'
                                }
                        val sortedKeys =
                                grouped.keys.sortedWith(
                                        compareBy { if (it == '#') '\uFFFF' else it }
                                )
                        sortedKeys.forEach { letter ->
                            add(DrawerItem.Header(letter))
                            grouped[letter]?.sortedBy { it.label.lowercase() }?.forEach {
                                add(DrawerItem.App(it))
                            }
                        }
                    }
                }
            }

    val letterIndex by
            remember(items) {
                derivedStateOf {
                    buildMap<Char, Int> {
                        items.forEachIndexed { i, item ->
                            if (item is DrawerItem.Header) put(item.letter, i)
                        }
                    }
                }
            }

    val letters = letterIndex.keys.toList()

    // Swipe-down saat di top → close
    var dragTotal by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize().background(theme.bgColor())) {
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                    state = listState,
                    modifier =
                            Modifier.weight(1f).fillMaxHeight().pointerInput(Unit) {
                                detectVerticalDragGestures(
                                        onDragStart = { dragTotal = 0f },
                                        onDragCancel = { dragTotal = 0f },
                                        onDragEnd = {
                                            if (dragTotal > 140f &&
                                                            listState.firstVisibleItemIndex == 0 &&
                                                            listState.firstVisibleItemScrollOffset <
                                                                    20
                                            ) {
                                                onClose()
                                            }
                                            dragTotal = 0f
                                        },
                                        onVerticalDrag = { _, amt -> dragTotal += amt },
                                )
                            },
                    contentPadding = PaddingValues(top = 20.dp, bottom = 80.dp),
            ) {
                items(
                        count = items.size,
                        key = {
                            when (val item = items[it]) {
                                is DrawerItem.Header -> "hdr_${item.letter}"
                                is DrawerItem.App -> item.app.packageName
                            }
                        },
                ) { idx ->
                    when (val item = items[idx]) {
                        is DrawerItem.Header -> {
                            Box(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .padding(
                                                            start = 24.dp,
                                                            top = 12.dp,
                                                            bottom = 2.dp
                                                    ),
                            ) {
                                Text(
                                        text = item.letter.toString(),
                                        color = theme.accentColor(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                )
                            }
                        }
                        is DrawerItem.App -> {
                            NiagaraDrawerRow(
                                    app = item.app,
                                    darkMode = darkMode,
                                    overlayActive = overlayActive,
                                    onPress = onAppPress,
                                    onLong = onAppLong,
                            )
                        }
                    }
                }
            }

            // A-Z sidebar
            AlphaScrollSidebar(
                    letters = letters,
                    onLetterSelected = { letter ->
                        letterIndex[letter]?.let { idx ->
                            scope.launch { listState.scrollToItem(idx) }
                        }
                    },
                    modifier =
                            Modifier.width(28.dp)
                                    .fillMaxHeight()
                                    .navigationBarsPadding()
                                    .padding(vertical = 12.dp),
            )
        }

        // Drag handle di atas
        Box(
                modifier =
                        Modifier.align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 6.dp)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(theme.textTertiary()),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DrawerItem
// ─────────────────────────────────────────────────────────────────────────────
private sealed class DrawerItem {
    data class Header(val letter: Char) : DrawerItem()
    data class App(val app: AppData) : DrawerItem()
}

// ─────────────────────────────────────────────────────────────────────────────
// NiagaraDrawerRow — satu baris di app drawer
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NiagaraDrawerRow(
        app: AppData,
        darkMode: Boolean,
        overlayActive: Boolean,
        onPress: (String) -> Unit,
        onLong: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val theme = LocalAppTheme.current

    val alpha by
            animateFloatAsState(
                    targetValue = if (isPressed) 0.4f else 1f,
                    animationSpec = tween(60),
                    label = "drawerAlpha",
            )

    val bitmap = remember(app.packageName) { iconCache.get(app.packageName) }

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .alpha(alpha)
                            .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { if (!overlayActive) onPress(app.packageName) },
                                    onLongClick = { if (!overlayActive) onLong(app.packageName) },
                            )
                            .padding(horizontal = 24.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))) {
            if (bitmap != null) {
                Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(
                                                if (darkMode) Color(0xFF3A3A3C)
                                                else Color(0xFFE5E5EA)
                                        )
                )
            }
        }

        Text(
                text = app.label,
                color = theme.fontColor(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AlphaScrollSidebar — A-Z di kanan drawer
// Touch/drag → jump ke huruf
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AlphaScrollSidebar(
        letters: List<Char>,
        onLetterSelected: (Char) -> Unit,
        modifier: Modifier = Modifier,
) {
    if (letters.isEmpty()) return

    val theme = LocalAppTheme.current
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var containerHeightPx by remember { mutableStateOf(1f) }

    fun letterAt(y: Float): Char {
        val frac = (y / containerHeightPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val idx = (frac * letters.size).toInt().coerceIn(0, letters.size - 1)
        return letters[idx]
    }

    Box(
            modifier =
                    modifier
                            .onGloballyPositioned { containerHeightPx = it.size.height.toFloat() }
                            // Tap: langsung pilih huruf saat jari menyentuh
                            .pointerInput(letters) {
                                detectTapGestures(
                                        onPress = { offset ->
                                            val letter = letterAt(offset.y)
                                            activeLetter = letter
                                            onLetterSelected(letter)
                                            tryAwaitRelease()
                                            activeLetter = null
                                        },
                                )
                            }
                            // Drag: update huruf saat jari geser
                            .pointerInput(letters) {
                                detectDragGestures(
                                        onDragStart = { offset ->
                                            val letter = letterAt(offset.y)
                                            activeLetter = letter
                                            onLetterSelected(letter)
                                        },
                                        onDrag = { change, _ ->
                                            val letter = letterAt(change.position.y)
                                            if (letter != activeLetter) {
                                                activeLetter = letter
                                                onLetterSelected(letter)
                                            }
                                        },
                                        onDragEnd = { activeLetter = null },
                                        onDragCancel = { activeLetter = null },
                                )
                            },
            contentAlignment = Alignment.Center,
    ) {
        Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEach { letter ->
                val isActive = letter == activeLetter
                Text(
                        text = letter.toString(),
                        color = if (isActive) theme.accentColor() else theme.textSecondary(),
                        fontSize = if (isActive) 12.sp else 9.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        lineHeight = 13.sp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IosPagedGrid — TIDAK BERUBAH (grid mode)
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
    val appPageCount by
            remember(apps.size, itemsPerPage) {
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
                    val to = minOf(from + itemsPerPage, apps.size)
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
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (idx < apps.size) {
                            IosAppIcon(
                                    app = apps[idx],
                                    showName = showNames,
                                    iconSizeDp = iconSize,
                                    onPress = { if (!overlayActive) onPress(it) },
                                    onLongPress = { if (!overlayActive) onLongPress(it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

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
    val scale by
            animateFloatAsState(
                    targetValue = if (isPressed) 0.80f else 1f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                    label = "iconScale",
            )
    val alpha by
            animateFloatAsState(
                    targetValue = if (isPressed) 0.70f else 1f,
                    animationSpec = tween(50),
                    label = "iconAlpha",
            )
    val bitmap = remember(app.packageName) { iconCache.get(app.packageName) }

    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                    Modifier.padding(horizontal = 3.dp, vertical = 5.dp)
                            .scale(scale)
                            .alpha(alpha)
                            .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onPress(app.packageName) },
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
                    modifier =
                            Modifier.size(iconSizeDp.dp)
                                    .clip(RoundedCornerShape((iconSizeDp * 0.22f).dp))
                                    .background(Color.White.copy(alpha = 0.12f))
            )
        }
        if (showName) {
            Spacer(Modifier.height(4.dp))
            Text(
                    text = app.label,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = SatriaColors.TextPrimary.copy(alpha = 0.90f),
                    modifier = Modifier.width((iconSizeDp + 14).dp),
                    style =
                            androidx.compose.ui.text.TextStyle(
                                    shadow = Shadow(Color.Black.copy(0.30f), Offset(0f, 1f), 3f),
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
    val activeColor = if (darkMode) Color.White else Color(0xFF1C1C1E)
    val inactiveColor = if (darkMode) Color(0x50FFFFFF) else Color(0x55000000)

    Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { i ->
            val active = i == currentPage
            val dotW by
                    animateDpAsState(
                            targetValue = if (active) 18.dp else 7.dp,
                            animationSpec =
                                    spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                            label = "dw$i",
                    )
            val dotColor by
                    animateColorAsState(
                            targetValue = if (active) activeColor else inactiveColor,
                            animationSpec = tween(180),
                            label = "dc$i",
                    )
            Box(modifier = Modifier.width(dotW).height(7.dp).clip(CircleShape).background(dotColor))
        }
    }
}
