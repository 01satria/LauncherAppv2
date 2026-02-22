package id.satria.launcher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Helpers — hex AARRGGBB (8 chars) ↔ Color
// Color(Long) constructor takes ARGB packed as signed Long — same as 0xFFRRGGBB literals
// ─────────────────────────────────────────────────────────────────────────────
fun hexToColor(hex: String, default: Color): Color {
    val cleaned = hex.trimStart('#').padStart(8, 'F')
    return runCatching {
        val long = cleaned.toLong(16)
        // Color(long) uses ARGB packed — same convention as Color(0xFF......L)
        Color(
            alpha = ((long shr 24) and 0xFF) / 255f,
            red   = ((long shr 16) and 0xFF) / 255f,
            green = ((long shr  8) and 0xFF) / 255f,
            blue  = ((long       ) and 0xFF) / 255f,
        )
    }.getOrDefault(default)
}

fun colorToHex(c: Color): String {
    val a = (c.alpha * 255 + .5f).toInt()
    val r = (c.red   * 255 + .5f).toInt()
    val g = (c.green * 255 + .5f).toInt()
    val b = (c.blue  * 255 + .5f).toInt()
    return "%02X%02X%02X%02X".format(a, r, g, b)
}

// ─────────────────────────────────────────────────────────────────────────────
// AppThemeColors — mutable snapshot state, no inline-class clash
// Store as Float components (alpha/red/green/blue) to avoid Color JVM mangling
// ─────────────────────────────────────────────────────────────────────────────
@Stable
class AppThemeColors(init: ThemePalette) {

    private var accentA by mutableFloatStateOf(init.accent.alpha)
    private var accentR by mutableFloatStateOf(init.accent.red)
    private var accentG by mutableFloatStateOf(init.accent.green)
    private var accentB by mutableFloatStateOf(init.accent.blue)

    private var bgA by mutableFloatStateOf(init.bg.alpha)
    private var bgR by mutableFloatStateOf(init.bg.red)
    private var bgG by mutableFloatStateOf(init.bg.green)
    private var bgB by mutableFloatStateOf(init.bg.blue)

    private var borderA by mutableFloatStateOf(init.border.alpha)
    private var borderR by mutableFloatStateOf(init.border.red)
    private var borderG by mutableFloatStateOf(init.border.green)
    private var borderB by mutableFloatStateOf(init.border.blue)

    private var fontA by mutableFloatStateOf(init.font.alpha)
    private var fontR by mutableFloatStateOf(init.font.red)
    private var fontG by mutableFloatStateOf(init.font.green)
    private var fontB by mutableFloatStateOf(init.font.blue)

    fun updateAccent(c: Color) { accentA=c.alpha; accentR=c.red; accentG=c.green; accentB=c.blue }
    fun updateBg    (c: Color) { bgA=c.alpha;     bgR=c.red;     bgG=c.green;     bgB=c.blue }
    fun updateBorder(c: Color) { borderA=c.alpha; borderR=c.red; borderG=c.green; borderB=c.blue }
    fun updateFont  (c: Color) { fontA=c.alpha;   fontR=c.red;   fontG=c.green;   fontB=c.blue }

    // Static structural colors (never change)
    val Surface     = Color(0xFF1C1C1E)
    val SurfaceMid  = Color(0xFF2C2C2E)
    val SurfaceHigh = Color(0xFF3A3A3C)
    val Danger      = Color(0xFFFF453A)

    // Dynamic — computed from float components (no Color return mangling issue)
    fun accentColor()      = Color(accentA, accentR, accentG, accentB)
    fun bgColor()          = Color(bgA, bgR, bgG, bgB)
    fun borderColor()      = Color(borderA, borderR, borderG, borderB)
    fun fontColor()        = Color(fontA, fontR, fontG, fontB)
    fun accentDimColor()   = Color(accentA * .75f, accentR, accentG, accentB)
    fun dockBgColor()      = Color(bgA * .92f, bgR, bgG, bgB)
    fun borderLightColor() = Color(fontA * .08f, fontR, fontG, fontB)
    fun textSecondary()    = Color(fontA * .55f, fontR, fontG, fontB)
    fun textTertiary()     = Color(fontA * .28f, fontR, fontG, fontB)
}

data class ThemePalette(
    val accent : Color = Color(0xFF27AE60),
    val bg     : Color = Color(0xFF000000),
    val border : Color = Color(0xFF1A1A1A),
    val font   : Color = Color(0xFFFFFFFF),
)

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocal
// ─────────────────────────────────────────────────────────────────────────────
val LocalAppTheme = staticCompositionLocalOf { AppThemeColors(ThemePalette()) }

// ─────────────────────────────────────────────────────────────────────────────
// SatriaColors — @Composable getters, delegates to LocalAppTheme
// ─────────────────────────────────────────────────────────────────────────────
object SatriaColors {
    val Surface      : Color @Composable get() = LocalAppTheme.current.Surface
    val SurfaceMid   : Color @Composable get() = LocalAppTheme.current.SurfaceMid
    val SurfaceHigh  : Color @Composable get() = LocalAppTheme.current.SurfaceHigh
    val Danger       : Color @Composable get() = LocalAppTheme.current.Danger
    val Accent       : Color @Composable get() = LocalAppTheme.current.accentColor()
    val AccentDim    : Color @Composable get() = LocalAppTheme.current.accentDimColor()
    val Border       : Color @Composable get() = LocalAppTheme.current.borderColor()
    val BorderLight  : Color @Composable get() = LocalAppTheme.current.borderLightColor()
    val DockBg       : Color @Composable get() = LocalAppTheme.current.dockBgColor()
    val ScreenBackground: Color @Composable get() = LocalAppTheme.current.bgColor()
    val TextPrimary  : Color @Composable get() = LocalAppTheme.current.fontColor()
    val TextSecondary: Color @Composable get() = LocalAppTheme.current.textSecondary()
    val TextTertiary : Color @Composable get() = LocalAppTheme.current.textTertiary()
}

// ─────────────────────────────────────────────────────────────────────────────
// SatriaTheme
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SatriaTheme(
    accentHex : String = "FF27AE60",
    bgHex     : String = "FF000000",
    borderHex : String = "FF1A1A1A",
    fontHex   : String = "FFFFFFFF",
    content   : @Composable () -> Unit,
) {
    val palette = ThemePalette(
        accent = hexToColor(accentHex, Color(0xFF27AE60)),
        bg     = hexToColor(bgHex,     Color(0xFF000000)),
        border = hexToColor(borderHex, Color(0xFF1A1A1A)),
        font   = hexToColor(fontHex,   Color(0xFFFFFFFF)),
    )

    // remember with no key — update in-place via SideEffect every recomposition
    val theme = remember { AppThemeColors(palette) }
    SideEffect {
        theme.updateAccent(palette.accent)
        theme.updateBg    (palette.bg)
        theme.updateBorder(palette.border)
        theme.updateFont  (palette.font)
    }

    val colorScheme = darkColorScheme(
        background     = Color.Transparent,
        surface        = theme.Surface,
        surfaceVariant = theme.SurfaceMid,
        primary        = palette.accent,
        onPrimary      = Color.White,
        onBackground   = palette.font,
        onSurface      = palette.font,
        error          = theme.Danger,
        outline        = palette.border,
    )

    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
