package id.satria.launcher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// AppThemeColors — backing fields dengan prefix _ untuk hindari JVM clash
// ─────────────────────────────────────────────────────────────────────────────
@Stable
class AppThemeColors(
    accentInit : Color,
    bgInit     : Color,
    borderInit : Color,
    fontInit   : Color,
) {
    // Backing mutable state — private prefix menghindari JVM signature clash
    private var _accent  by mutableStateOf(accentInit)
    private var _bg      by mutableStateOf(bgInit)
    private var _border  by mutableStateOf(borderInit)
    private var _font    by mutableStateOf(fontInit)

    // Public updaters — dipanggil dari SatriaTheme LaunchedEffect
    fun updateAccent(c: Color) { _accent = c }
    fun updateBg    (c: Color) { _bg     = c }
    fun updateBorder(c: Color) { _border = c }
    fun updateFont  (c: Color) { _font   = c }

    // Static structural colors
    val Surface          = Color(0xFF1C1C1E)
    val SurfaceMid       = Color(0xFF2C2C2E)
    val SurfaceHigh      = Color(0xFF3A3A3C)
    val Danger           = Color(0xFFFF453A)

    // Dynamic palette — derived from user theme
    val Accent           get() = _accent
    val AccentDim        get() = _accent.copy(alpha = 0.75f)
    val Border           get() = _border
    val BorderLight      get() = _font.copy(alpha = 0.08f)
    val DockBg           get() = _bg.copy(alpha = 0.92f)
    val ScreenBackground get() = _bg
    val TextPrimary      get() = _font
    val TextSecondary    get() = _font.copy(alpha = 0.55f)
    val TextTertiary     get() = _font.copy(alpha = 0.28f)
}

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocal
// ─────────────────────────────────────────────────────────────────────────────
val LocalAppTheme = staticCompositionLocalOf<AppThemeColors> {
    error("No AppTheme provided")
}

// ─────────────────────────────────────────────────────────────────────────────
// SatriaColors — @Composable getters, zero allocation, delegates to LocalAppTheme
// ─────────────────────────────────────────────────────────────────────────────
object SatriaColors {
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
// SatriaTheme — root composable
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
            accentInit = hexToColor(accentHex, Color(0xFF27AE60)),
            bgInit     = hexToColor(bgHex,     Color(0xFF000000)),
            borderInit = hexToColor(borderHex, Color(0xFF1A1A1A)),
            fontInit   = hexToColor(fontHex,   Color(0xFFFFFFFF)),
        )
    }
    // Update in-place — no reallocation
    LaunchedEffect(accentHex) { theme.updateAccent(hexToColor(accentHex, Color(0xFF27AE60))) }
    LaunchedEffect(bgHex)     { theme.updateBg    (hexToColor(bgHex,     Color(0xFF000000))) }
    LaunchedEffect(borderHex) { theme.updateBorder(hexToColor(borderHex, Color(0xFF1A1A1A))) }
    LaunchedEffect(fontHex)   { theme.updateFont  (hexToColor(fontHex,   Color(0xFFFFFFFF))) }

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
