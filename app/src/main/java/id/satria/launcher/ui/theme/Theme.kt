package id.satria.launcher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object SatriaColors {
    val Background      = Color(0xFF000000)
    val Surface         = Color(0xFF1C1C1E)
    val SurfaceMid      = Color(0xFF2C2C2E)
    val SurfaceHigh     = Color(0xFF3A3A3C)
    val TextPrimary     = Color(0xFFFFFFFF)
    val TextSecondary   = Color(0xFF8E8E93)
    val TextTertiary    = Color(0xFF555555)
    val Accent          = Color(0xFF27AE60)
    val AccentDim       = Color(0xFF1E8449)
    val Danger          = Color(0xFFFF453A)
    val DockBg          = Color(0xEB000000)
    val Border          = Color(0xFF1A1A1A)
    val BorderLight     = Color(0x14FFFFFF)
}

private val DarkColorScheme = darkColorScheme(
    background   = SatriaColors.Background,
    surface      = SatriaColors.Surface,
    surfaceVariant = SatriaColors.SurfaceMid,
    primary      = SatriaColors.Accent,
    onPrimary    = Color.White,
    onBackground = SatriaColors.TextPrimary,
    onSurface    = SatriaColors.TextPrimary,
    error        = SatriaColors.Danger,
    outline      = SatriaColors.Border,
)

@Composable
fun SatriaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
