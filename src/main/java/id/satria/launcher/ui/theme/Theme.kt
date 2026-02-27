package id.satria.launcher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Dark Palette
// ─────────────────────────────────────────────────────────────────────────────
private val darkAccent       = Color(0xFF27AE60)
private val darkBg           = Color(0xFF000000)
private val darkBorder       = Color(0xFF1A1A1A)
private val darkFont         = Color(0xFFFFFFFF)
private val darkSurface      = Color(0xFF1C1C1E)
private val darkSurfaceMid   = Color(0xFF2C2C2E)
private val darkSurfaceHigh  = Color(0xFF3A3A3C)
private val darkDanger       = Color(0xFFFF453A)
// Hardcoded dark colors used in widgets/tool cards
val DarkCardBg               = Color(0xFF0D0D0D)
val DarkWidgetBg             = Color(0xCC1C1C1E)
val DarkDivider              = Color(0xFF1A1A1A)
val DarkDockBorder           = Color(0x1AFFFFFF) // White 10%

// ─────────────────────────────────────────────────────────────────────────────
// Light Palette
// ─────────────────────────────────────────────────────────────────────────────
private val lightAccent      = Color(0xFF1E8449)
private val lightBg          = Color(0xFFF2F2F7)
private val lightBorder      = Color(0xFFD1D1D6)
private val lightFont        = Color(0xFF1C1C1E)
private val lightSurface     = Color(0xFFFFFFFF)
private val lightSurfaceMid  = Color(0xFFE5E5EA)
private val lightSurfaceHigh = Color(0xFFD1D1D6)
private val lightDanger      = Color(0xFFD93025)
// Hardcoded light colors used in widgets/tool cards
val LightCardBg              = Color(0xFFFFFFFF)
val LightWidgetBg            = Color(0xFFFFFFFF)
val LightDivider             = Color(0xFFD1D1D6)
val LightDockBorder          = Color(0x33000000) // Black 20%

// ─────────────────────────────────────────────────────────────────────────────
// AppThemeColors — resolved per mode
// ─────────────────────────────────────────────────────────────────────────────
@Stable
class AppThemeColors(val darkMode: Boolean) {

    val Accent      : Color = if (darkMode) darkAccent      else lightAccent
    val Surface     : Color = if (darkMode) darkSurface     else lightSurface
    val SurfaceMid  : Color = if (darkMode) darkSurfaceMid  else lightSurfaceMid
    val SurfaceHigh : Color = if (darkMode) darkSurfaceHigh else lightSurfaceHigh
    val Danger      : Color = if (darkMode) darkDanger      else lightDanger
    val CardBg      : Color = if (darkMode) DarkCardBg      else LightCardBg
    val WidgetBg    : Color = if (darkMode) DarkWidgetBg    else LightWidgetBg
    val Divider     : Color = if (darkMode) DarkDivider     else LightDivider
    val DockBorder  : Color = if (darkMode) DarkDockBorder  else LightDockBorder

    private val bg     : Color = if (darkMode) darkBg     else lightBg
    private val border : Color = if (darkMode) darkBorder else lightBorder
    private val font   : Color = if (darkMode) darkFont   else lightFont

    fun accentColor()      = Accent
    fun bgColor()          = bg
    fun borderColor()      = border
    fun fontColor()        = font
    fun accentDimColor()   = Accent.copy(alpha = 0.75f)
    fun dockBgColor()      = if (darkMode) Color(0xEB000000) else Color(0xF0FFFFFF)
    fun borderLightColor() = if (darkMode) Color(0x14FFFFFF) else Color(0x33000000)
    fun textSecondary()    = font.copy(alpha = 0.55f)
    fun textTertiary()     = font.copy(alpha = 0.35f)
}

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocal
// ─────────────────────────────────────────────────────────────────────────────
val LocalAppTheme = staticCompositionLocalOf { AppThemeColors(darkMode = true) }

// ─────────────────────────────────────────────────────────────────────────────
// SatriaColors — @Composable getters
// ─────────────────────────────────────────────────────────────────────────────
object SatriaColors {
    val Surface         : Color @Composable get() = LocalAppTheme.current.Surface
    val SurfaceMid      : Color @Composable get() = LocalAppTheme.current.SurfaceMid
    val SurfaceHigh     : Color @Composable get() = LocalAppTheme.current.SurfaceHigh
    val Danger          : Color @Composable get() = LocalAppTheme.current.Danger
    val CardBg          : Color @Composable get() = LocalAppTheme.current.CardBg
    val WidgetBg        : Color @Composable get() = LocalAppTheme.current.WidgetBg
    val Divider         : Color @Composable get() = LocalAppTheme.current.Divider
    val DockBorder      : Color @Composable get() = LocalAppTheme.current.DockBorder
    val Accent          : Color @Composable get() = LocalAppTheme.current.accentColor()
    val AccentDim       : Color @Composable get() = LocalAppTheme.current.accentDimColor()
    val Border          : Color @Composable get() = LocalAppTheme.current.borderColor()
    val BorderLight     : Color @Composable get() = LocalAppTheme.current.borderLightColor()
    val DockBg          : Color @Composable get() = LocalAppTheme.current.dockBgColor()
    val ScreenBackground: Color @Composable get() = LocalAppTheme.current.bgColor()
    val TextPrimary     : Color @Composable get() = LocalAppTheme.current.fontColor()
    val TextSecondary   : Color @Composable get() = LocalAppTheme.current.textSecondary()
    val TextTertiary    : Color @Composable get() = LocalAppTheme.current.textTertiary()
}

// ─────────────────────────────────────────────────────────────────────────────
// SatriaTheme
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SatriaTheme(
    darkMode: Boolean = true,
    content : @Composable () -> Unit,
) {
    val theme = remember(darkMode) { AppThemeColors(darkMode) }

    val colorScheme = if (darkMode) {
        darkColorScheme(
            background     = Color.Transparent,
            surface        = theme.Surface,
            surfaceVariant = theme.SurfaceMid,
            primary        = theme.Accent,
            onPrimary      = Color.White,
            onBackground   = theme.fontColor(),
            onSurface      = theme.fontColor(),
            error          = theme.Danger,
            outline        = theme.borderColor(),
        )
    } else {
        lightColorScheme(
            background     = Color.Transparent,
            surface        = theme.Surface,
            surfaceVariant = theme.SurfaceMid,
            primary        = theme.Accent,
            onPrimary      = Color.White,
            onBackground   = theme.fontColor(),
            onSurface      = theme.fontColor(),
            error          = theme.Danger,
            outline        = theme.borderColor(),
        )
    }

    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
