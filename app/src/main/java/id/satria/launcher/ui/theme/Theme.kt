package id.satria.launcher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Color Palettes
// ─────────────────────────────────────────────────────────────────────────────

private object DarkPalette {
    val Accent       = Color(0xFF27AE60)
    val Bg           = Color(0xFF000000)
    val Border       = Color(0xFF1A1A1A)
    val Font         = Color(0xFFFFFFFF)
    val Surface      = Color(0xFF1C1C1E)
    val SurfaceMid   = Color(0xFF2C2C2E)
    val SurfaceHigh  = Color(0xFF3A3A3C)
    val Danger       = Color(0xFFFF453A)
}

private object LightPalette {
    val Accent       = Color(0xFF1E8449)
    val Bg           = Color(0xFFF2F2F7)
    val Border       = Color(0xFFD1D1D6)
    val Font         = Color(0xFF000000)
    val Surface      = Color(0xFFFFFFFF)
    val SurfaceMid   = Color(0xFFE5E5EA)
    val SurfaceHigh  = Color(0xFFD1D1D6)
    val Danger       = Color(0xFFD93025)
}

// ─────────────────────────────────────────────────────────────────────────────
// AppThemeColors — resolved per mode, no mutation needed
// ─────────────────────────────────────────────────────────────────────────────

@Stable
class AppThemeColors(val darkMode: Boolean) {
    private val p = if (darkMode) DarkPalette else LightPalette

    val Accent      : Color = p.Accent
    val Surface     : Color = p.Surface
    val SurfaceMid  : Color = p.SurfaceMid
    val SurfaceHigh : Color = p.SurfaceHigh
    val Danger      : Color = p.Danger

    fun accentColor()      = p.Accent
    fun bgColor()          = p.Bg
    fun borderColor()      = p.Border
    fun fontColor()        = p.Font
    fun accentDimColor()   = p.Accent.copy(alpha = 0.75f)
    fun dockBgColor()      = p.Bg.copy(alpha = 0.92f)
    fun borderLightColor() = p.Font.copy(alpha = 0.08f)
    fun textSecondary()    = p.Font.copy(alpha = 0.55f)
    fun textTertiary()     = p.Font.copy(alpha = 0.28f)
}

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocal
// ─────────────────────────────────────────────────────────────────────────────

val LocalAppTheme = staticCompositionLocalOf { AppThemeColors(darkMode = true) }

// ─────────────────────────────────────────────────────────────────────────────
// SatriaColors — @Composable getters, delegates to LocalAppTheme
// ─────────────────────────────────────────────────────────────────────────────

object SatriaColors {
    val Surface         : Color @Composable get() = LocalAppTheme.current.Surface
    val SurfaceMid      : Color @Composable get() = LocalAppTheme.current.SurfaceMid
    val SurfaceHigh     : Color @Composable get() = LocalAppTheme.current.SurfaceHigh
    val Danger          : Color @Composable get() = LocalAppTheme.current.Danger
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
