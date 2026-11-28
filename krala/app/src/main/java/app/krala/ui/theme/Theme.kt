package app.krala.ui.theme

import android.app.Activity
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import app.krala.data.settings.ThemeMode

object KralaColors {
    val Seed = Color(0xFFB3121F)
    val Mid = Color(0xFF5A0912)
    val Logo = Brush.linearGradient(listOf(Color(0xFFB3121F), Color(0xFF5A0912), Color(0xFF000000)))
}

private val LightKrala = lightColorScheme(
    primary = Color(0xFFB3121F), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6), onPrimaryContainer = Color(0xFF410003),
    secondary = Color(0xFF775653), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6), onSecondaryContainer = Color(0xFF2C1513),
    tertiary = Color(0xFF725B2E), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEDFA6), onTertiaryContainer = Color(0xFF261900),
    error = Color(0xFFBA1A1A), onError = Color.White, errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F7), onBackground = Color(0xFF231918),
    surface = Color(0xFFFFF8F7), onSurface = Color(0xFF231918),
    surfaceVariant = Color(0xFFF5DDDA), onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857371), outlineVariant = Color(0xFFD8C2BF),
    inverseSurface = Color(0xFF392E2D), inverseOnSurface = Color(0xFFFFEDEA), inversePrimary = Color(0xFFFFB3AC),
    surfaceTint = Color(0xFFB3121F), scrim = Color.Black,
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFF0EE),
    surfaceContainer = Color(0xFFFCEAE7), surfaceContainerHigh = Color(0xFFF6E4E2),
    surfaceContainerHighest = Color(0xFFF1DEDC), surfaceDim = Color(0xFFE8D6D4), surfaceBright = Color(0xFFFFF8F7),
)

/** dark red on black */
private val DarkKrala = darkColorScheme(
    primary = Color(0xFFFFB3AC), onPrimary = Color(0xFF680008),
    primaryContainer = Color(0xFF8F0A15), onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB8), onSecondary = Color(0xFF442927),
    secondaryContainer = Color(0xFF5D3F3C), onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFE1C38C), onTertiary = Color(0xFF3F2D04),
    tertiaryContainer = Color(0xFF584419), onTertiaryContainer = Color(0xFFFEDFA6),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F0A0A), onBackground = Color(0xFFF1DEDC),
    surface = Color(0xFF0F0A0A), onSurface = Color(0xFFF1DEDC),
    surfaceVariant = Color(0xFF534341), onSurfaceVariant = Color(0xFFD8C2BF),
    outline = Color(0xFFA08C8A), outlineVariant = Color(0xFF534341),
    inverseSurface = Color(0xFFF1DEDC), inverseOnSurface = Color(0xFF392E2D), inversePrimary = Color(0xFFB3121F),
    surfaceTint = Color(0xFFFFB3AC), scrim = Color.Black,
    surfaceContainerLowest = Color(0xFF0A0606), surfaceContainerLow = Color(0xFF181212),
    surfaceContainer = Color(0xFF1D1515), surfaceContainerHigh = Color(0xFF271F1E),
    surfaceContainerHighest = Color(0xFF322928), surfaceDim = Color(0xFF0F0A0A), surfaceBright = Color(0xFF3A2F2E),
)

private fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black, surface = Color.Black, surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF0B0707),
    surfaceContainer = Color(0xFF110B0B), surfaceContainerHigh = Color(0xFF1A1212),
    surfaceContainerHighest = Color(0xFF231A1A), surfaceBright = Color(0xFF2A2020),
)

private val KralaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val base = Typography()
private val KralaType = base.copy(
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(lineHeight = 24.sp),
)

val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp)

/** haptics that respect the in app switch */
class Haptics(private val view: View, private val enabled: Boolean) {
    fun tick() = perform(if (Build.VERSION.SDK_INT >= 27) HapticFeedbackConstants.KEYBOARD_RELEASE else HapticFeedbackConstants.KEYBOARD_TAP)
    fun click() = perform(HapticFeedbackConstants.VIRTUAL_KEY)
    fun long() = perform(HapticFeedbackConstants.LONG_PRESS)
    fun confirm() = perform(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
    fun reject() = perform(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)
    private fun perform(c: Int) {
        if (enabled) view.performHapticFeedback(c)
    }
}

val LocalHaptics = staticCompositionLocalOf<Haptics> { error("no haptics") }

@Composable
fun KralaTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    dynamic: Boolean = false,
    haptics: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val amoled = mode == ThemeMode.AMOLED
    val context = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkKrala
        else -> LightKrala
    }.let { if (amoled) it.amoled() else it }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val h = remember(view, haptics) { Haptics(view, haptics) }
    CompositionLocalProvider(LocalHaptics provides h) {
        MaterialTheme(colorScheme = scheme, typography = KralaType, shapes = KralaShapes, content = content)
    }
}
