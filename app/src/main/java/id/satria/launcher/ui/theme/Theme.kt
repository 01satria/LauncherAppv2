package id.satria.launcher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// AppThemeColors — mutable state, di-update dari ViewModel
// ─────────────────────────────────────────────────────────────────────────────
@Stable
class AppThemeColors(
    accent : Color,
    bg     : Color,
    border : Color,
    font   : Color,
) {
    var accent  by mutableStateOf(accent)
    var bg      by mutableStateOf(bg)
    var border  by mutableStateOf(border)
    var font    by mutableStateOf(font)

    val Surface          = Color(0xFF1C1C1E)
    val SurfaceMid       = Color(0xFF2C2C2E)
    val SurfaceHigh      = Color(0xFF3A3A3C)
    val Danger           = Color(0xFFFF453A)
    val Background       = Color(0x00000000)

    val TextPrimary      get() = font
    val TextSecondary    get() = font.copy(alpha = 0.55f)
    val TextTertiary     get() = font.copy(alpha = 0.28f)
    val Accent           get() = accent
    val AccentDim        get() = accent.copy(alpha = 0.75f)
    val DockBg           get() = bg.copy(alpha = 0.92f)
    val Border           get() = border
    val BorderLight      get() = font.copy(alpha = 0.08f)
    val ScreenBackground get() = bg
}

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocal
// ─────────────────────────────────────────────────────────────────────────────
val LocalAppTheme = staticCompositionLocalOf<AppThemeColors> {
    error("No AppTheme provided")
}

// ─────────────────────────────────────────────────────────────────────────────
// SatriaColors — @Composable accessor, no allocation, delegates to LocalAppTheme
// Usage in composables: SatriaColors.Accent, SatriaColors.TextPrimary, etc.
// ─────────────────────────────────────────────────────────────────────────────
object SatriaColors {
    val current: AppThemeColors
        @Composable get() = LocalAppTheme.current

    val Background: Color
        @Composable get() = Color(0x00000000)
    val Surface: Color
        @Composable get() = LocalAppTheme.current.Surface
    val SurfaceMid: Color
        @Composable get() = LocalAppTheme.current.SurfaceMid
    val SurfaceHigh: Color
        @Composable get() = LocalAppTheme.current.SurfaceHigh
    val TextPrimary: Color
        @Composable get() = LocalAppTheme.current.TextPrimary
    val TextSecondary: Color
        @Composable get() = LocalAppTheme.current.TextSecondary
    val TextTertiary: Color
        @Composable get() = LocalAppTheme.current.TextTertiary
    val Accent: Color
        @Composable get() = LocalAppTheme.current.Accent
    val AccentDim: Color
        @Composable get() = LocalAppTheme.current.AccentDim
    val Danger: Color
        @Composable get() = Color(0xFFFF453A)
    val DockBg: Color
        @Composable get() = LocalAppTheme.current.DockBg
    val Border: Color
        @Composable get() = LocalAppTheme.current.Border
    val BorderLight: Color
        @Composable get() = LocalAppTheme.current.BorderLight
    val ScreenBackground: Color
        @Composable get() = LocalAppTheme.current.ScreenBackground
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
fun hexToColor(hex: String, default: Color): Color = runCatching {
    Color(java.lang.Long.parseLong(hex, 16).toInt())
}.getOrDefault(default)

fun colorToHex(color: Color): String {
    val a = (color.alpha * 255).toInt()
    val r = (color.red   * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue  * 255).toInt()
    return "%02X%02X%02X%02X".format(a, r, g, b)
}

// ─────────────────────────────────────────────────────────────────────────────
// SatriaTheme — root composable, provide theme + MaterialTheme
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SatriaTheme(
    accentHex : String = "FF27AE60",
    bgHex     : String = "FF000000",
    borderHex : String = "FF1A1A1A",
    fontHex   : String = "FFFFFFFF",
    content   : @Composable () -> Unit,
) {
    val theme = remember {
        AppThemeColors(
            accent = hexToColor(accentHex, Color(0xFF27AE60)),
            bg     = hexToColor(bgHex,     Color(0xFF000000)),
            border = hexToColor(borderHex, Color(0xFF1A1A1A)),
            font   = hexToColor(fontHex,   Color(0xFFFFFFFF)),
        )
    }
    // Update state in-place when hex values change (no reallocation)
    LaunchedEffect(accentHex) { theme.accent = hexToColor(accentHex, Color(0xFF27AE60)) }
    LaunchedEffect(bgHex)     { theme.bg     = hexToColor(bgHex,     Color(0xFF000000)) }
    LaunchedEffect(borderHex) { theme.border = hexToColor(borderHex, Color(0xFF1A1A1A)) }
    LaunchedEffect(fontHex)   { theme.font   = hexToColor(fontHex,   Color(0xFFFFFFFF)) }

    val colorScheme = darkColorScheme(
        background     = Color.Transparent,
        surface        = theme.Surface,
        surfaceVariant = theme.SurfaceMid,
        primary        = theme.accent,
        onPrimary      = Color.White,
        onBackground   = theme.font,
        onSurface      = theme.font,
        error          = theme.Danger,
        outline        = theme.border,
    )

    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
