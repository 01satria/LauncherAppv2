package id.satria.launcher.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import id.satria.launcher.MainViewModel
import id.satria.launcher.data.AppData
import id.satria.launcher.ui.component.*
import id.satria.launcher.ui.theme.SatriaColors
import id.satria.launcher.ui.theme.LocalAppTheme
import kotlin.math.ceil

// Berapa ikon per halaman (4 kolom x 5 baris = 20)
private const val COLS = 4
private const val ROWS = 5
private const val ITEMS_PER_PAGE = COLS * ROWS   // 20

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val filteredApps   by vm.filteredApps.collectAsState()
    val dockApps       by vm.dockApps.collectAsState()
    val layoutMode     by vm.layoutMode.collectAsState()
    val showNames      by vm.showNames.collectAsState()
    val avatarPath     by vm.avatarPath.collectAsState()
    val avatarVersion  by vm.avatarVersion.collectAsState()
    val iconSize       by vm.iconSize.collectAsState()
    val dockIconSize   by vm.dockIconSize.collectAsState()
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

        // -- App list / grid ------------------------------------------------
        if (layoutMode == "grid") {
            PagedGrid(
                apps          = filteredApps,
                showNames     = showNames,
                iconSize      = iconSize,
                overlayActive = overlayActive,
                onPress       = { if (!overlayActive) vm.launchApp(it) },
                onLongPress   = { if (!overlayActive) actionTarget = it },
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 16.dp, bottom = 148.dp),
                modifier       = Modifier.fillMaxSize(),
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppListItem(
                        app         = app,
                        showName    = showNames,
                        iconSizeDp  = iconSize,
                        onPress     = { if (!overlayActive) vm.launchApp(it) },
                        onLongPress = { if (!overlayActive) actionTarget = it },
                    )
                }
            }
        }

        // -- Brief pill — tap: dashboard, long press: settings ---------------
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 106.dp)
                .width(120.dp)
                .height(32.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = { if (!overlayActive) showDashboard = true },
                    onLongClick       = { if (!overlayActive) showSettings = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            val darkMode = LocalAppTheme.current.darkMode
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = if (darkMode) Color(0xEB000000)
                            else         Color(0xEBFFFFFF),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                )
            }
            // Warna solid kontras tinggi — putih di dark mode, hitam di light mode
            Text(
                text          = "✦  Brief",
                color         = if (LocalAppTheme.current.darkMode) Color.White else Color(0xFF1C1C1E),
                fontSize      = 12.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
        }

        // -- Dock -----------------------------------------------------------
        Dock(
            dockApps            = dockApps,
            avatarPath          = avatarPath,
            avatarVersion       = avatarVersion,
            dockIconSize        = dockIconSize,
            onAvatarClick       = { if (!overlayActive) showDashboard = true },
            onAppPress          = { if (!overlayActive) vm.launchApp(it) },
            onAppLongPress      = { if (!overlayActive) actionTarget = it },
            onLongPressSettings = { if (!overlayActive) showSettings = true },
            modifier            = Modifier.align(Alignment.BottomCenter),
        )

        // -- Dashboard ------------------------------------------------------
        AnimatedVisibility(
            visible = showDashboard,
            enter   = slideInVertically { it } + fadeIn(tween(250)),
            exit    = slideOutVertically { it } + fadeOut(tween(200)),
        ) {
            DashboardScreen(vm = vm, onClose = { showDashboard = false })
        }

        // -- Settings -------------------------------------------------------
        AnimatedVisibility(
            visible = showSettings,
            enter   = fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)),
            exit    = fadeOut(tween(180)) + scaleOut(targetScale = 0.92f, animationSpec = tween(180)),
        ) {
            SettingsSheet(vm = vm, onClose = { showSettings = false })
        }

        // -- App action sheet -----------------------------------------------
        AnimatedVisibility(
            visible = actionTarget != null,
            enter   = slideInVertically { it / 2 } + fadeIn(tween(200)),
            exit    = slideOutVertically { it / 2 } + fadeOut(tween(150)),
        ) {
            actionTarget?.let { pkg ->
                AppActionSheet(
                    pkg         = pkg,
                    label       = filteredApps.find { it.packageName == pkg }?.label
                                  ?: dockApps.find { it.packageName == pkg }?.label ?: pkg,
                    isHidden    = hiddenPackages.contains(pkg),
                    isDocked    = dockApps.any { it.packageName == pkg },
                    dockFull    = dockApps.size >= 5,
                    onClose     = { actionTarget = null },
                    onHide      = { vm.hideApp(pkg);      actionTarget = null },
                    onUnhide    = { vm.unhideApp(pkg);    actionTarget = null },
                    onDock      = { vm.toggleDock(pkg);   actionTarget = null },
                    onUninstall = { vm.uninstallApp(pkg); actionTarget = null },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PagedGrid — horizontal pager seperti iOS / One UI
//
// Strategi hemat RAM:
//   * beyondViewportPageCount = 0  → hanya halaman aktif yang di-compose
//   * Tiap halaman pakai Grid statis (Column+Row) bukan LazyVerticalGrid
//     sehingga tidak ada dua scroll-state bertumpuk
//   * subList() tanpa alokasi list baru (view ke list asli)
//   * PageIndicator pakai Canvas dots, nol Composable tambahan per dot
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagedGrid(
    apps          : List<AppData>,
    showNames     : Boolean,
    iconSize      : Int,
    overlayActive : Boolean,
    onPress       : (String) -> Unit,
    onLongPress   : (String) -> Unit,
) {
    if (apps.isEmpty()) return

    val pageCount  = remember(apps.size) {
        ceil(apps.size / ITEMS_PER_PAGE.toFloat()).toInt().coerceAtLeast(1)
    }
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Box(modifier = Modifier.fillMaxSize()) {

        HorizontalPager(
            state                   = pagerState,
            beyondViewportPageCount = 0,          // compose halaman aktif saja
            modifier                = Modifier
                .fillMaxSize()
                .padding(bottom = 148.dp),
            key                     = { it },
        ) { page ->
            val from     = page * ITEMS_PER_PAGE
            val to       = minOf(from + ITEMS_PER_PAGE, apps.size)
            val pageApps = apps.subList(from, to)  // view, bukan copy

            GridPage(
                apps          = pageApps,
                showNames     = showNames,
                iconSize      = iconSize,
                overlayActive = overlayActive,
                onPress       = onPress,
                onLongPress   = onLongPress,
            )
        }

        // Indicator halaman — muncul hanya jika lebih dari 1 halaman
        if (pageCount > 1) {
            PageIndicator(
                pageCount   = pageCount,
                currentPage = pagerState.currentPage,
                modifier    = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 150.dp),
            )
        }
    }
}

// Grid statis per halaman — Column + Row tanpa LazyGrid
@Composable
private fun GridPage(
    apps          : List<AppData>,
    showNames     : Boolean,
    iconSize      : Int,
    overlayActive : Boolean,
    onPress       : (String) -> Unit,
    onLongPress   : (String) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 16.dp)
    ) {
        val cellWidth  = maxWidth  / COLS
        val cellHeight = maxHeight / ROWS

        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until ROWS) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .height(cellHeight),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (col in 0 until COLS) {
                        val idx = row * COLS + col
                        if (idx < apps.size) {
                            Box(modifier = Modifier.size(cellWidth, cellHeight)) {
                                AppGridItem(
                                    app         = apps[idx],
                                    showName    = showNames,
                                    iconSizeDp  = iconSize,
                                    onPress     = { if (!overlayActive) onPress(it) },
                                    onLongPress = { if (!overlayActive) onLongPress(it) },
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(cellWidth, cellHeight))
                        }
                    }
                }
            }
        }
    }
}

// Dot indicator — minimal Canvas dots
@Composable
private fun PageIndicator(
    pageCount   : Int,
    currentPage : Int,
    modifier    : Modifier = Modifier,
) {
    val darkMode    = LocalAppTheme.current.darkMode
    val activeDot   = if (darkMode) Color.White       else Color(0xFF1C1C1E)
    val inactiveDot = if (darkMode) Color(0x55FFFFFF) else Color(0x55000000)

    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { i ->
            val active = i == currentPage
            Box(
                modifier = Modifier
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (active) activeDot else inactiveDot)
            )
        }
    }
}
