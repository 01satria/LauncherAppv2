package id.satria.launcher.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.satria.launcher.MainViewModel
import id.satria.launcher.ui.theme.SatriaColors
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(vm: MainViewModel, onClose: () -> Unit) {
    val context       = LocalContext.current
    val userName      by vm.userName.collectAsState()
    val assistantName by vm.assistantName.collectAsState()
    val showHidden    by vm.showHidden.collectAsState()
    val showNames     by vm.showNames.collectAsState()
    val layoutMode    by vm.layoutMode.collectAsState()
    val avatarPath    by vm.avatarPath.collectAsState()

    var tempName      by remember(userName)      { mutableStateOf(userName) }
    var tempAssist    by remember(assistantName) { mutableStateOf(assistantName) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val bmp = if (android.os.Build.VERSION.SDK_INT >= 28) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            vm.saveAvatar(bmp)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SatriaColors.Surface),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Title
                Text("Settings", color = SatriaColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                // Avatar
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SatriaColors.SurfaceMid),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatarPath != null) {
                            AsyncImage(model = avatarPath, contentDescription = null, modifier = Modifier.fillMaxSize())
                        } else {
                            Text("👤", fontSize = 32.sp)
                        }
                    }
                }

                TextButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Change Photo", color = SatriaColors.TextSecondary)
                }

                // User Name
                SettingsLabel("YOUR NAME")
                SettingsTextField(value = tempName, onValueChange = { tempName = it }, placeholder = "User")

                // Assistant Name
                SettingsLabel("ASSISTANT NAME")
                SettingsTextField(value = tempAssist, onValueChange = { tempAssist = it }, placeholder = "Assistant")

                // Layout Mode
                SettingsLabel("LAYOUT")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SatriaColors.SurfaceMid),
                ) {
                    listOf("grid" to "Grid", "list" to "List").forEach { (mode, label) ->
                        val active = layoutMode == mode
                        TextButton(
                            onClick = { vm.setLayoutMode(mode) },
                            modifier = Modifier
                                .weight(1f)
                                .background(if (active) SatriaColors.SurfaceHigh else Color.Transparent),
                        ) {
                            Text(label, color = if (active) SatriaColors.TextPrimary else SatriaColors.TextSecondary)
                        }
                    }
                }

                // Toggles
                SettingsToggleRow("Show hidden apps", showHidden) { vm.setShowHidden(it) }
                SettingsToggleRow("Show app names", showNames) { vm.setShowNames(it) }

                // Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SatriaColors.TextSecondary),
                    ) { Text("Cancel") }

                    Button(
                        onClick = {
                            vm.saveUserName(tempName)
                            vm.saveAssistantName(tempAssist)
                            onClose()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SatriaColors.SurfaceHigh),
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(text, color = SatriaColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
}

@Composable
private fun SettingsTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    TextField(
        value = value,
        onValueChange = onValueChange,
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

@Composable
private fun SettingsToggleRow(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
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
                uncheckedThumbColor= Color.White,
                uncheckedTrackColor= SatriaColors.SurfaceMid,
            )
        )
    }
}
