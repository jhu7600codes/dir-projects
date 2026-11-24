package com.patternbox.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FallbackDark = darkColorScheme(
    primary = Color(0xFFFF8A65), onPrimary = Color(0xFF3B0E00),
    primaryContainer = Color(0xFF5A1F0A), onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFE7BDB0), secondaryContainer = Color(0xFF5D4037),
    tertiary = Color(0xFF7CC4FF), onTertiary = Color(0xFF00325A),
    tertiaryContainer = Color(0xFF004880), onTertiaryContainer = Color(0xFFD1E4FF),
)
private val FallbackLight = lightColorScheme(
    primary = Color(0xFFB4421C), tertiary = Color(0xFF00629F), secondary = Color(0xFF77574C),
    primaryContainer = Color(0xFFFFDBCF), tertiaryContainer = Color(0xFFD1E4FF),
)

/** material you: on android 12+ everything (fighters included) takes the wallpaper colours */
@Composable
fun PatternboxTheme(state: GameState, content: @Composable () -> Unit) {
    val dark = when (state.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val wallpaper = if (state.dynamicColor) LocalPlatform.current.dynamicScheme(dark) else null
    val scheme = wallpaper ?: if (dark) FallbackDark else FallbackLight
    MaterialTheme(colorScheme = scheme, content = content)
}

/** six team colours. the first three come from the wallpaper, the rest are fixed so ffa stays readable */
fun teamColor(scheme: ColorScheme, team: Int): Color = when (((team % 6) + 6) % 6) {
    0 -> scheme.primary
    1 -> scheme.tertiary
    2 -> scheme.secondary
    3 -> Color(0xFF6FCF8E)
    4 -> Color(0xFFE5B93C)
    else -> Color(0xFFC77DFF)
}

fun teamContainer(scheme: ColorScheme, team: Int): Color = when (((team % 6) + 6) % 6) {
    0 -> scheme.primaryContainer
    1 -> scheme.tertiaryContainer
    2 -> scheme.secondaryContainer
    else -> teamColor(scheme, team).copy(alpha = .3f)
}

fun onTeamContainer(scheme: ColorScheme, team: Int): Color = when (((team % 6) + 6) % 6) {
    0 -> scheme.onPrimaryContainer
    1 -> scheme.onTertiaryContainer
    2 -> scheme.onSecondaryContainer
    else -> scheme.onSurface
}

@Composable
fun playerColor(i: Int): Color = teamColor(MaterialTheme.colorScheme, i)
