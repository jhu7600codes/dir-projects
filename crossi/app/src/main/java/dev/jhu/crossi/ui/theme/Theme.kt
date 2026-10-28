package dev.jhu.crossi.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { DARK, LIGHT, SYSTEM }

// Fallback palette for android < 12 or when dynamic color is turned off.
private val CrossiDark = darkColorScheme(
    primary = Color(0xFFB7C4FF),
    onPrimary = Color(0xFF1E2C60),
    primaryContainer = Color(0xFF354378),
    onPrimaryContainer = Color(0xFFDCE1FF),
    secondary = Color(0xFFFFB68A),
    onSecondary = Color(0xFF522300),
    tertiary = Color(0xFF8FD5B0),
    background = Color(0xFF121318),
    surface = Color(0xFF121318),
    surfaceContainer = Color(0xFF1F1F25),
    error = Color(0xFFFFB4AB),
)

private val CrossiLight = lightColorScheme(
    primary = Color(0xFF4D5C92),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE1FF),
    onPrimaryContainer = Color(0xFF05164B),
    secondary = Color(0xFF8B4B1F),
    tertiary = Color(0xFF2A6A4D),
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFBF8FF),
    error = Color(0xFFBA1A1A),
)

@Composable
fun CrossiTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> CrossiDark
        else -> CrossiLight
    }
    MaterialTheme(colorScheme = scheme, typography = CrossiTypography, content = content)
}
