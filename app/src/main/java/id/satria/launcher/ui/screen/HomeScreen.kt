package id.satria.launcher.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import id.satria.launcher.MainViewModel
import id.satria.launcher.data.AppData
import id.satria.launcher.ui.component.*
import id.satria.launcher.ui.theme.LocalAppTheme
import id.satria.launcher.ui.theme.SatriaColors
import kotlin.math.absoluteValue
import kotlin.math.ceil

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen — iOS 26-style, tanpa liquid glass
//
// Navigasi gestur:
//   • Swipe kanan (halaman 1 grid) → Dashboard slide-in dari kiri
//   • Long-press background        → Settings modal
//   • Back                         → tutup overlay aktif
//
// RAM: beyondViewportPageCount=0, derivedStateOf, shared iconCache
// ─────────────────────────────────────────────────────────────────────────────
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
    val gridCols       by vm.gridCols.collectAsState()
    val gridRows       by vm.gridRows.collectAsState()
    val darkMode       by vm.darkMode.collectAsState()

    var showDashboard by remember { mutableStateOf(false) }
    var showSettings  by remember { mutableStateOf(false) }
    var actionTarget  by remember { mutableStateOf<String?>(null) }

    val overlayActive by remember { derivedStateOf { showDashboard || showSettings || actionTarget != null } }

    BackHandler(enabled = overlayActive) {
        when {
            actionTarget  != null -> actionTarget  = null
            showSettings          -> showSettings  = false
            showDashboard         -> showDashboard = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── App Content ─────────────────────────────────────────────────────
        HomeContent(
            filteredApps  = filteredApps,
            layoutMode    = layoutMode,
            showNames     = showNames,
            iconSize      = iconSize,
            gridCols      = gridCols,
            gridRows      = gridRows,
            darkMode      = darkMode,
            overlayActive = overlayActive,
            onAppPress    = { if (!overlayActive) vm.launchApp(it) },
            onAppLong     = { if (!overlayActive) actionTarget = it },
            onBgLongPress = { if (!overlayActive) showSettings = true },
            onSwipeRight  = { if (!overlayActive) showDashboard = true },
        )

        // ── Dock ────────────────────────────────────────────────────────────
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

        // ── Dashboard — slide dari kiri / keluar ke kiri ─────────────────
        AnimatedVisibility(
            visible = showDashboard,
            enter   = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec  = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness    = Spring.StiffnessMediumLow,
                ),
            ) + fadeIn(tween(180)),
            exit    = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(210, easing = FastOutSlowInEasing),
            ) + fadeOut(tween(160)),
        ) {
            DashboardScreen(vm = vm, onClose = { showDashboard = false })
        }

        // ── Settings ────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showSettings,
            enter   = fadeIn(tween(220)) + scaleIn(initialScale = 0.94f, animationSpec = tween(220)),
            exit    = fadeOut(tween(180)) + scaleOut(targetScale = 0.94f, animationSpec = tween(180)),
        ) {
            SettingsSheet(vm = vm, onClose = { showSettings = false })
        }

        // ── App Action Sheet ─────────────────────────────────────────────
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
// HomeContent
// Mendeteksi swipe-right via pointerInput terpisah (tidak konflik dengan pager)
// Long-press background via combinedClickable
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    filteredApps  : List<AppData>,
    layoutMode    : String,
    showNames     : Boolean,
    iconSize      : Int,
    gridCols      : Int,
    gridRows      : Int,
    darkMode      : Boolean,
    overlayActive : Boolean,
    onAppPress    : (String) -> Unit,
    onAppLong     : (String) -> Unit,
    onBgLongPress : () -> Unit,
    onSwipeRight  : () -> Unit,
) {
    val density        = LocalDensity.current
    val swipeThreshold = with(density) { 72.dp.toPx() }

    // Track apakah pager sedang di halaman pertama
    var isOnFirstPage by remember { mutableStateOf(true) }

    // State drag untuk deteksi swipe-right
    var dragStartX    by remember { mutableFloatStateOf(0f) }
    var dragStartY    by remember { mutableFloatStateOf(0f) }
    var swipeFired    by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Long press background = buka settings
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = {},
                onLongClick       = { if (!overlayActive) onBgLongPress() },
            )
            // Swipe-right detection — hanya pada halaman pertama grid
            .pointerInput(overlayActive, layoutMode) {
                if (overlayActive) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartX = offset.x
                        dragStartY = offset.y
                        swipeFired = false
                    },
                    onDrag = { change, _ ->
                        if (!swipeFired && (layoutMode != "grid" || isOnFirstPage)) {
                            val dx = change.position.x - dragStartX
                            val dy = (change.position.y - dragStartY).absoluteValue
                            // Horizontal dominan + arah kanan + melewati threshold
                            if (dx > swipeThreshold && dy < dx * 0.65f) {
                                swipeFired = true
                                onSwipeRight()
                            }
                        }
                    },
                    onDragEnd    = { swipeFired = false },
                    onDragCancel = { swipeFired = false },
                )
            },
    ) {
        if (layoutMode == "grid") {
            IosPagedGrid(
                apps          = filteredApps,
                showNames     = showNames,
                iconSize      = iconSize,
                cols          = gridCols,
                rows          = gridRows,
                overlayActive = overlayActive,
                onPress       = onAppPress,
                onLongPress   = onAppLong,
                onFirstPage   = { isOnFirstPage = it },
            )
        } else {
            IosListView(
                apps          = filteredApps,
                showNames     = showNames,
                iconSize      = iconSize,
                darkMode      = darkMode,
                overlayActive = overlayActive,
                onPress       = onAppPress,
                onLongPress   = onAppLong,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IosPagedGrid — HorizontalPager dengan parallax ringan
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosPagedGrid(
    apps          : List<AppData>,
    showNames     : Boolean,
    iconSize      : Int,
    cols          : Int,
    rows          : Int,
    overlayActive : Boolean,
    onPress       : (String) -> Unit,
    onLongPress   : (String) -> Unit,
    onFirstPage   : (Boolean) -> Unit,
) {
    if (apps.isEmpty()) return

    val itemsPerPage by remember(cols, rows) { derivedStateOf { cols * rows } }
    val pageCount    by remember(apps.size, itemsPerPage) {
        derivedStateOf { ceil(apps.size / itemsPerPage.toFloat()).toInt().coerceAtLeast(1) }
    }
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // Beritahu parent halaman mana yang aktif
    LaunchedEffect(pagerState.currentPage) {
        onFirstPage(pagerState.currentPage == 0)
    }

    val flingBehavior = PagerDefaults.flingBehavior(
        state             = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1),
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state                   = pagerState,
            beyondViewportPageCount = 0,           // hemat RAM maksimal
            flingBehavior           = flingBehavior,
            userScrollEnabled       = !overlayActive,
            modifier                = Modifier
                .fillMaxSize()
                .padding(bottom = 148.dp),
            key                     = { it },
        ) { page ->
            val from     = page * itemsPerPage
            val to       = minOf(from + itemsPerPage, apps.size)
            val pageApps = remember(apps, from, to) { apps.subList(from, to) }

            // Parallax offset — ringan, tidak mempengaruhi composable children
            val offsetFrac by remember(pagerState, page) {
                derivedStateOf { pagerState.getOffsetFractionForPage(page) }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offsetFrac * size.width * 0.06f
                        alpha        = 1f - offsetFrac.absoluteValue * 0.12f
                    },
            ) {
                IosGridPage(
                    apps          = pageApps,
                    showNames     = showNames,
                    iconSize      = iconSize,
                    cols          = cols,
                    rows          = rows,
                    overlayActive = overlayActive,
                    onPress       = onPress,
                    onLongPress   = onLongPress,
                )
            }
        }

        // Page indicator iOS — dot aktif melebar jadi pill
        if (pageCount > 1) {
            IosPageDots(
                pageCount   = pageCount,
                currentPage = pagerState.currentPage,
                modifier    = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 154.dp),
            )
        }
    }
}

// Grid page — icon merata dengan weight
@Composable
private fun IosGridPage(
    apps          : List<AppData>,
    showNames     : Boolean,
    iconSize      : Int,
    cols          : Int,
    rows          : Int,
    overlayActive : Boolean,
    onPress       : (String) -> Unit,
    onLongPress   : (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp, bottom = 12.dp),
    ) {
        for (row in 0 until rows) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                for (col in 0 until cols) {
                    val idx = row * cols + col
                    Box(
                        modifier         = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (idx < apps.size) {
                            IosAppIcon(
                                app         = apps[idx],
                                showName    = showNames,
                                iconSizeDp  = iconSize,
                                onPress     = { if (!overlayActive) onPress(it) },
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
// IosAppIcon — press-scale spring ala iOS, label dengan text shadow
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosAppIcon(
    app        : AppData,
    showName   : Boolean,
    iconSizeDp : Int,
    onPress    : (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.80f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "iconScale",
    )
    val alpha by animateFloatAsState(
        targetValue   = if (isPressed) 0.70f else 1f,
        animationSpec = tween(50),
        label         = "iconAlpha",
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
                indication        = null,
                onClick           = { onPress(app.packageName) },
                onLongClick       = { onLongPress(app.packageName) },
            ),
    ) {
        if (bitmap != null) {
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = app.label,
                contentScale       = ContentScale.Fit,
                filterQuality      = FilterQuality.Medium,
                modifier           = Modifier.size(iconSizeDp.dp),
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
                text      = app.label,
                fontSize  = 10.sp,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color     = SatriaColors.TextPrimary.copy(alpha = 0.90f),
                modifier  = Modifier.width((iconSizeDp + 14).dp),
                style     = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color      = Color.Black.copy(alpha = 0.30f),
                        offset     = Offset(0f, 1f),
                        blurRadius = 3f,
                    ),
                    fontSize = 10.sp,
                ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IosPageDots — dot aktif: pill melebar dengan animasi spring
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IosPageDots(
    pageCount   : Int,
    currentPage : Int,
    modifier    : Modifier = Modifier,
) {
    val darkMode      = LocalAppTheme.current.darkMode
    val activeColor   = if (darkMode) Color.White else Color(0xFF1C1C1E)
    val inactiveColor = if (darkMode) Color(0x50FFFFFF) else Color(0x55000000)

    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { i ->
            val active = i == currentPage

            val dotW by animateDpAsState(
                targetValue   = if (active) 18.dp else 7.dp,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label         = "dw$i",
            )
            val dotColor by animateColorAsState(
                targetValue   = if (active) activeColor else inactiveColor,
                animationSpec = tween(180),
                label         = "dc$i",
            )

            Box(
                modifier = Modifier
                    .width(dotW)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IosListView — alphabetical sticky-header list
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosListView(
    apps          : List<AppData>,
    showNames     : Boolean,
    iconSize      : Int,
    darkMode      : Boolean,
    overlayActive : Boolean,
    onPress       : (String) -> Unit,
    onLongPress   : (String) -> Unit,
) {
    val grouped by remember(apps) {
        derivedStateOf {
            apps.groupBy { app ->
                val ch = app.label.firstOrNull()?.uppercaseChar() ?: '#'
                if (ch.isLetter()) ch.toString() else "#"
            }.entries.sortedBy { it.key }
        }
    }

    val headerBg = if (darkMode) Color(0xCC000000) else Color(0xCCF2F2F7)

    LazyColumn(
        contentPadding = PaddingValues(top = 56.dp, bottom = 148.dp),
        modifier       = Modifier.fillMaxSize(),
    ) {
        grouped.forEach { (letter, groupApps) ->
            stickyHeader(key = "h_$letter") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    Text(
                        text          = letter,
                        color         = SatriaColors.TextTertiary,
                        fontSize      = 12.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.0.sp,
                    )
                }
            }
            items(groupApps, key = { it.packageName }) { app ->
                IosListRow(
                    app           = app,
                    showName      = showNames,
                    iconSizeDp    = iconSize,
                    darkMode      = darkMode,
                    overlayActive = overlayActive,
                    onPress       = onPress,
                    onLongPress   = onLongPress,
                )
            }
        }
    }
}

// List row — highlight saat di-press, separator mulai dari icon
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosListRow(
    app           : AppData,
    showName      : Boolean,
    iconSizeDp    : Int,
    darkMode      : Boolean,
    overlayActive : Boolean,
    onPress       : (String) -> Unit,
    onLongPress   : (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(
        targetValue   = if (isPressed)
            (if (darkMode) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
        else Color.Transparent,
        animationSpec = tween(80),
        label         = "listBg",
    )

    val bitmap = remember(app.packageName) { iconCache.get(app.packageName) }
    val startPad = 20 + iconSizeDp + 14

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = { if (!overlayActive) onPress(app.packageName) },
                onLongClick       = { if (!overlayActive) onLongPress(app.packageName) },
            ),
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 9.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(iconSizeDp.dp)
                    .clip(RoundedCornerShape((iconSizeDp * 0.22f).dp)),
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap             = bitmap.asImageBitmap(),
                        contentDescription = app.label,
                        contentScale       = ContentScale.Fit,
                        filterQuality      = FilterQuality.Medium,
                        modifier           = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(
                        if (darkMode) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
                    ))
                }
            }

            if (showName) {
                Text(
                    text       = app.label,
                    color      = SatriaColors.TextPrimary,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f),
                )
            }

            Text(
                text     = "›",
                color    = SatriaColors.TextTertiary.copy(alpha = 0.40f),
                fontSize = 20.sp,
            )
        }
        // Separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .padding(start = startPad.dp)
                .background(if (darkMode) Color(0xFF38383A) else Color(0xFFD1D1D6)),
        )
    }
}
