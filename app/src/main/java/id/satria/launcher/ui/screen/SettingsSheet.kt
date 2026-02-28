package id.satria.launcher.ui.screen

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import id.satria.launcher.data.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import id.satria.launcher.MainViewModel
import id.satria.launcher.ui.theme.SatriaColors

// ─────────────────────────────────────────────────────────────────────────────
// Performance notes:
//  • NO InfiniteTransition at top level — avoids constant recomposition
//  • Static gradient background — drawn once, never redrawn
//  • AnimatedVisibility uses only fadeIn/fadeOut (cheap) — no expand layout pass
//  • animateColorAsState instead of animateFloatAsState for color interpolation
//  • remember(key){} used everywhere to avoid redundant allocations
//  • Slider: onValueChangeFinished only → DataStore only written on lift-off
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SettingsSheet(vm: MainViewModel, onClose: () -> Unit) {
    val context       = LocalContext.current
    val userName      by vm.userName.collectAsState()
    val assistantName by vm.assistantName.collectAsState()
    val showHidden    by vm.showHidden.collectAsState()
    val showNames     by vm.showNames.collectAsState()
    val layoutMode    by vm.layoutMode.collectAsState()
    val avatarPath    by vm.avatarPath.collectAsState()
    val iconSize      by vm.iconSize.collectAsState()
    val dockIconSize  by vm.dockIconSize.collectAsState()
    val darkMode      by vm.darkMode.collectAsState()
    val gridCols      by vm.gridCols.collectAsState()
    val gridRows      by vm.gridRows.collectAsState()

    var tempIconSize     by remember(iconSize)     { mutableStateOf(iconSize.toFloat()) }
    var tempDockIconSize by remember(dockIconSize) { mutableStateOf(dockIconSize.toFloat()) }
    var tempName         by remember(userName)      { mutableStateOf(userName) }
    var tempAssist       by remember(assistantName) { mutableStateOf(assistantName) }
    var avatarKey        by remember { mutableStateOf(0) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val rawBmp = if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, uri)
                ) { dec, _, _ -> dec.isMutableRequired = true }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            val size = minOf(rawBmp.width, rawBmp.height)
            val sq   = android.graphics.Bitmap.createBitmap(rawBmp, (rawBmp.width - size) / 2, (rawBmp.height - size) / 2, size, size)
            if (sq !== rawBmp) rawBmp.recycle()
            val sc = android.graphics.Bitmap.createScaledBitmap(sq, 512, 512, true)
            if (sc !== sq) sq.recycle()
            vm.saveAvatar(sc)
            coil.Coil.imageLoader(context).memoryCache?.clear()
            avatarKey++
        }
    }

    // Resolve colors ONCE at top level — not inside loops/lambdas
    val accent      = SatriaColors.Accent
    val surface     = SatriaColors.Surface
    val surfaceMid  = SatriaColors.SurfaceMid
    val surfaceHigh = SatriaColors.SurfaceHigh
    val textPrimary = SatriaColors.TextPrimary

    Box(modifier = Modifier.fillMaxSize().background(surface)) {
        // Static decorative gradient — drawn once, zero animation overhead
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.08f),
                            Color.Transparent,
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Text("←", color = textPrimary, fontSize = 20.sp)
                }
                Text(
                    "Settings",
                    color      = textPrimary,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f).padding(start = 4.dp),
                )
                TextButton(onClick = {
                    vm.saveUserName(tempName)
                    vm.saveAssistantName(tempAssist)
                    onClose()
                }) {
                    Text(
                        "Save",
                        color      = accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp,
                    )
                }
            }

            // ── Scrollable Content ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                // ── Assistant Card (avatar + name) ────────────────────────
                AssistantCard(
                    assistantName = tempAssist,
                    avatarPath    = avatarPath,
                    avatarKey     = avatarKey,
                    onChangeName  = { tempAssist = it },
                    onPickAvatar  = { imagePicker.launch("image/*") },
                    accent        = accent,
                    surfaceMid    = surfaceMid,
                    surfaceHigh   = surfaceHigh,
                    textPrimary   = textPrimary,
                )

                // ── Profile Card (user name only) ─────────────────────────
                SettingsCard(title = "PROFILE", emoji = "👤", surfaceMid = surfaceMid, surfaceHigh = surfaceHigh, textPrimary = textPrimary, accent = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel("Your name", accent)
                        SField(tempName, { tempName = it }, "User", surfaceHigh, textPrimary, accent)
                    }
                }

                // ── Display Card ──────────────────────────────────────────
                SettingsCard(title = "DISPLAY", emoji = "🎨", surfaceMid = surfaceMid, surfaceHigh = surfaceHigh, textPrimary = textPrimary, accent = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SectionLabel("Appearance", accent)
                        SegmentedControl(
                            options     = listOf("🌙  Dark" to true, "☀️  Light" to false),
                            selectedKey = darkMode,
                            onSelect    = { vm.setDarkMode(it) },
                            accent      = accent,
                            surfaceHigh = surfaceHigh,
                        )
                        SectionLabel("Layout", accent)
                        SegmentedControl(
                            options     = listOf("⊞  Grid" to "grid", "☰  List" to "list"),
                            selectedKey = layoutMode,
                            onSelect    = { vm.setLayoutMode(it) },
                            accent      = accent,
                            surfaceHigh = surfaceHigh,
                        )
                    }
                }

                // ── Icons Card ────────────────────────────────────────────
                SettingsCard(title = "ICONS", emoji = "📱", surfaceMid = surfaceMid, surfaceHigh = surfaceHigh, textPrimary = textPrimary, accent = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SToggleRow("Show app names",  showNames,  textPrimary, accent, surfaceHigh) { vm.setShowNames(it) }
                        SToggleRow("Show hidden apps", showHidden, textPrimary, accent, surfaceHigh) { vm.setShowHidden(it) }
                        SectionLabel("App icon size  (${tempIconSize.toInt()} dp)", accent)
                        PerfSlider(
                            value                 = tempIconSize,
                            onValueChange         = { tempIconSize = it },
                            onValueChangeFinished = { vm.setIconSize(tempIconSize.toInt()) },
                            valueRange            = MIN_ICON_SIZE.toFloat()..MAX_ICON_SIZE.toFloat(),
                            accent                = accent,
                            surfaceHigh           = surfaceHigh,
                        )
                        SectionLabel("Dock icon size  (${tempDockIconSize.toInt()} dp)", accent)
                        PerfSlider(
                            value                 = tempDockIconSize,
                            onValueChange         = { tempDockIconSize = it },
                            onValueChangeFinished = { vm.setDockIconSize(tempDockIconSize.toInt()) },
                            valueRange            = MIN_DOCK_ICON_SIZE.toFloat()..MAX_DOCK_ICON_SIZE.toFloat(),
                            accent                = accent,
                            surfaceHigh           = surfaceHigh,
                        )
                    }
                }

                // ── Grid Card (visible only in grid mode) ─────────────────
                // Using crossfade + fast tween — no expand layout thrashing
                AnimatedVisibility(
                    visible = layoutMode == "grid",
                    enter   = fadeIn(tween(160)),
                    exit    = fadeOut(tween(120)),
                ) {
                    SettingsCard(title = "GRID", emoji = "⊞", surfaceMid = surfaceMid, surfaceHigh = surfaceHigh, textPrimary = textPrimary, accent = accent) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionLabel("Columns  ($gridCols)", accent)
                            NumberPicker(
                                range       = MIN_GRID_COLS..MAX_GRID_COLS,
                                selected    = gridCols,
                                onSelect    = { vm.setGridCols(it) },
                                accent      = accent,
                                surfaceHigh = surfaceHigh,
                            )
                            SectionLabel("Rows  ($gridRows)", accent)
                            NumberPicker(
                                range       = MIN_GRID_ROWS..MAX_GRID_ROWS,
                                selected    = gridRows,
                                onSelect    = { vm.setGridRows(it) },
                                accent      = accent,
                                surfaceHigh = surfaceHigh,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AssistantCard — avatar + assistant name (correctly labelled)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AssistantCard(
    assistantName : String,
    avatarPath    : String?,
    avatarKey     : Int,
    onChangeName  : (String) -> Unit,
    onPickAvatar  : () -> Unit,
    accent        : Color,
    surfaceMid    : Color,
    surfaceHigh   : Color,
    textPrimary   : Color,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceMid)
            .padding(20.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Avatar + camera badge — no animation = zero overhead
            Box(
                modifier         = Modifier.size(84.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Accent ring
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .border(2.dp, accent.copy(alpha = 0.5f), CircleShape)
                )
                // Avatar
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(surfaceHigh)
                        .clickable(onClick = onPickAvatar),
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatarPath != null) {
                        key(avatarKey) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(avatarPath)
                                    .diskCacheKey("avatar_$avatarKey")
                                    .memoryCacheKey("avatar_$avatarKey")
                                    .crossfade(200)
                                    .build(),
                                contentDescription = null,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize().clip(CircleShape),
                            )
                        }
                    } else {
                        Text("🤖", fontSize = 36.sp)
                    }
                }
                // Camera badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable(onClick = onPickAvatar),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("📷", fontSize = 11.sp)
                }
            }

            // Assistant name field
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectionLabel("Assistant name", accent)
                SField(assistantName, onChangeName, "Assistant", surfaceHigh, textPrimary, accent)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SettingsCard — collapsible section card
// Colors passed in to avoid @Composable reads inside animation lambdas
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsCard(
    title      : String,
    emoji      : String,
    surfaceMid : Color,
    surfaceHigh: Color,
    textPrimary: Color,
    accent     : Color,
    content    : @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    val arrowRotation by animateFloatAsState(
        targetValue   = if (expanded) 0f else -90f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label         = "arrow",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceMid),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null, // no ripple = no extra draw pass
                ) { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaceHigh),
                    contentAlignment = Alignment.Center,
                ) { Text(emoji, fontSize = 16.sp) }
                Text(
                    title,
                    color         = accent.copy(alpha = 0.7f),
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
            }
            Text(
                "▾",
                color    = textPrimary.copy(alpha = 0.35f),
                fontSize = 14.sp,
                modifier = Modifier.rotate(arrowRotation),
            )
        }

        // Content — fade only (no layout expand/shrink = no measure pass per frame)
        AnimatedVisibility(
            visible = expanded,
            enter   = fadeIn(tween(160)),
            exit    = fadeOut(tween(120)),
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                content  = content,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SegmentedControl
// Uses animateColorAsState — single color lerp per segment, no alpha math
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun <T> SegmentedControl(
    options     : List<Pair<String, T>>,
    selectedKey : T,
    onSelect    : (T) -> Unit,
    accent      : Color,
    surfaceHigh : Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceHigh)
            .padding(3.dp),
    ) {
        options.forEach { (label, key) ->
            val active = selectedKey == key
            val bgColor by animateColorAsState(
                targetValue   = if (active) accent.copy(alpha = 0.18f) else Color.Transparent,
                animationSpec = tween(180),
                label         = "segColor",
            )
            val textColor by animateColorAsState(
                targetValue   = if (active) accent else accent.copy(alpha = 0.4f),
                animationSpec = tween(180),
                label         = "segText",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) { onSelect(key) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color      = textColor,
                    fontSize   = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NumberPicker — instant color swap, no scale animation (lighter)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NumberPicker(
    range       : IntRange,
    selected    : Int,
    onSelect    : (Int) -> Unit,
    accent      : Color,
    surfaceHigh : Color,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        range.forEach { v ->
            val active = v == selected
            val bgColor by animateColorAsState(
                targetValue   = if (active) accent else surfaceHigh,
                animationSpec = tween(160),
                label         = "numBg",
            )
            val txtColor by animateColorAsState(
                targetValue   = if (active) Color.White else accent.copy(alpha = 0.45f),
                animationSpec = tween(160),
                label         = "numTxt",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) { onSelect(v) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$v",
                    color      = txtColor,
                    fontSize   = 14.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PerfSlider — no extra wrapper, direct Slider (minimizes composition depth)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PerfSlider(
    value                : Float,
    onValueChange        : (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange           : ClosedFloatingPointRange<Float>,
    accent               : Color,
    surfaceHigh          : Color,
) {
    Slider(
        value                 = value,
        onValueChange         = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange            = valueRange,
        colors                = SliderDefaults.colors(
            thumbColor         = accent,
            activeTrackColor   = accent,
            inactiveTrackColor = surfaceHigh,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SToggleRow
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SToggleRow(
    label       : String,
    value       : Boolean,
    textPrimary : Color,
    accent      : Color,
    surfaceHigh : Color,
    onToggle    : (Boolean) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(label, color = textPrimary, fontSize = 15.sp)
        Switch(
            checked        = value,
            onCheckedChange = onToggle,
            colors         = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = surfaceHigh,
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String, accent: Color) =
    Text(
        text,
        color         = accent.copy(alpha = 0.55f),
        fontSize      = 12.sp,
        fontWeight    = FontWeight.Medium,
        letterSpacing = 0.3.sp,
    )

@Composable
private fun SField(
    value       : String,
    onChange    : (String) -> Unit,
    placeholder : String,
    surfaceHigh : Color,
    textPrimary : Color,
    accent      : Color,
) = TextField(
    value          = value,
    onValueChange  = onChange,
    modifier       = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
    placeholder    = { Text(placeholder, color = textPrimary.copy(alpha = 0.3f)) },
    singleLine     = true,
    colors         = TextFieldDefaults.colors(
        focusedContainerColor   = surfaceHigh,
        unfocusedContainerColor = surfaceHigh,
        focusedTextColor        = textPrimary,
        unfocusedTextColor      = textPrimary,
        focusedIndicatorColor   = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        cursorColor             = accent,
    ),
)
