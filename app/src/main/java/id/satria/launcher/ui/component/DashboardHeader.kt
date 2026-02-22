package id.satria.launcher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import id.satria.launcher.ui.theme.SatriaColors
import id.satria.launcher.utils.getAssistantMessage
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardHeader(
    avatarPath: String?,
    userName: String,
    onAvatarClick: () -> Unit,
    onClose: () -> Unit,
) {
    var clockStr by remember { mutableStateOf(getClockStr()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); clockStr = getClockStr() }
    }

    var message by remember(userName) { mutableStateOf(getAssistantMessage(userName)) }
    LaunchedEffect(userName) {
        while (true) { delay(60_000); message = getAssistantMessage(userName) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar — tap buka chat
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SatriaColors.Surface)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center,
        ) {
            if (avatarPath != null) {
                val ctx = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(ctx)
                        .data(avatarPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text("👤", fontSize = 22.sp)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = clockStr,
                color = SatriaColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 3.sp,
            )
            Text(
                text = message,
                color = SatriaColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
            )
        }

        IconButton(onClick = onClose) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SatriaColors.Surface),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = SatriaColors.TextSecondary, fontSize = 14.sp)
            }
        }
    }
}

private fun getClockStr(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())