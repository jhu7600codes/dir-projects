package com.spy.game.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Spy is always dark -- it's a pass-the-phone party game meant to be played
// at a table, and the red-on-black look is part of the brand, not a user
// preference. There's deliberately no light color scheme.
private val SpyColorScheme = darkColorScheme(
    primary = SpyRed,
    onPrimary = SpyOnBackground,
    primaryContainer = SpyRedContainer,
    onPrimaryContainer = SpyOnBackground,
    secondary = SpyBlue,
    onSecondary = SpyOnBackground,
    background = SpyBackground,
    onBackground = SpyOnBackground,
    surface = SpySurface,
    onSurface = SpyOnBackground,
    surfaceVariant = SpySurfaceVariant,
    onSurfaceVariant = SpyOnSurfaceMuted,
    outline = SpyOutline,
    error = SpyRedDark,
    onError = SpyOnBackground,
)

@Composable
fun SpyTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SpyBackground.toArgb()
            window.navigationBarColor = SpyBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = SpyColorScheme,
        typography = SpyTypography,
        content = content,
    )
}
