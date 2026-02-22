package id.satria.launcher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// AppTheme — mutable, di-compose dari ViewModel, persisted via Prefs
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

    // Derived static colors — tidak ikut user palette (structural / accessibility)
    val Surface         get() = Color(0xFF1C1C1E)
    val SurfaceMid      get() = Color(0xFF2C2C2E)
    val SurfaceHigh     get() = Color(0xFF3A3A3C)
    val TextPrimary     get() = font
    val TextSecondary   get() = font.copy(alpha = 0.55f)
    val TextTertiary    get() = font.copy(alpha = 0.30f)
    val Accent          get() = accent
    val AccentDim       get() = accent.copy(alpha = 0.75f)
    val Danger          get() = Color(0xFFFF453A)
    val DockBg          get() = bg.copy(alpha = 0.92f)
    val Border          get() = border
    val BorderLight     get() = font.copy(alpha = 0.08f)
    val ScreenBackground get() = bg
}

val LocalAppTheme = staticCompositionLocalOf<AppThemeColors> {
    error("No AppTheme provided")
}

// Convenience accessor
object SatriaColors {
    // These are accessed statically from composables — delegate to local theme
    val Background      = Color(0x00000000)
    val Surface         get() = Color(0xFF1C1C1E)
    val SurfaceMid      get() = Color(0xFF2C2C2E)
    val SurfaceHigh     get() = Color(0xFF3A3A3C)
    val TextPrimary     get() = LocalAppTheme.current.TextPrimary
    val TextSecondary   get() = LocalAppTheme.current.TextSecondary
    val TextTertiary    get() = LocalAppTheme.current.TextTertiary
    val Accent          get() = LocalAppTheme.current.Accent
    val AccentDim       get() = LocalAppTheme.current.AccentDim
    val Danger          get() = Color(0xFFFF453A)
    val DockBg          get() = LocalAppTheme.current.DockBg
    val Border          get() = LocalAppTheme.current.Border
    val BorderLight     get() = LocalAppTheme.current.BorderLight
    val ScreenBackground get() = LocalAppTheme.current.ScreenBackground
}

// Parse AARRGGBB hex string → Color, fallback to default
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
    // Update only when hex values change — no reallocation
    LaunchedEffect(accentHex) { theme.accent = hexToColor(accentHex, Color(0xFF27AE60)) }
    LaunchedEffect(bgHex)     { theme.bg     = hexToColor(bgHex,     Color(0xFF000000)) }
    LaunchedEffect(borderHex) { theme.border = hexToColor(borderHex, Color(0xFF1A1A1A)) }
    LaunchedEffect(fontHex)   { theme.font   = hexToColor(fontHex,   Color(0xFFFFFFFF)) }

    val colorScheme = darkColorScheme(
        background     = Color.Transparent,
        surface        = theme.Surface,
        surfaceVariant = theme.SurfaceMid,
        primary        = theme.Accent,
        onPrimary      = Color.White,
        onBackground   = theme.TextPrimary,
        onSurface      = theme.TextPrimary,
        error          = theme.Danger,
        outline        = theme.Border,
    )

    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
