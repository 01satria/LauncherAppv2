package id.satria.launcher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// AppThemeColors
// Store colors as Long (ULong bits) to avoid JVM inline-class signature clash.
// Color is an inline class over Long — two properties returning Color in the
// same class produce identical JVM signatures → "Platform declaration clash".
// Storing as Long sidesteps this entirely.
// ─────────────────────────────────────────────────────────────────────────────
@Stable
class AppThemeColors(
    accentInit : Color,
    bgInit     : Color,
    borderInit : Color,
    fontInit   : Color,
) {
    private var accentBits by mutableLongStateOf(accentInit.value.toLong())
    private var bgBits     by mutableLongStateOf(bgInit.value.toLong())
    private var borderBits by mutableLongStateOf(borderInit.value.toLong())
    private var fontBits   by mutableLongStateOf(fontInit.value.toLong())

    fun updateAccent(c: Color) { accentBits = c.value.toLong() }
    fun updateBg    (c: Color) { bgBits     = c.value.toLong() }
    fun updateBorder(c: Color) { borderBits = c.value.toLong() }
    fun updateFont  (c: Color) { fontBits   = c.value.toLong() }

    // Static structural — no conflict, these are val (not Color getters with mangling)
    val Surface    = Color(0xFF1C1C1E)
    val SurfaceMid = Color(0xFF2C2C2E)
    val SurfaceHigh= Color(0xFF3A3A3C)
    val Danger     = Color(0xFFFF453A)

    // Derived — computed from Long bits, returned as Color
    // Functions instead of val to avoid any getter mangling issue
    fun accent()        : Color = Color(accentBits.toULong())
    fun bg()            : Color = Color(bgBits.toULong())
    fun border()        : Color = Color(borderBits.toULong())
    fun font()          : Color = Color(fontBits.toULong())
    fun accentDim()     : Color = Color(accentBits.toULong()).copy(alpha = 0.75f)
    fun borderLight()   : Color = Color(fontBits.toULong()).copy(alpha = 0.08f)
    fun dockBg()        : Color = Color(bgBits.toULong()).copy(alpha = 0.92f)
    fun screenBg()      : Color = Color(bgBits.toULong())
    fun textPrimary()   : Color = Color(fontBits.toULong())
    fun textSecondary() : Color = Color(fontBits.toULong()).copy(alpha = 0.55f)
    fun textTertiary()  : Color = Color(fontBits.toULong()).copy(alpha = 0.28f)
}

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocal
// ─────────────────────────────────────────────────────────────────────────────
val LocalAppTheme = staticCompositionLocalOf<AppThemeColors> {
    error("No AppTheme provided")
}

// ─────────────────────────────────────────────────────────────────────────────
// SatriaColors — @Composable getters delegating to LocalAppTheme
// ─────────────────────────────────────────────────────────────────────────────
object SatriaColors {
    val Background   : Color @Composable get() = Color(0x00000000)
    val Surface      : Color @Composable get() = LocalAppTheme.current.Surface
    val SurfaceMid   : Color @Composable get() = LocalAppTheme.current.SurfaceMid
    val SurfaceHigh  : Color @Composable get() = LocalAppTheme.current.SurfaceHigh
    val Danger       : Color @Composable get() = LocalAppTheme.current.Danger
    val Accent       : Color @Composable get() = LocalAppTheme.current.accent()
    val AccentDim    : Color @Composable get() = LocalAppTheme.current.accentDim()
    val Border       : Color @Composable get() = LocalAppTheme.current.border()
    val BorderLight  : Color @Composable get() = LocalAppTheme.current.borderLight()
    val DockBg       : Color @Composable get() = LocalAppTheme.current.dockBg()
    val ScreenBackground: Color @Composable get() = LocalAppTheme.current.screenBg()
    val TextPrimary  : Color @Composable get() = LocalAppTheme.current.textPrimary()
    val TextSecondary: Color @Composable get() = LocalAppTheme.current.textSecondary()
    val TextTertiary : Color @Composable get() = LocalAppTheme.current.textTertiary()
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
    val theme = remember {
        AppThemeColors(
            accentInit = hexToColor(accentHex, Color(0xFF27AE60)),
            bgInit     = hexToColor(bgHex,     Color(0xFF000000)),
            borderInit = hexToColor(borderHex, Color(0xFF1A1A1A)),
            fontInit   = hexToColor(fontHex,   Color(0xFFFFFFFF)),
        )
    }
    LaunchedEffect(accentHex) { theme.updateAccent(hexToColor(accentHex, Color(0xFF27AE60))) }
    LaunchedEffect(bgHex)     { theme.updateBg    (hexToColor(bgHex,     Color(0xFF000000))) }
    LaunchedEffect(borderHex) { theme.updateBorder(hexToColor(borderHex, Color(0xFF1A1A1A))) }
    LaunchedEffect(fontHex)   { theme.updateFont  (hexToColor(fontHex,   Color(0xFFFFFFFF))) }

    val colorScheme = darkColorScheme(
        background     = Color.Transparent,
        surface        = theme.Surface,
        surfaceVariant = theme.SurfaceMid,
        primary        = theme.accent(),
        onPrimary      = Color.White,
        onBackground   = theme.textPrimary(),
        onSurface      = theme.textPrimary(),
        error          = theme.Danger,
        outline        = theme.border(),
    )

    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
