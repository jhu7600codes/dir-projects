package app.venderian.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import app.venderian.R
import app.venderian.data.Holiday
import androidx.compose.ui.graphics.lerp

val VenderianFont = FontFamily(Font(R.font.venderian))

// violet fallback for phones without material you (android 11 and older)
private val Fallback = darkColorScheme(
    primary = Color(0xFFCFBCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378A),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFCBC2DB),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    tertiaryContainer = Color(0xFF633B48),
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
)

/** a holiday's colors on top of the normal dark scheme */
private fun holidayScheme(base: androidx.compose.material3.ColorScheme, h: Holiday): androidx.compose.material3.ColorScheme {
    val primary = Color(h.primary)
    val container = Color(h.container)
    val bg = Color(h.background)
    return base.copy(
        primary = primary,
        onPrimary = Color(0xFF1A1A1A),
        primaryContainer = container,
        onPrimaryContainer = Color(0xFFF5F0FF),
        tertiary = primary,
        tertiaryContainer = container,
        onTertiaryContainer = Color(0xFFF5F0FF),
        secondaryContainer = lerp(container, bg, 0.4f),
        background = bg,
        surface = bg,
        surfaceContainer = lerp(bg, container, 0.15f),
        surfaceContainerHigh = lerp(bg, container, 0.25f),
        surfaceContainerHighest = lerp(bg, container, 0.35f),
    )
}

@Composable
fun VenderianTheme(content: @Composable () -> Unit) {
    val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(LocalContext.current) else Fallback
    val holiday = Holiday.today()
    val colors = if (holiday != null) holidayScheme(base, holiday) else base
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
