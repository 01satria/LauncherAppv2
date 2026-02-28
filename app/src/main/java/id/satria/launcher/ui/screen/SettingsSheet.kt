package id.satria.launcher.ui.screen

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
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
// SettingsSheet — redesigned with animations, clean sections, premium look
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
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { dec, _, _ -> dec.isMutableRequired = true }
            } else {
                @Suppress("DEPRECATION") android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
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

    // Entry animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SatriaColors.Surface),
    ) {
        // Subtle animated gradient header glow
        val infiniteTransition = rememberInfiniteTransition(label = "bgGlow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.06f, targetValue = 0.13f,
            animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
            label = "glowAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SatriaColors.Accent.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(Float.MAX_VALUE / 2f, 0f),
                        radius = 600f,
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
                    Text("←", color = SatriaColors.TextPrimary, fontSize = 20.sp)
                }
                Text(
                    "Settings",
                    color = SatriaColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                TextButton(onClick = {
                    vm.saveUserName(tempName); vm.saveAssistantName(tempAssist); onClose()
                }) {
                    Text("Save", color = SatriaColors.Accent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            // ── Scrollable content ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                // ── Profile Card ──────────────────────────────────────────
                ProfileCard(
                    userName    = tempName,
                    avatarPath  = avatarPath,
                    avatarKey   = avatarKey,
                    onChangeName = { tempName = it },
                    onPickAvatar = { imagePicker.launch("image/*") },
                )

                // ── Assistant Card ────────────────────────────────────────
                SettingsCard(title = "ASSISTANT", emoji = "🤖") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel("Name")
                        SField(tempAssist, { tempAssist = it }, "Assistant")
                    }
                }

                // ── Display Card ──────────────────────────────────────────
                SettingsCard(title = "DISPLAY", emoji = "🎨") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Appearance toggle
                        SectionLabel("Appearance")
                        SegmentedControl(
                            options = listOf("🌙  Dark" to true, "☀️  Light" to false),
                            selectedKey = darkMode,
                            onSelect = { vm.setDarkMode(it) },
                        )

                        // Layout toggle
                        SectionLabel("Layout")
                        SegmentedControl(
                            options = listOf("⊞  Grid" to "grid", "☰  List" to "list"),
                            selectedKey = layoutMode,
                            onSelect = { vm.setLayoutMode(it) },
                        )
                    }
                }

                // ── Icons Card ────────────────────────────────────────────
                SettingsCard(title = "ICONS", emoji = "📱") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SToggleRow("Show app names", showNames) { vm.setShowNames(it) }
                        SToggleRow("Show hidden apps", showHidden) { vm.setShowHidden(it) }

                        SectionLabel("App icon size  (${tempIconSize.toInt()} dp)")
                        AnimatedSlider(
                            value = tempIconSize,
                            onValueChange = { tempIconSize = it },
                            onValueChangeFinished = { vm.setIconSize(tempIconSize.toInt()) },
                            valueRange = MIN_ICON_SIZE.toFloat()..MAX_ICON_SIZE.toFloat(),
                        )

                        SectionLabel("Dock icon size  (${tempDockIconSize.toInt()} dp)")
                        AnimatedSlider(
                            value = tempDockIconSize,
                            onValueChange = { tempDockIconSize = it },
                            onValueChangeFinished = { vm.setDockIconSize(tempDockIconSize.toInt()) },
                            valueRange = MIN_DOCK_ICON_SIZE.toFloat()..MAX_DOCK_ICON_SIZE.toFloat(),
                        )
                    }
                }

                // ── Grid Card (only in grid mode) ─────────────────────────
                AnimatedVisibility(
                    visible = layoutMode == "grid",
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically(),
                ) {
                    SettingsCard(title = "GRID", emoji = "⊞") {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionLabel("Columns  ($gridCols)")
                            NumberPicker(
                                range    = MIN_GRID_COLS..MAX_GRID_COLS,
                                selected = gridCols,
                                onSelect = { vm.setGridCols(it) },
                            )
                            SectionLabel("Rows  ($gridRows)")
                            NumberPicker(
                                range    = MIN_GRID_ROWS..MAX_GRID_ROWS,
                                selected = gridRows,
                                onSelect = { vm.setGridRows(it) },
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
// ProfileCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileCard(
    userName: String,
    avatarPath: String?,
    avatarKey: Int,
    onChangeName: (String) -> Unit,
    onPickAvatar: () -> Unit,
) {
    val context = LocalContext.current

    // Pulse animation on avatar
    val pulseAnim = rememberInfiniteTransition(label = "avatarPulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SatriaColors.SurfaceMid)
            .padding(20.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Avatar with ring
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center,
            ) {
                // Glowing ring
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .border(2.dp, SatriaColors.Accent.copy(alpha = 0.6f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(SatriaColors.SurfaceHigh)
                        .clickable { onPickAvatar() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatarPath != null) {
                        key(avatarKey) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(avatarPath)
                                    .diskCacheKey("avatar_$avatarKey")
                                    .memoryCacheKey("avatar_$avatarKey")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                            )
                        }
                    } else {
                        Text("👤", fontSize = 38.sp)
                    }
                }
                // Camera badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(SatriaColors.Accent)
                        .clickable { onPickAvatar() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("📷", fontSize = 12.sp)
                }
            }

            // Name field — inline styled
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SectionLabel("Your name")
                SField(userName, onChangeName, "User")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SettingsCard — generic card container with animated expand
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsCard(
    title: String,
    emoji: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    // Animated arrow rotation
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "arrow",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SatriaColors.SurfaceMid),
    ) {
        // Header row (tap to collapse)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Emoji badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SatriaColors.SurfaceHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, fontSize = 16.sp)
                }
                Text(
                    title,
                    color = SatriaColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
            }
            Text(
                "▾",
                color = SatriaColors.TextTertiary,
                fontSize = 14.sp,
                modifier = Modifier.rotate(arrowRotation),
            )
        }

        // Animated content
        AnimatedVisibility(
            visible = expanded,
            enter   = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit    = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, bottom = 16.dp),
                content = content,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SegmentedControl — animated pill selector
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun <T> SegmentedControl(
    options: List<Pair<String, T>>,
    selectedKey: T,
    onSelect: (T) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SatriaColors.SurfaceHigh)
            .padding(3.dp),
    ) {
        Row {
            options.forEach { (label, key) ->
                val active = selectedKey == key
                val bgAlpha by animateFloatAsState(
                    targetValue = if (active) 1f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "segBg",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SatriaColors.Accent.copy(alpha = bgAlpha * 0.15f))
                        .border(
                            width = if (active) 1.dp else 0.dp,
                            color = if (active) SatriaColors.Accent.copy(alpha = 0.4f) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                        )
                        .clickable { onSelect(key) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (active) SatriaColors.Accent else SatriaColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NumberPicker — animated pill buttons
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NumberPicker(range: IntRange, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        range.forEach { v ->
            val active = v == selected
            val scale by animateFloatAsState(
                targetValue = if (active) 1.05f else 1f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy),
                label = "numScale",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) SatriaColors.Accent else SatriaColors.SurfaceHigh)
                    .clickable { onSelect(v) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$v",
                    color = if (active) Color.White else SatriaColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AnimatedSlider — Accent-colored slider
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AnimatedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor         = SatriaColors.Accent,
            activeTrackColor   = SatriaColors.Accent,
            inactiveTrackColor = SatriaColors.SurfaceHigh,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SToggleRow
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SToggleRow(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = SatriaColors.TextPrimary, fontSize = 15.sp)
        Switch(
            checked = value,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = SatriaColors.Accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = SatriaColors.SurfaceHigh,
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small helpers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) =
    Text(
        text,
        color = SatriaColors.TextTertiary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
    )

@Composable
private fun SField(value: String, onChange: (String) -> Unit, placeholder: String) =
    TextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        placeholder = { Text(placeholder, color = SatriaColors.TextTertiary) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor   = SatriaColors.SurfaceHigh,
            unfocusedContainerColor = SatriaColors.SurfaceHigh,
            focusedTextColor        = SatriaColors.TextPrimary,
            unfocusedTextColor      = SatriaColors.TextPrimary,
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor             = SatriaColors.Accent,
        ),
    )
