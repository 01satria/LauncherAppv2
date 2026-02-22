package id.satria.launcher.ui.screen

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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

    // Slider state — local untuk real-time preview, simpan saat user angkat jari
    var tempIconSize     by remember(iconSize)     { mutableStateOf(iconSize.toFloat()) }
    var tempDockIconSize by remember(dockIconSize) { mutableStateOf(dockIconSize.toFloat()) }

    var tempName   by remember(userName)      { mutableStateOf(userName) }
    var tempAssist by remember(assistantName) { mutableStateOf(assistantName) }

    // ── FIX: key untuk force-recompose AsyncImage setiap ganti avatar ────
    var avatarKey by remember { mutableStateOf(0) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            // ── Decode asli TANPA target size agar tidak distorsi (penyet) ──
            // Lalu crop square dari tengah secara manual
            val rawBmp = if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    // TIDAK set targetSize — biarkan dimensi asli agar proporsional
                }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            // Crop square dari tengah gambar asli (tidak memaksa resize dulu)
            val size   = minOf(rawBmp.width, rawBmp.height)
            val xOff   = (rawBmp.width  - size) / 2
            val yOff   = (rawBmp.height - size) / 2
            val square = android.graphics.Bitmap.createBitmap(rawBmp, xOff, yOff, size, size)
            // Bebaskan raw bitmap segera agar tidak double RAM
            if (square !== rawBmp) rawBmp.recycle()

            // Scale ke 512x512 setelah crop (tetap proporsional karena sudah square)
            val scaled = android.graphics.Bitmap.createScaledBitmap(square, 512, 512, true)
            if (scaled !== square) square.recycle()

            // Simpan avatar
            vm.saveAvatar(scaled)

            // ── Invalidate Coil singleton (bukan instance baru!) ──────────
            // context.imageLoader adalah singleton Coil yang benar-benar dipakai
            coil.Coil.imageLoader(context).memoryCache?.clear()

            // Increment key → paksa AsyncImage recompose dengan data baru
            avatarKey++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* Blok */ },
            shape = RoundedCornerShape(20.dp),
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
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(SatriaColors.SurfaceMid)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatarPath != null) {
                            // key(avatarKey) memaksa Compose buang composable lama dan buat baru
                            // sehingga AsyncImage fetch ulang dari disk tanpa terblokir cache lama
                            key(avatarKey) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(avatarPath)
                                        .diskCacheKey("avatar_$avatarKey") // cache key unik tiap ganti foto
                                        .memoryCacheKey("avatar_$avatarKey")
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape), // ← FIX: clip agar tidak penyet
                                )
                            }
                        } else {
                            Text("👤", fontSize = 36.sp)
                        }
                    }
                }
                TextButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Change Photo", color = SatriaColors.TextSecondary, fontSize = 14.sp)
                }

                SettingsLabel("YOUR NAME")
                SettingsTextField(value = tempName, onValueChange = { tempName = it }, placeholder = "User")

                SettingsLabel("ASSISTANT NAME")
                SettingsTextField(value = tempAssist, onValueChange = { tempAssist = it }, placeholder = "Assistant")

                SettingsLabel("LAYOUT")
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SatriaColors.SurfaceMid),
                ) {
                    listOf("grid" to "⊞  Grid", "list" to "☰  List").forEach { (mode, label) ->
                        val active = layoutMode == mode
                        TextButton(
                            onClick = { vm.setLayoutMode(mode) },
                            modifier = Modifier.weight(1f)
                                .background(if (active) SatriaColors.SurfaceHigh else Color.Transparent),
                        ) {
                            Text(label, color = if (active) SatriaColors.TextPrimary else SatriaColors.TextSecondary,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }

                SettingsToggleRow("Show hidden apps", showHidden) { vm.setShowHidden(it) }
                SettingsToggleRow("Show app names",   showNames)  { vm.setShowNames(it) }

                // ── Icon size sliders ────────────────────────────────────
                SettingsLabel("APP ICON SIZE  (${tempIconSize.toInt()} dp)")
                Slider(
                    value = tempIconSize,
                    onValueChange = { tempIconSize = it },
                    onValueChangeFinished = { vm.setIconSize(tempIconSize.toInt()) },
                    valueRange = MIN_ICON_SIZE.toFloat()..MAX_ICON_SIZE.toFloat(),
                    steps = (MAX_ICON_SIZE - MIN_ICON_SIZE) / 2 - 1,
                    colors = SliderDefaults.colors(
                        thumbColor       = SatriaColors.Accent,
                        activeTrackColor = SatriaColors.Accent,
                        inactiveTrackColor = SatriaColors.SurfaceMid,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                SettingsLabel("DOCK ICON SIZE  (${tempDockIconSize.toInt()} dp)")
                Slider(
                    value = tempDockIconSize,
                    onValueChange = { tempDockIconSize = it },
                    onValueChangeFinished = { vm.setDockIconSize(tempDockIconSize.toInt()) },
                    valueRange = MIN_DOCK_ICON_SIZE.toFloat()..MAX_DOCK_ICON_SIZE.toFloat(),
                    steps = (MAX_DOCK_ICON_SIZE - MIN_DOCK_ICON_SIZE) / 2 - 1,
                    colors = SliderDefaults.colors(
                        thumbColor       = SatriaColors.Accent,
                        activeTrackColor = SatriaColors.Accent,
                        inactiveTrackColor = SatriaColors.SurfaceMid,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SatriaColors.TextSecondary),
                        border = BorderStroke(1.dp, SatriaColors.SurfaceHigh),
                    ) { Text("Cancel") }
                    Button(
                        onClick = { vm.saveUserName(tempName); vm.saveAssistantName(tempAssist); onClose() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.Accent),
                    ) { Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = SatriaColors.TextPrimary, fontSize = 15.sp)
        Switch(checked = value, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Color.White, checkedTrackColor   = SatriaColors.Accent,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = SatriaColors.SurfaceMid,
            )
        )
    }
}