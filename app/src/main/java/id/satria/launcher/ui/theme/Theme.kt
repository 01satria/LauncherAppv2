package id.satria.launcher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object SatriaColors {
    // ── Background transparan — wallpaper terlihat di belakang ──────────────
    val Background      = Color(0x00000000) // fully transparent
    val Surface         = Color(0xCC1C1C1E) // 80% opaque — tetap readable
    val SurfaceMid      = Color(0xCC2C2C2E)
    val SurfaceHigh     = Color(0xCC3A3A3C)
    val TextPrimary     = Color(0xFFFFFFFF)
    val TextSecondary   = Color(0xFF8E8E93)
    val TextTertiary    = Color(0xFF555555)
    val Accent          = Color(0xFF27AE60)
    val AccentDim       = Color(0xFF1E8449)
    val Danger          = Color(0xFFFF453A)
    val DockBg          = Color(0xCC000000) // 80% opaque dock
    val Border          = Color(0x331A1A1A)
    val BorderLight     = Color(0x14FFFFFF)
}

private val DarkColorScheme = darkColorScheme(
    background     = Color.Transparent,
    surface        = SatriaColors.Surface,
    surfaceVariant = SatriaColors.SurfaceMid,
    primary        = SatriaColors.Accent,
    onPrimary      = Color.White,
    onBackground   = SatriaColors.TextPrimary,
    onSurface      = SatriaColors.TextPrimary,
    error          = SatriaColors.Danger,
    outline        = SatriaColors.Border,
)

@Composable
fun SatriaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}