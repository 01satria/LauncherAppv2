package id.satria.launcher.ui.screen

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.satria.launcher.data.DEFAULT_DOCK_ICON_SIZE
import id.satria.launcher.data.DEFAULT_ICON_SIZE
import id.satria.launcher.data.MAX_DOCK_ICON_SIZE
import id.satria.launcher.data.MAX_ICON_SIZE
import id.satria.launcher.data.MIN_DOCK_ICON_SIZE
import id.satria.launcher.data.MIN_ICON_SIZE
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

    // Theme colors from VM
    val themeAccentHex by vm.themeAccent.collectAsState()
    val themeBgHex     by vm.themeBg.collectAsState()
    val themeBorderHex by vm.themeBorder.collectAsState()
    val themeFontHex   by vm.themeFont.collectAsState()

    // Local temp state
    var tempIconSize     by remember(iconSize)     { mutableStateOf(iconSize.toFloat()) }
    var tempDockIconSize by remember(dockIconSize) { mutableStateOf(dockIconSize.toFloat()) }
    var tempName         by remember(userName)      { mutableStateOf(userName) }
    var tempAssist       by remember(assistantName) { mutableStateOf(assistantName) }

    // Local color state — edit disini, save saat tombol Save
    var tempAccentHex by remember(themeAccentHex) { mutableStateOf(themeAccentHex) }
    var tempBgHex     by remember(themeBgHex)     { mutableStateOf(themeBgHex) }
    var tempBorderHex by remember(themeBorderHex) { mutableStateOf(themeBorderHex) }
    var tempFontHex   by remember(themeFontHex)   { mutableStateOf(themeFontHex) }

    var avatarKey by remember { mutableStateOf(0) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val rawBmp = if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            val size   = minOf(rawBmp.width, rawBmp.height)
            val xOff   = (rawBmp.width  - size) / 2
            val yOff   = (rawBmp.height - size) / 2
            val square = android.graphics.Bitmap.createBitmap(rawBmp, xOff, yOff, size, size)
            if (square !== rawBmp) rawBmp.recycle()
            val scaled = android.graphics.Bitmap.createScaledBitmap(square, 512, 512, true)
            if (scaled !== square) square.recycle()
            vm.saveAvatar(scaled)
            coil.Coil.imageLoader(context).memoryCache?.clear()
            avatarKey++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SatriaColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Settings", color = SatriaColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                // ── Avatar ─────────────────────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                            .background(SatriaColors.SurfaceMid)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatarPath != null) {
                            key(avatarKey) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(avatarPath)
                                        .diskCacheKey("avatar_$avatarKey").memoryCacheKey("avatar_$avatarKey")
                                        .crossfade(true).build(),
                                    contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                )
                            }
                        } else { Text("👤", fontSize = 36.sp) }
                    }
                }
                TextButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Change Photo", color = SatriaColors.TextSecondary, fontSize = 14.sp)
                }

                SettingsLabel("YOUR NAME")
                SettingsTextField(tempName, { tempName = it }, "User")

                SettingsLabel("ASSISTANT NAME")
                SettingsTextField(tempAssist, { tempAssist = it }, "Assistant")

                SettingsLabel("LAYOUT")
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SatriaColors.SurfaceMid)) {
                    listOf("grid" to "⊞  Grid", "list" to "☰  List").forEach { (mode, label) ->
                        val active = layoutMode == mode
                        TextButton(
                            onClick  = { vm.setLayoutMode(mode) },
                            modifier = Modifier.weight(1f).background(if (active) SatriaColors.SurfaceHigh else Color.Transparent),
                        ) {
                            Text(label, color = if (active) SatriaColors.TextPrimary else SatriaColors.TextSecondary,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }

                SettingsToggleRow("Show hidden apps", showHidden) { vm.setShowHidden(it) }
                SettingsToggleRow("Show app names",   showNames)  { vm.setShowNames(it) }

                SettingsLabel("APP ICON SIZE  (${tempIconSize.toInt()} dp)")
                Slider(
                    value = tempIconSize, onValueChange = { tempIconSize = it },
                    onValueChangeFinished = { vm.setIconSize(tempIconSize.toInt()) },
                    valueRange = MIN_ICON_SIZE.toFloat()..MAX_ICON_SIZE.toFloat(),
                    steps = (MAX_ICON_SIZE - MIN_ICON_SIZE) / 2 - 1,
                    colors = SliderDefaults.colors(thumbColor = SatriaColors.Accent, activeTrackColor = SatriaColors.Accent, inactiveTrackColor = SatriaColors.SurfaceMid),
                    modifier = Modifier.fillMaxWidth(),
                )

                SettingsLabel("DOCK ICON SIZE  (${tempDockIconSize.toInt()} dp)")
                Slider(
                    value = tempDockIconSize, onValueChange = { tempDockIconSize = it },
                    onValueChangeFinished = { vm.setDockIconSize(tempDockIconSize.toInt()) },
                    valueRange = MIN_DOCK_ICON_SIZE.toFloat()..MAX_DOCK_ICON_SIZE.toFloat(),
                    steps = (MAX_DOCK_ICON_SIZE - MIN_DOCK_ICON_SIZE) / 2 - 1,
                    colors = SliderDefaults.colors(thumbColor = SatriaColors.Accent, activeTrackColor = SatriaColors.Accent, inactiveTrackColor = SatriaColors.SurfaceMid),
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Theme Color Palette ────────────────────────────────────
                SettingsLabel("THEME PALETTE")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ColorPickerRow(
                        label       = "Accent",
                        description = "Buttons, toggles, progress",
                        hexValue    = tempAccentHex,
                        onChange    = { tempAccentHex = it },
                    )
                    ColorPickerRow(
                        label       = "Background",
                        description = "Dashboard, sheets",
                        hexValue    = tempBgHex,
                        onChange    = { tempBgHex = it },
                    )
                    ColorPickerRow(
                        label       = "Border",
                        description = "Dividers, outlines",
                        hexValue    = tempBorderHex,
                        onChange    = { tempBorderHex = it },
                    )
                    ColorPickerRow(
                        label       = "Font",
                        description = "All text colors",
                        hexValue    = tempFontHex,
                        onChange    = { tempFontHex = it },
                    )
                }

                // Reset theme
                TextButton(
                    onClick  = {
                        tempAccentHex = "FF27AE60"
                        tempBgHex     = "FF000000"
                        tempBorderHex = "FF1A1A1A"
                        tempFontHex   = "FFFFFFFF"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reset to Default", color = SatriaColors.TextTertiary, fontSize = 13.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = onClose,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = SatriaColors.TextSecondary),
                        border   = BorderStroke(1.dp, SatriaColors.SurfaceHigh),
                    ) { Text("Cancel") }
                    Button(
                        onClick  = {
                            vm.saveUserName(tempName)
                            vm.saveAssistantName(tempAssist)
                            vm.setThemeAccent(tempAccentHex)
                            vm.setThemeBg(tempBgHex)
                            vm.setThemeBorder(tempBorderHex)
                            vm.setThemeFont(tempFontHex)
                            onClose()
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = SatriaColors.Accent),
                    ) { Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

// ── Color picker row ─────────────────────────────────────────────────────────
// Simpel & ringan: swatch preview + hex TextField. No library tambahan.
@Composable
private fun ColorPickerRow(
    label       : String,
    description : String,
    hexValue    : String,
    onChange    : (String) -> Unit,
) {
    val color = hexToColor(hexValue, Color.Gray)
    var editing by remember { mutableStateOf(false) }
    var tempHex by remember(hexValue) { mutableStateOf(hexValue) }

    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Color swatch — tap untuk toggle edit
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .clickable { editing = !editing },
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = SatriaColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, color = SatriaColors.TextTertiary, fontSize = 11.sp)
        }

        // Compact hex input — 8 chars AARRGGBB
        if (editing) {
            TextField(
                value         = tempHex,
                onValueChange = { v ->
                    val clean = v.uppercase().filter { it.isLetterOrDigit() }.take(8)
                    tempHex = clean
                    if (clean.length == 8) onChange(clean)
                },
                modifier      = Modifier.width(110.dp),
                placeholder   = { Text("AARRGGBB", fontSize = 11.sp) },
                singleLine    = true,
                colors        = TextFieldDefaults.colors(
                    focusedContainerColor   = SatriaColors.SurfaceMid,
                    unfocusedContainerColor = SatriaColors.SurfaceMid,
                    focusedTextColor        = SatriaColors.TextPrimary,
                    unfocusedTextColor      = SatriaColors.TextPrimary,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = SatriaColors.Accent,
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            )
        }
    }
}

@Composable private fun SettingsLabel(text: String) {
    Text(text, color = SatriaColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
}

@Composable private fun SettingsTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    TextField(
        value = value, onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
        placeholder = { Text(placeholder, color = SatriaColors.TextTertiary) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor   = SatriaColors.SurfaceMid,
            unfocusedContainerColor = SatriaColors.SurfaceMid,
            focusedTextColor        = SatriaColors.TextPrimary,
            unfocusedTextColor      = SatriaColors.TextPrimary,
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor             = SatriaColors.Accent,
        ),
        singleLine = true,
    )
}

@Composable private fun SettingsToggleRow(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SatriaColors.TextPrimary, fontSize = 15.sp)
        Switch(
            checked = value, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Color.White, checkedTrackColor   = SatriaColors.Accent,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = SatriaColors.SurfaceMid,
            )
        )
    }
}
