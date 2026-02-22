package id.satria.launcher.ui.screen

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import id.satria.launcher.data.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import id.satria.launcher.MainViewModel
import id.satria.launcher.ui.theme.SatriaColors
import id.satria.launcher.ui.theme.colorToHex
import id.satria.launcher.ui.theme.hexToColor

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
    val themeAccentHex by vm.themeAccent.collectAsState()
    val themeBgHex     by vm.themeBg.collectAsState()
    val themeBorderHex by vm.themeBorder.collectAsState()
    val themeFontHex   by vm.themeFont.collectAsState()

    var tempIconSize     by remember(iconSize)     { mutableStateOf(iconSize.toFloat()) }
    var tempDockIconSize by remember(dockIconSize) { mutableStateOf(dockIconSize.toFloat()) }
    var tempName   by remember(userName)      { mutableStateOf(userName) }
    var tempAssist by remember(assistantName) { mutableStateOf(assistantName) }

    // Color state — Color objects for direct manipulation
    var tempAccent by remember(themeAccentHex) { mutableStateOf(hexToColor(themeAccentHex, Color(0xFF27AE60))) }
    var tempBg     by remember(themeBgHex)     { mutableStateOf(hexToColor(themeBgHex,     Color(0xFF000000))) }
    var tempBorder by remember(themeBorderHex) { mutableStateOf(hexToColor(themeBorderHex, Color(0xFF1A1A1A))) }
    var tempFont   by remember(themeFontHex)   { mutableStateOf(hexToColor(themeFontHex,   Color(0xFFFFFFFF))) }

    var avatarKey by remember { mutableStateOf(0) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val rawBmp = if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { dec, _, _ -> dec.isMutableRequired = true }
            } else {
                @Suppress("DEPRECATION") android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            val size = minOf(rawBmp.width, rawBmp.height)
            val sq   = android.graphics.Bitmap.createBitmap(rawBmp, (rawBmp.width-size)/2, (rawBmp.height-size)/2, size, size)
            if (sq !== rawBmp) rawBmp.recycle()
            val sc = android.graphics.Bitmap.createScaledBitmap(sq, 512, 512, true)
            if (sc !== sq) sq.recycle()
            vm.saveAvatar(sc)
            coil.Coil.imageLoader(context).memoryCache?.clear()
            avatarKey++
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(20.dp)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SatriaColors.Surface),
            elevation = CardDefaults.cardElevation(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(22.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Settings", color = SatriaColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                // Avatar
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(76.dp).clip(CircleShape).background(SatriaColors.SurfaceMid).clickable { imagePicker.launch("image/*") }, contentAlignment = Alignment.Center) {
                        if (avatarPath != null) key(avatarKey) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(avatarPath).diskCacheKey("avatar_$avatarKey").memoryCacheKey("avatar_$avatarKey").crossfade(true).build(),
                                contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                            )
                        } else Text("👤", fontSize = 34.sp)
                    }
                }
                TextButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Change Photo", color = SatriaColors.TextSecondary, fontSize = 13.sp)
                }

                SLabel("YOUR NAME");   SField(tempName,   { tempName   = it }, "User")
                SLabel("ASSISTANT NAME"); SField(tempAssist, { tempAssist = it }, "Assistant")

                SLabel("LAYOUT")
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SatriaColors.SurfaceMid)) {
                    listOf("grid" to "⊞  Grid", "list" to "☰  List").forEach { (mode, label) ->
                        val active = layoutMode == mode
                        TextButton(onClick = { vm.setLayoutMode(mode) }, modifier = Modifier.weight(1f).background(if (active) SatriaColors.SurfaceHigh else Color.Transparent)) {
                            Text(label, color = if (active) SatriaColors.TextPrimary else SatriaColors.TextSecondary, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }

                SToggle("Show hidden apps", showHidden) { vm.setShowHidden(it) }
                SToggle("Show app names",   showNames)  { vm.setShowNames(it) }

                SLabel("APP ICON SIZE  (${tempIconSize.toInt()} dp)")
                Slider(value = tempIconSize, onValueChange = { tempIconSize = it }, onValueChangeFinished = { vm.setIconSize(tempIconSize.toInt()) },
                    valueRange = MIN_ICON_SIZE.toFloat()..MAX_ICON_SIZE.toFloat(), steps = (MAX_ICON_SIZE - MIN_ICON_SIZE) / 2 - 1,
                    colors = SliderDefaults.colors(thumbColor = SatriaColors.Accent, activeTrackColor = SatriaColors.Accent, inactiveTrackColor = SatriaColors.SurfaceMid),
                    modifier = Modifier.fillMaxWidth())

                SLabel("DOCK ICON SIZE  (${tempDockIconSize.toInt()} dp)")
                Slider(value = tempDockIconSize, onValueChange = { tempDockIconSize = it }, onValueChangeFinished = { vm.setDockIconSize(tempDockIconSize.toInt()) },
                    valueRange = MIN_DOCK_ICON_SIZE.toFloat()..MAX_DOCK_ICON_SIZE.toFloat(), steps = (MAX_DOCK_ICON_SIZE - MIN_DOCK_ICON_SIZE) / 2 - 1,
                    colors = SliderDefaults.colors(thumbColor = SatriaColors.Accent, activeTrackColor = SatriaColors.Accent, inactiveTrackColor = SatriaColors.SurfaceMid),
                    modifier = Modifier.fillMaxWidth())

                // ── Theme palette ──────────────────────────────────────────
                SLabel("THEME PALETTE")
                ColorSelectorRow("Accent",     "Buttons · toggles",    tempAccent) { tempAccent = it }
                ColorSelectorRow("Background", "Dashboard · sheets",   tempBg)     { tempBg     = it }
                ColorSelectorRow("Border",     "Dividers · outlines",  tempBorder) { tempBorder = it }
                ColorSelectorRow("Font",       "All text",             tempFont)   { tempFont   = it }

                TextButton(onClick = {
                    tempAccent = Color(0xFF27AE60); tempBg = Color(0xFF000000)
                    tempBorder = Color(0xFF1A1A1A); tempFont = Color(0xFFFFFFFF)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Reset to Default", color = SatriaColors.TextTertiary, fontSize = 13.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SatriaColors.TextSecondary),
                        border = BorderStroke(1.dp, SatriaColors.SurfaceHigh)) { Text("Cancel") }
                    Button(onClick = {
                        vm.saveUserName(tempName); vm.saveAssistantName(tempAssist)
                        vm.setThemeAccent(colorToHex(tempAccent))
                        vm.setThemeBg    (colorToHex(tempBg))
                        vm.setThemeBorder(colorToHex(tempBorder))
                        vm.setThemeFont  (colorToHex(tempFont))
                        onClose()
                    }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.Accent)) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Simple Color Selector — hue strip + brightness strip + alpha strip + preview
// No external library, pure Canvas — very RAM-light
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ColorSelectorRow(
    label: String,
    desc: String,
    color: Color,
    onChange: (Color) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    // Decompose color to HSV for slider control
    val hsv = remember(color) {
        val arr = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (color.red   * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue  * 255).toInt(), arr)
        arr
    }
    var hue   by remember(color) { mutableFloatStateOf(hsv[0]) }
    var sat   by remember(color) { mutableFloatStateOf(hsv[1]) }
    var value by remember(color) { mutableFloatStateOf(hsv[2]) }
    var alpha by remember(color) { mutableFloatStateOf(color.alpha) }

    // Rebuild color whenever sliders change
    val currentColor = remember(hue, sat, value, alpha) {
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        Color(
            red   = android.graphics.Color.red(rgb)   / 255f,
            green = android.graphics.Color.green(rgb) / 255f,
            blue  = android.graphics.Color.blue(rgb)  / 255f,
            alpha = alpha,
        )
    }

    // Propagate to parent only when actually changed
    LaunchedEffect(currentColor) {
        if (currentColor != color) onChange(currentColor)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Row header — swatch + label + expand toggle
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Swatch
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                .background(currentColor)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = SatriaColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(desc,  color = SatriaColors.TextTertiary, fontSize = 11.sp)
            }
            Text(if (expanded) "▲" else "▼", color = SatriaColors.TextTertiary, fontSize = 11.sp)
        }

        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SatriaColors.SurfaceMid)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Hue strip
                SliderStrip(label = "H", value = hue / 360f,
                    brush = Brush.horizontalGradient(
                        (0..12).map { Color.hsv(it * 30f, 1f, 1f) }
                    )
                ) { hue = it * 360f }

                // Saturation strip
                SliderStrip(label = "S", value = sat,
                    brush = Brush.horizontalGradient(
                        listOf(Color.hsv(hue, 0f, value), Color.hsv(hue, 1f, value))
                    )
                ) { sat = it }

                // Brightness strip
                SliderStrip(label = "V", value = value,
                    brush = Brush.horizontalGradient(
                        listOf(Color.Black, Color.hsv(hue, sat, 1f))
                    )
                ) { value = it }

                // Alpha strip
                SliderStrip(label = "A", value = alpha,
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, currentColor.copy(alpha = 1f))
                    )
                ) { alpha = it }

                // Preset palette untuk quick pick
                Text("Presets", color = SatriaColors.TextTertiary, fontSize = 10.sp, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Color(0xFF27AE60), Color(0xFF2980B9), Color(0xFF8E44AD),
                        Color(0xFFE74C3C), Color(0xFFF39C12), Color(0xFFFFFFFF),
                        Color(0xFF8E8E93), Color(0xFF000000),
                    ).forEach { preset ->
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                            .background(preset)
                            .border(if (preset == currentColor) 2.dp else 0.5.dp,
                                if (preset == currentColor) Color.White else Color.White.copy(.2f), CircleShape)
                            .clickable {
                                val a = FloatArray(3)
                                android.graphics.Color.RGBToHSV((preset.red*255).toInt(), (preset.green*255).toInt(), (preset.blue*255).toInt(), a)
                                hue = a[0]; sat = a[1]; value = a[2]; alpha = preset.alpha
                            })
                    }
                }
            }
        }
    }
}

// Compact horizontal gradient strip with thumb
@Composable
private fun SliderStrip(label: String, value: Float, brush: Brush, onChange: (Float) -> Unit) {
    var width by remember { mutableIntStateOf(1) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = SatriaColors.TextTertiary, fontSize = 10.sp, modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f).height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .onSizeChanged { width = it.width }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onChange((change.position.x / width).coerceIn(0f, 1f))
                }
            }
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Checkerboard under alpha strip (lightweight)
                drawRoundRect(brush = brush, cornerRadius = CornerRadius(11.dp.toPx()))
                // Thumb
                val x = value * size.width
                drawCircle(color = Color.White, radius = 9.dp.toPx(), center = Offset(x, size.height / 2f))
                drawCircle(color = Color.Black.copy(alpha = .3f), radius = 9.dp.toPx(), center = Offset(x, size.height / 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            }
        }
    }
}

@Composable private fun SLabel(text: String) =
    Text(text, color = SatriaColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)

@Composable private fun SField(value: String, onChange: (String) -> Unit, placeholder: String) =
    TextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
        placeholder = { Text(placeholder, color = SatriaColors.TextTertiary) }, singleLine = true,
        colors = TextFieldDefaults.colors(focusedContainerColor = SatriaColors.SurfaceMid, unfocusedContainerColor = SatriaColors.SurfaceMid,
            focusedTextColor = SatriaColors.TextPrimary, unfocusedTextColor = SatriaColors.TextPrimary,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = SatriaColors.Accent))

@Composable private fun SToggle(label: String, value: Boolean, onToggle: (Boolean) -> Unit) =
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SatriaColors.TextPrimary, fontSize = 15.sp)
        Switch(checked = value, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SatriaColors.Accent, uncheckedThumbColor = Color.White, uncheckedTrackColor = SatriaColors.SurfaceMid))
    }
