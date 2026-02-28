package id.satria.launcher.ui.screen

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import id.satria.launcher.MainViewModel
import id.satria.launcher.data.*
import id.satria.launcher.ui.theme.SatriaColors

// ─────────────────────────────────────────────────────────────────────────────
// SettingsSheet — Fullscreen (bukan modal)
// Dipanggil dari HomeScreen saat overlayActive=true
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SettingsSheet(
    vm: MainViewModel,
    onClose: () -> Unit,
    onRequestOverlayPermission: () -> Unit = {},
) {
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
    val recentAppsEnabled by vm.recentAppsEnabled.collectAsState()

    var tempIconSize     by remember(iconSize)     { mutableStateOf(iconSize.toFloat()) }
    var tempDockIconSize by remember(dockIconSize) { mutableStateOf(dockIconSize.toFloat()) }
    var tempName         by remember(userName)      { mutableStateOf(userName) }
    var tempAssist       by remember(assistantName) { mutableStateOf(assistantName) }

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
            val sq   = android.graphics.Bitmap.createBitmap(rawBmp, (rawBmp.width - size) / 2, (rawBmp.height - size) / 2, size, size)
            if (sq !== rawBmp) rawBmp.recycle()
            val sc = android.graphics.Bitmap.createScaledBitmap(sq, 512, 512, true)
            if (sc !== sq) sq.recycle()
            vm.saveAvatar(sc)
            coil.Coil.imageLoader(context).memoryCache?.clear()
            avatarKey++
        }
    }

    // ── Fullscreen surface ────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SatriaColors.ScreenBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Settings",
                    color = SatriaColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onClose) {
                        Text("Cancel", color = SatriaColors.TextSecondary, fontSize = 15.sp)
                    }
                    Button(
                        onClick = {
                            vm.saveUserName(tempName)
                            vm.saveAssistantName(tempAssist)
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.Accent),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            HorizontalDivider(color = SatriaColors.Border, thickness = 0.5.dp)

            // ── Scrollable content ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {

                // ── Avatar ────────────────────────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(SatriaColors.SurfaceMid)
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (avatarPath != null) key(avatarKey) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(avatarPath)
                                        .diskCacheKey("avatar_$avatarKey").memoryCacheKey("avatar_$avatarKey")
                                        .crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                )
                            } else Text("👤", fontSize = 40.sp)
                        }
                        TextButton(onClick = { imagePicker.launch("image/*") }) {
                            Text("Ganti Foto", color = SatriaColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                }

                // ── Profile ───────────────────────────────────────────────────
                SLabel("PROFIL")
                SField(tempName, { tempName = it }, "Nama kamu")
                SField(tempAssist, { tempAssist = it }, "Nama asisten")

                // ── Layout ────────────────────────────────────────────────────
                SLabel("TAMPILAN")
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SatriaColors.SurfaceMid)) {
                    listOf("grid" to "⊞  Grid", "list" to "☰  List").forEach { (mode, label) ->
                        val active = layoutMode == mode
                        TextButton(
                            onClick = { vm.setLayoutMode(mode) },
                            modifier = Modifier.weight(1f).background(if (active) SatriaColors.SurfaceHigh else Color.Transparent),
                        ) {
                            Text(label, color = if (active) SatriaColors.TextPrimary else SatriaColors.TextSecondary,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SatriaColors.SurfaceMid)) {
                    listOf(true to "🌙  Dark", false to "☀️  Light").forEach { (isDark, label) ->
                        val active = darkMode == isDark
                        TextButton(
                            onClick = { vm.setDarkMode(isDark) },
                            modifier = Modifier.weight(1f).background(if (active) SatriaColors.SurfaceHigh else Color.Transparent),
                        ) {
                            Text(label, color = if (active) SatriaColors.TextPrimary else SatriaColors.TextSecondary,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }

                SToggle("Tampilkan app tersembunyi", showHidden) { vm.setShowHidden(it) }
                SToggle("Tampilkan nama app",        showNames)  { vm.setShowNames(it) }

                // ── Icon sizes ────────────────────────────────────────────────
                SLabel("UKURAN ICON APP  (${tempIconSize.toInt()} dp)")
                Slider(
                    value = tempIconSize, onValueChange = { tempIconSize = it },
                    onValueChangeFinished = { vm.setIconSize(tempIconSize.toInt()) },
                    valueRange = MIN_ICON_SIZE.toFloat()..MAX_ICON_SIZE.toFloat(),
                    steps = (MAX_ICON_SIZE - MIN_ICON_SIZE) / 2 - 1,
                    colors = SliderDefaults.colors(thumbColor = SatriaColors.Accent, activeTrackColor = SatriaColors.Accent, inactiveTrackColor = SatriaColors.SurfaceMid),
                    modifier = Modifier.fillMaxWidth(),
                )

                SLabel("UKURAN ICON DOCK  (${tempDockIconSize.toInt()} dp)")
                Slider(
                    value = tempDockIconSize, onValueChange = { tempDockIconSize = it },
                    onValueChangeFinished = { vm.setDockIconSize(tempDockIconSize.toInt()) },
                    valueRange = MIN_DOCK_ICON_SIZE.toFloat()..MAX_DOCK_ICON_SIZE.toFloat(),
                    steps = (MAX_DOCK_ICON_SIZE - MIN_DOCK_ICON_SIZE) / 2 - 1,
                    colors = SliderDefaults.colors(thumbColor = SatriaColors.Accent, activeTrackColor = SatriaColors.Accent, inactiveTrackColor = SatriaColors.SurfaceMid),
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Grid ──────────────────────────────────────────────────────
                if (layoutMode == "grid") {
                    SLabel("KOLOM GRID  ($gridCols)")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (MIN_GRID_COLS..MAX_GRID_COLS).forEach { v ->
                            val active = gridCols == v
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                    .background(if (active) SatriaColors.Accent else SatriaColors.SurfaceMid)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.setGridCols(v) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("$v",
                                    color = if (active) Color.White else SatriaColors.TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }

                    SLabel("BARIS GRID  ($gridRows)")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (MIN_GRID_ROWS..MAX_GRID_ROWS).forEach { v ->
                            val active = gridRows == v
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                    .background(if (active) SatriaColors.Accent else SatriaColors.SurfaceMid)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.setGridRows(v) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("$v",
                                    color = if (active) Color.White else SatriaColors.TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }

                // ── Recent Apps ───────────────────────────────────────────────
                SLabel("RECENT APPS")

                // Toggle
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SatriaColors.SurfaceMid)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Aktifkan recent apps", color = SatriaColors.TextPrimary, fontSize = 15.sp)
                        Text("Swipe tepi kiri untuk membuka", color = SatriaColors.TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = recentAppsEnabled,
                        onCheckedChange = { enabled ->
                            vm.setRecentAppsEnabled(enabled)
                            val activity = context as? id.satria.launcher.MainActivity
                            activity?.syncEdgeSwipeService()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SatriaColors.Accent,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = SatriaColors.SurfaceHigh,
                        ),
                    )
                }

                // Overlay permission checker — selalu tampil jika recent aktif
                if (recentAppsEnabled) {
                    OverlayPermissionCard(onRequestOverlayPermission = onRequestOverlayPermission)
                }
            }
        }
    }
}

// ── Overlay permission card ───────────────────────────────────────────────────
@Composable
private fun OverlayPermissionCard(onRequestOverlayPermission: () -> Unit) {
    val context = LocalContext.current
    // Re-check setiap kali composable di-recompose (user kembali dari Settings)
    val hasOverlay = remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    LaunchedEffect(Unit) {
        // Update saat composable pertama kali muncul
        hasOverlay.value = Settings.canDrawOverlays(context)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (hasOverlay.value) Color(0xFF0F2318) else Color(0xFF2A1212))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (hasOverlay.value) "✅" else "⚠️", fontSize = 18.sp)
            Column {
                Text(
                    if (hasOverlay.value) "Overlay diizinkan" else "Izin overlay diperlukan",
                    color = if (hasOverlay.value) Color(0xFF4CAF50) else Color(0xFFFF6B6B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (hasOverlay.value)
                        "Panel recent akan tampil di atas semua app"
                    else
                        "Tanpa izin ini, swipe tidak akan bekerja",
                    color = SatriaColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        if (!hasOverlay.value) {
            Button(
                onClick = onRequestOverlayPermission,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.Accent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                Text(
                    "Izinkan 'Tampil di atas aplikasi lain'",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
@Composable private fun SLabel(text: String) =
    Text(text, color = SatriaColors.TextSecondary, fontSize = 11.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)

@Composable private fun SField(value: String, onChange: (String) -> Unit, placeholder: String) =
    TextField(value = value, onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
        placeholder = { Text(placeholder, color = SatriaColors.TextTertiary) }, singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SatriaColors.SurfaceMid, unfocusedContainerColor = SatriaColors.SurfaceMid,
            focusedTextColor = SatriaColors.TextPrimary, unfocusedTextColor = SatriaColors.TextPrimary,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            cursorColor = SatriaColors.Accent,
        ))

@Composable private fun SToggle(label: String, value: Boolean, onToggle: (Boolean) -> Unit) =
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SatriaColors.TextPrimary, fontSize = 15.sp)
        Switch(checked = value, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SatriaColors.Accent,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = SatriaColors.SurfaceHigh))
    }
