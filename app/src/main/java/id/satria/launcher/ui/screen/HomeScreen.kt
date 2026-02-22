package id.satria.launcher.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import id.satria.launcher.MainViewModel
import id.satria.launcher.data.AppData
import id.satria.launcher.ui.component.*
import id.satria.launcher.ui.theme.SatriaColors

// ── Grid config ───────────────────────────────────────────────────────────────
private const val COLS = 4

@Composable
fun HomeScreen(vm: MainViewModel) {
    val filteredApps   by vm.filteredApps.collectAsState()
    val dockApps       by vm.dockApps.collectAsState()
    val showNames      by vm.showNames.collectAsState()
    val avatarPath     by vm.avatarPath.collectAsState()
    val avatarVersion  by vm.avatarVersion.collectAsState()
    val iconSize       by vm.iconSize.collectAsState()
    val dockIconSize   by vm.dockIconSize.collectAsState()
    val hiddenPackages by vm.hiddenPackages.collectAsState()
    val homeItems      by vm.homeItems.collectAsState()

    var showDashboard      by remember { mutableStateOf(false) }
    var showSettings       by remember { mutableStateOf(false) }
    var showWidgetPicker   by remember { mutableStateOf(false) }
    var actionTarget       by remember { mutableStateOf<String?>(null) }

    val overlayActive = showDashboard || showSettings || actionTarget != null || showWidgetPicker

    BackHandler(enabled = overlayActive) {
        when {
            actionTarget != null  -> actionTarget = null
            showWidgetPicker      -> showWidgetPicker = false
            showSettings          -> showSettings = false
            showDashboard         -> showDashboard = false
        }
    }

    // ── Drag state ─────────────────────────────────────────────────────────
    // We track a dragged item by index into `displayItems` (widgets + apps combined)
    var draggedIndex     by remember { mutableStateOf<Int?>(null) }
    var dragOffset       by remember { mutableStateOf(Offset.Zero) }
    var dragOrigin       by remember { mutableStateOf(Offset.Zero) }

    // Build unified display list: saved homeItems widgets first, then apps
    // For simplicity & performance: widgets are shown above the app grid
    val widgetItems = homeItems.filter { it.type != "app" }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Main scrollable content ────────────────────────────────────────
        LazyColumn(
            contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // ── Widgets ────────────────────────────────────────────────────
            if (widgetItems.isNotEmpty()) {
                item(key = "widgets_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        widgetItems.forEach { item ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                SystemWidget(type = item.type)
                                // Long press to remove widget
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = { if (!overlayActive) vm.removeWidget(item.id) }
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // ── App grid (Samsung-style 4-col) ─────────────────────────────
            item(key = "app_grid") {
                AppGridDraggable(
                    apps         = filteredApps,
                    showNames    = showNames,
                    iconSizeDp   = iconSize,
                    overlayActive = overlayActive,
                    onPress      = { if (!overlayActive) vm.launchApp(it) },
                    onLongPress  = { if (!overlayActive) actionTarget = it },
                )
            }
        }

        // ── Brief pill (replaces search) — center bottom above dock ────────
        BriefPill(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),   // above dock
            onClick  = { if (!overlayActive) showDashboard = true },
        )

        // ── Dock ───────────────────────────────────────────────────────────
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

        // ── Dashboard ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showDashboard,
            enter   = slideInVertically { it } + fadeIn(tween(250)),
            exit    = slideOutVertically { it } + fadeOut(tween(200)),
        ) {
            DashboardScreen(vm = vm, onClose = { showDashboard = false })
        }

        // ── Settings ───────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showSettings,
            enter   = fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)),
            exit    = fadeOut(tween(180)) + scaleOut(targetScale = 0.92f, animationSpec = tween(180)),
        ) {
            SettingsSheet(vm = vm, onClose = { showSettings = false })
        }

        // ── Widget Picker ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showWidgetPicker,
            enter   = slideInVertically { it / 2 } + fadeIn(tween(200)),
            exit    = slideOutVertically { it / 2 } + fadeOut(tween(150)),
        ) {
            WidgetPickerSheet(
                onClose     = { showWidgetPicker = false },
                onAddWidget = { type -> vm.addWidget(type); showWidgetPicker = false },
            )
        }

        // ── Action sheet ───────────────────────────────────────────────────
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
                    onHide      = { vm.hideApp(pkg);    actionTarget = null },
                    onUnhide    = { vm.unhideApp(pkg);  actionTarget = null },
                    onDock      = { vm.toggleDock(pkg); actionTarget = null },
                    onUninstall = { vm.uninstallApp(pkg); actionTarget = null },
                )
            }
        }

        // ── Widget add button (top right corner) ───────────────────────────
        if (!overlayActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .size(32.dp)
                    .background(Color(0xBB1C1C1E), RoundedCornerShape(16.dp))
                    .clickable { showWidgetPicker = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = SatriaColors.TextSecondary, fontSize = 20.sp, fontWeight = FontWeight.Light)
            }
        }

    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Brief Pill — replaces search pill, opens Dashboard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BriefPill(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .width(180.dp)
            .height(36.dp)
            .background(Color(0xCC1C1C1E), RoundedCornerShape(18.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = "✦  Brief",
            color      = SatriaColors.TextSecondary,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// App grid with drag & drop reorder
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridDraggable(
    apps: List<AppData>,
    showNames: Boolean,
    iconSizeDp: Int,
    overlayActive: Boolean,
    onPress: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    // Mutable reorder list — only for visual; persisted via vm on drop
    val orderedApps = remember(apps) { mutableStateListOf<AppData>().also { it.addAll(apps) } }

    LaunchedEffect(apps) {
        // Sync when external list changes (app installed/uninstalled)
        val currentPkgs = orderedApps.map { it.packageName }
        val newApps = apps.filter { it.packageName !in currentPkgs }
        val removedPkgs = currentPkgs.filter { pkg -> apps.none { it.packageName == pkg } }
        orderedApps.removeAll { it.packageName in removedPkgs }
        orderedApps.addAll(newApps)
    }

    var dragIndex     by remember { mutableStateOf<Int?>(null) }
    var dragOffsetPx  by remember { mutableStateOf(Offset.Zero) }
    val itemPositions = remember { mutableStateMapOf<Int, Offset>() }

    val cellSizeDp = iconSizeDp + 12
    val cols = COLS

    // We layout as a non-scrolling grid (lives inside LazyColumn item)
    val rows = (orderedApps.size + cols - 1) / cols

    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (col in 0 until cols) {
                        val idx = row * cols + col
                        if (idx < orderedApps.size) {
                            val app = orderedApps[idx]
                            val isDragging = dragIndex == idx

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { coords ->
                                        itemPositions[idx] = coords.positionInRoot()
                                    }
                                    .alpha(if (isDragging) 0f else 1f)
                                    .pointerInput(app.packageName, overlayActive) {
                                        if (overlayActive) return@pointerInput
                                        detectTapGestures(
                                            onTap       = { onPress(app.packageName) },
                                            onLongPress = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                dragIndex    = idx
                                                dragOffsetPx = Offset.Zero
                                                onLongPress(app.packageName)
                                            },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                AppGridItem(
                                    app         = app,
                                    showName    = showNames,
                                    iconSizeDp  = iconSizeDp,
                                    onPress     = {},
                                    onLongPress = {},
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // ── Floating dragged icon ──────────────────────────────────────────
        if (dragIndex != null) {
            val di = dragIndex!!
            if (di < orderedApps.size) {
                val app = orderedApps[di]
                Box(
                    modifier = Modifier
                        .offset {
                            val origin = itemPositions[di] ?: Offset.Zero
                            IntOffset(
                                (origin.x + dragOffsetPx.x).toInt(),
                                (origin.y + dragOffsetPx.y).toInt(),
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDrag = { change, delta ->
                                    change.consume()
                                    dragOffsetPx += delta
                                    // Find target cell
                                    val curPos = (itemPositions[di] ?: Offset.Zero) + dragOffsetPx
                                    val target = itemPositions.entries.minByOrNull { (_, pos) ->
                                        val dx = pos.x - curPos.x; val dy = pos.y - curPos.y
                                        dx * dx + dy * dy
                                    }?.key
                                    if (target != null && target != di && target < orderedApps.size) {
                                        val moved = orderedApps.removeAt(di)
                                        orderedApps.add(target, moved)
                                        dragIndex    = target
                                        dragOffsetPx = Offset.Zero
                                    }
                                },
                                onDragEnd    = { dragIndex = null; dragOffsetPx = Offset.Zero },
                                onDragCancel = { dragIndex = null; dragOffsetPx = Offset.Zero },
                            )
                        }
                        .scale(1.15f)
                        .alpha(0.92f),
                    contentAlignment = Alignment.Center,
                ) {
                    AppGridItem(
                        app         = app,
                        showName    = showNames,
                        iconSizeDp  = iconSizeDp,
                        onPress     = {},
                        onLongPress = {},
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Widget Picker Sheet
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WidgetPickerSheet(
    onClose: () -> Unit,
    onAddWidget: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SatriaColors.Surface, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .navigationBarsPadding()
                .clickable(
                    indication        = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    onClick           = {},
                )
                .padding(bottom = 12.dp),
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(36.dp).height(4.dp)
                    .background(SatriaColors.SurfaceHigh, RoundedCornerShape(2.dp))
            )

            Text(
                text       = "Add Widget",
                color      = SatriaColors.TextPrimary,
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            )

            AVAILABLE_WIDGETS.forEach { widget ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddWidget(widget.type) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(widget.icon, fontSize = 26.sp)
                    Column {
                        Text(widget.label, color = SatriaColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("System widget", color = SatriaColors.TextTertiary, fontSize = 12.sp)
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(SatriaColors.Border))
            }
        }
    }
}
