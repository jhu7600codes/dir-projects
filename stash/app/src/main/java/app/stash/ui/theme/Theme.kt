package app.stash.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import app.stash.data.prefs.ThemeMode

// stash's own look (dynamic wallpaper colors are an option in settings): near-black with a
// violet-to-pink accent, the colors of the icon. the light theme is the same, inverted.
private val LightScheme = lightColorScheme(
    primary = Color(0xFF6B3FE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DCFF),
    onPrimaryContainer = Color(0xFF22005D),
    inversePrimary = Color(0xFFC4B0FF),
    secondary = Color(0xFFC2185B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E4),
    onSecondaryContainer = Color(0xFF3E0020),
    tertiary = Color(0xFF00897B),
    tertiaryContainer = Color(0xFFCDEFEA),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFFBFAFE),
    onBackground = Color(0xFF121216),
    surface = Color(0xFFFBFAFE),
    onSurface = Color(0xFF121216),
    onSurfaceVariant = Color(0xFF5D5B66),
    surfaceVariant = Color(0xFFE7E5EE),
    outline = Color(0xFF8E8B98),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F3FA),
    surfaceContainer = Color(0xFFEFEDF5),
    surfaceContainerHigh = Color(0xFFE9E7F0),
    surfaceContainerHighest = Color(0xFFE2E0EA),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB9A2FF),
    onPrimary = Color(0xFF250A6B),
    primaryContainer = Color(0xFF3F2A86),
    onPrimaryContainer = Color(0xFFE6DCFF),
    inversePrimary = Color(0xFF6B3FE0),
    secondary = Color(0xFFFF84B4),
    onSecondary = Color(0xFF4A0025),
    secondaryContainer = Color(0xFF5E1A3B),
    onSecondaryContainer = Color(0xFFFFD9E4),
    tertiary = Color(0xFF5FDBC9),
    tertiaryContainer = Color(0xFF0E3D37),
    onTertiaryContainer = Color(0xFFB8F5EC),
    background = Color(0xFF09090C),
    onBackground = Color(0xFFF2F1F6),
    surface = Color(0xFF09090C),
    onSurface = Color(0xFFF2F1F6),
    onSurfaceVariant = Color(0xFFA9A7B3),
    surfaceVariant = Color(0xFF2A2933),
    outline = Color(0xFF6E6C78),
    surfaceContainerLowest = Color(0xFF050507),
    surfaceContainerLow = Color(0xFF111116),
    surfaceContainer = Color(0xFF17171E),
    surfaceContainerHigh = Color(0xFF1F1F28),
    surfaceContainerHighest = Color(0xFF292933),
)

// tighter corners than before, closer to spotify's covers and cards
val StashShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

private val base = Typography()
// heavy, tight headlines and bold titles: big confident type, like spotify's
val StashTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.25).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun StashTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(colorScheme = scheme, shapes = StashShapes, typography = StashTypography, content = content)
}
