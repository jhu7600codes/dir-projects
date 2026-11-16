package com.patternbox.app

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val FallbackDark = darkColorScheme(
    primary = Color(0xFFFF8A65), onPrimary = Color(0xFF3B0E00),
    primaryContainer = Color(0xFF5A1F0A), onPrimaryContainer = Color(0xFFFFDBCF),
    tertiary = Color(0xFF7CC4FF), onTertiary = Color(0xFF00325A),
    tertiaryContainer = Color(0xFF004880), onTertiaryContainer = Color(0xFFD1E4FF),
)
private val FallbackLight = lightColorScheme(
    primary = Color(0xFFB4421C), tertiary = Color(0xFF00629F),
    primaryContainer = Color(0xFFFFDBCF), tertiaryContainer = Color(0xFFD1E4FF),
)

/** material you: on android 12+ everything (fighters included) takes the wallpaper colours */
@Composable
fun PatternboxTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> FallbackDark
        else -> FallbackLight
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** p1 = primary, p2 = tertiary, so the two sides always look different but still match the wallpaper */
@Composable
fun playerColor(i: Int): Color = if (i == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

@Composable
fun playerContainer(i: Int): Color = if (i == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer

@Composable
fun onPlayerContainer(i: Int): Color = if (i == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
