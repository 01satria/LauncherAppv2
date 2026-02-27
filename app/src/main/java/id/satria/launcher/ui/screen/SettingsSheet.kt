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
    val darkMode          by vm.darkMode.collectAsState()
    val recentAppsEnabled by vm.recentAppsEnabled.collectAsState()
    val gridCols      by vm.gridCols.collectAsState()
    val gridRows      by vm.gridRows.collectAsState()

    var tempIconSize     by remember(iconSize)     { mutableStateOf(iconSize.toFloat()) }
    var tempDockIconSize by remember(dockIconSize) { mutableStateOf(dockIconSize.toFloat()) }
    var tempName   by remember(userName)      { mutableStateOf(userName) }
    var tempAssist by remember(assistantName) { mutableStateOf(assistantName) }

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

                // ── Grid layout selector (hanya muncul di mode grid) ──────
                if (layoutMode == "grid") {
                    SLabel("GRID COLUMNS  ($gridCols)")
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        (MIN_GRID_COLS..MAX_GRID_COLS).forEach { v ->
                            val active = gridCols == v
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) SatriaColors.Accent else SatriaColors.SurfaceMid)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = null,
                                    ) { vm.setGridCols(v) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$v",
                                    color      = if (active) androidx.compose.ui.graphics.Color.White else SatriaColors.TextSecondary,
                                    fontSize   = 14.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }

                    SLabel("GRID ROWS  ($gridRows)")
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        (MIN_GRID_ROWS..MAX_GRID_ROWS).forEach { v ->
                            val active = gridRows == v
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) SatriaColors.Accent else SatriaColors.SurfaceMid)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = null,
                                    ) { vm.setGridRows(v) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$v",
                                    color      = if (active) androidx.compose.ui.graphics.Color.White else SatriaColors.TextSecondary,
                                    fontSize   = 14.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }

                // ── Theme Mode ─────────────────────────────────────────────
                SLabel("APPEARANCE")
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SatriaColors.SurfaceMid)) {
                    listOf(true to "🌙  Dark", false to "☀️  Light").forEach { (isDark, label) ->
                        val active = darkMode == isDark
                        TextButton(
                            onClick = { vm.setDarkMode(isDark) },
                            modifier = Modifier.weight(1f).background(if (active) SatriaColors.SurfaceHigh else Color.Transparent)
                        ) {
                            Text(
                                label,
                                color = if (active) SatriaColors.TextPrimary else SatriaColors.TextSecondary,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                // ── Recent Apps ─────────────────────────────────────────────
                SLabel("RECENT APPS")
                SToggle("Enable Recent Apps (Double tap background)", recentAppsEnabled) {
                    vm.setRecentAppsEnabled(it)
                }
                if (recentAppsEnabled) {
                    Text(
                        "✅ Aktif · Double tap background launcher = buka Recent Apps\nData: Usage Access dari sistem (akurat, tidak perlu tracking manual)",
                        color = SatriaColors.TextSecondary,
                        fontSize = 11.sp,
                    )
                } else {
                    Text(
                        "Double tap background launcher untuk membuka Recent Apps.\nIzin Usage Access diperlukan (akan diminta otomatis).",
                        color = SatriaColors.TextSecondary,
                        fontSize = 11.sp,
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SatriaColors.TextSecondary),
                        border = BorderStroke(1.dp, SatriaColors.SurfaceHigh)) { Text("Cancel") }
                    Button(onClick = {
                        vm.saveUserName(tempName); vm.saveAssistantName(tempAssist)
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
