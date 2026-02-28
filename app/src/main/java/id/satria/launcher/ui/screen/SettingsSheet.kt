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
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// SettingsSheet — animated, smooth, lightweight
// Animation strategy:
//  • Staggered slide-up entrance per card (offset + alpha, no layout invalidation)
//  • Press-scale on cards via collectIsPressedAsState (runs on UI thread only)
//  • expand/shrink collapse uses clip — no expensive measure passes
//  • All colors resolved once at top level
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
            val sc   = android.graphics.Bitmap.createScaledBitmap(sq, 512, 512, true)
            if (sc !== sq) sq.recycle()
            vm.saveAvatar(sc)
            coil.Coil.imageLoader(context).memoryCache?.clear()
            avatarKey++
        }
    }

    // Resolve colors once at composition root — never inside loops
    val accent      = SatriaColors.Accent
    val surface     = SatriaColors.Surface
    val surfaceMid  = SatriaColors.SurfaceMid
    val surfaceHigh = SatriaColors.SurfaceHigh
    val textPrimary = SatriaColors.TextPrimary

    // ── Entrance animation state (staggered per item) ─────────────────────
    // Each card has its own animated float so they animate independently
    val cardCount = 5
    val enterProgress = remember { List(cardCount) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        enterProgress.forEachIndexed { i, anim ->
            delay(i * 55L) // 55ms stagger between cards
            anim.animateTo(
                targetValue   = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium,
                ),
            )
        }
    }

    // Top-bar header entrance (slides down from above)
    val headerAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        headerAnim.animateTo(1f, tween(280, easing = EaseOutCubic))
    }

    Box(modifier = Modifier.fillMaxSize().background(surface)) {
        // Static decorative gradient header wash
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.09f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar — slides down from top ────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = (1f - headerAnim.value) * -40f
                        alpha        = headerAnim.value
                    }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Back button with press-scale
                val backSource = remember { MutableInteractionSource() }
                val backPressed by backSource.collectIsPressedAsState()
                val backScale by animateFloatAsState(
                    targetValue   = if (backPressed) 0.88f else 1f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy),
                    label         = "backScale",
                )
                IconButton(
                    onClick           = onClose,
                    interactionSource = backSource,
                ) {
                    Text(
                        "←",
                        color    = textPrimary,
                        fontSize = 20.sp,
                        modifier = Modifier.scale(backScale),
                    )
                }

                Text(
                    "Settings",
                    color      = textPrimary,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f).padding(start = 4.dp),
                )

                // Save button with press-scale
                val saveSource = remember { MutableInteractionSource() }
                val savePressed by saveSource.collectIsPressedAsState()
                val saveScale by animateFloatAsState(
                    targetValue   = if (savePressed) 0.9f else 1f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy),
                    label         = "saveScale",
                )
                TextButton(
                    onClick           = { vm.saveUserName(tempName); vm.saveAssistantName(tempAssist); onClose() },
                    interactionSource = saveSource,
                ) {
                    Text(
                        "Save",
                        color      = accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp,
                        modifier   = Modifier.scale(saveScale),
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

                // Cards with staggered slide-up entrance
                // graphicsLayer does NOT trigger recompose — runs purely on render thread

                // Card 0: Assistant
                Box(
                    modifier = Modifier.graphicsLayer {
                        translationY = (1f - enterProgress[0].value) * 48f
                        alpha        = enterProgress[0].value
                    }
                ) {
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
                }

                // Card 1: Profile
                Box(
                    modifier = Modifier.graphicsLayer {
                        translationY = (1f - enterProgress[1].value) * 48f
                        alpha        = enterProgress[1].value
                    }
                ) {
                    AnimCard(title = "PROFILE", emoji = "👤", surfaceMid = surfaceMid, surfaceHigh = surfaceHigh, textPrimary = textPrimary, accent = accent) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel("Your name", accent)
                            SField(tempName, { tempName = it }, "User", surfaceHigh, textPrimary, accent)
                        }
                    }
                }

                // Card 2: Display
                Box(
                    modifier = Modifier.graphicsLayer {
                        translationY = (1f - enterProgress[2].value) * 48f
                        alpha        = enterProgress[2].value
                    }
                ) {
                    AnimCard(title = "DISPLAY", emoji = "🎨", surfaceMid = surfaceMid, surfaceHigh = surfaceHigh, textPrimary = textPrimary, accent = accent) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionLabel("Appearance", accent)
                            SegmentedControl(
                                options     = listOf("🌙  Dark" to true, "☀️  Light" to false),
                                selectedKey = darkMode,
                                onSelect    = { vm.setDarkMode(it) },
                                accent      = accent, surfaceHigh = surfaceHigh,
                            )
                            SectionLabel("Layout", accent)
                            SegmentedControl(
                                options     = listOf("⊞  Grid" to "grid", "☰  List" to "list"),
                                selectedKey = layoutMode,
                                onSelect    = { vm.setLayoutMode(it) },
                                accent      = accent, surfaceHigh = surfaceHigh,
                            )
                        }
                    }
                }

                // Card 3: Icons
                Box(
                    modifier = Modifier.graphicsLayer {
                        translationY = (1f - enterProgress[3].value) * 48f
                        alpha        = enterProgress[3].value
                    }
                ) {
                    AnimCard(title = "ICONS", emoji = "📱", surfaceMid = surfaceMid, surfaceHigh = surfaceHigh, textPrimary = textPrimary, accent = accent) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SToggleRow("Show app names",   showNames,  textPrimary, accent, surfaceHigh) { vm.setShowNames(it) }
                            SToggleRow("Show hidden apps", showHidden, textPrimary, accent, surfaceHigh) { vm.setShowHidden(it) }
                            SectionLabel("App icon size  (${tempIconSize.toInt()} dp)", accent)
                            PerfSlider(tempIconSize, { tempIconSize = it }, { vm.setIconSize(tempIconSize.toInt()) }, MIN_ICON_SIZE.toFloat()..MAX_ICON_SIZE.toFloat(), accent, surfaceHigh)
                            SectionLabel("Dock icon size  (${tempDockIconSize.toInt()} dp)", accent)
                            PerfSlider(tempDockIconSize, { tempDockIconSize = it }, { vm.setDockIconSize(tempDockIconSize.toInt()) }, MIN_DOCK_ICON_SIZE.toFloat()..MAX_DOCK_ICON_SIZE.toFloat(), accent, surfaceHigh)
                        }
                    }
                }

                // Card 4: Grid (only in grid mode)
                Box(
                    modifier = Modifier.graphicsLayer {
                        translationY = (1f - enterProgress[4].value) * 48f
                        alpha        = enterProgress[4].value
                    }
                ) {
                    AnimatedVisibility(
                        visible = layoutMode == "grid",
                        enter   = fadeIn(tween(180)) + expandVertically(tween(220, easing = EaseOutCubic)),
                        exit    = fadeOut(tween(140)) + shrinkVertically(tween(180, easing = EaseInCubic)),
                    ) {
                        AnimCard(title = "GRID", emoji = "⊞", surfaceMid = surfaceMid, surfaceHigh = surfaceHigh, textPrimary = textPrimary, accent = accent) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                SectionLabel("Columns  ($gridCols)", accent)
                                NumberPicker(MIN_GRID_COLS..MAX_GRID_COLS, gridCols, { vm.setGridCols(it) }, accent, surfaceHigh)
                                SectionLabel("Rows  ($gridRows)", accent)
                                NumberPicker(MIN_GRID_ROWS..MAX_GRID_ROWS, gridRows, { vm.setGridRows(it) }, accent, surfaceHigh)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AssistantCard — avatar + assistant name with tap-to-press feedback
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

    // Avatar press feedback
    val avatarSource  = remember { MutableInteractionSource() }
    val avatarPressed by avatarSource.collectIsPressedAsState()
    val avatarScale   by animateFloatAsState(
        targetValue   = if (avatarPressed) 0.93f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "avatarScale",
    )

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
            // Avatar
            Box(
                modifier         = Modifier.size(84.dp).scale(avatarScale),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .border(2.dp, accent.copy(alpha = 0.45f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(surfaceHigh)
                        .clickable(
                            interactionSource = avatarSource,
                            indication        = null,
                            onClick           = onPickAvatar,
                        ),
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
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable(onClick = onPickAvatar),
                    contentAlignment = Alignment.Center,
                ) { Text("📷", fontSize = 11.sp) }
            }

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
// AnimCard — collapsible card with press-scale + arrow rotation
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AnimCard(
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
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label         = "arrow",
    )

    // Header press-scale
    val headerSource  = remember { MutableInteractionSource() }
    val headerPressed by headerSource.collectIsPressedAsState()
    val headerScale   by animateFloatAsState(
        targetValue   = if (headerPressed) 0.97f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "cardScale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(headerScale)
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceMid),
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = headerSource,
                    indication        = null,
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

        // Collapsible content — AnimatedVisibility with expand+fade for natural feel
        AnimatedVisibility(
            visible = expanded,
            enter   = fadeIn(tween(160)) + expandVertically(tween(200, easing = EaseOutCubic)),
            exit    = fadeOut(tween(120)) + shrinkVertically(tween(160, easing = EaseInCubic)),
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                content  = content,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SegmentedControl — animated color sweep
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
                animationSpec = tween(200, easing = EaseInOutQuad),
                label         = "segBg",
            )
            val textColor by animateColorAsState(
                targetValue   = if (active) accent else accent.copy(alpha = 0.38f),
                animationSpec = tween(200),
                label         = "segTxt",
            )

            val src      = remember { MutableInteractionSource() }
            val pressed  by src.collectIsPressedAsState()
            val itemScale by animateFloatAsState(
                targetValue   = if (pressed) 0.94f else 1f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
                label         = "segScale",
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .scale(itemScale)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable(interactionSource = src, indication = null) { onSelect(key) }
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
// NumberPicker — color + scale animation on select
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
                animationSpec = tween(180, easing = EaseInOutQuad),
                label         = "numBg",
            )
            val txtColor by animateColorAsState(
                targetValue   = if (active) Color.White else accent.copy(alpha = 0.42f),
                animationSpec = tween(180),
                label         = "numTxt",
            )
            val numScale by animateFloatAsState(
                targetValue   = if (active) 1.07f else 1f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy),
                label         = "numScale",
            )

            val src = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .scale(numScale)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable(interactionSource = src, indication = null) { onSelect(v) }
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
// PerfSlider — minimal wrapper
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PerfSlider(
    value                : Float,
    onValueChange        : (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange           : ClosedFloatingPointRange<Float>,
    accent               : Color,
    surfaceHigh          : Color,
) = Slider(
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

// ─────────────────────────────────────────────────────────────────────────────
// SToggleRow — switch with smooth track color transition (built-in via Material3)
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
            checked         = value,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
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
private fun SectionLabel(text: String, accent: Color) = Text(
    text, color = accent.copy(alpha = 0.55f),
    fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp,
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
