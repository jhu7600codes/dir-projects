package com.vanbank.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * VANBank only ever renders dark: black as the primary accent, near-black
 * panels, white/light text. Every Material3 color role is overridden below
 * so no default Material blue/purple ever leaks through.
 */
private val VanBankColorScheme = darkColorScheme(
    primary = VbAccent,
    onPrimary = VbOnAccent,
    primaryContainer = VbAccentDim,
    onPrimaryContainer = VbOnAccent,
    secondary = VbTextSecondary,
    onSecondary = VbBackground,
    background = VbBackground,
    onBackground = VbTextPrimary,
    surface = VbPanel,
    onSurface = VbTextPrimary,
    surfaceVariant = VbPanelElevated,
    onSurfaceVariant = VbTextSecondary,
    surfaceContainer = VbPanel,
    surfaceContainerHigh = VbPanelElevated,
    surfaceContainerHighest = VbPanelElevated,
    outline = VbPanelBorder,
    outlineVariant = VbPanelBorder,
    error = VbNegative,
    onError = VbBackground,
)

@Composable
fun VanBankTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = VbBackground.toArgb()
            window.navigationBarColor = VbBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = VanBankColorScheme,
        typography = VanBankTypography,
        shapes = VanBankShapes,
        content = content,
    )
}
