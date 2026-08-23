package com.fivepesos.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 5 Pesos is always this flat gray-blue -- it's the exact color baked into
// the coin photography and the launcher icon, so there's deliberately no
// separate light theme to keep everything one consistent look.
private val FivePesosColorScheme = darkColorScheme(
    primary = PesosOnBackground,
    onPrimary = PesosBackground,
    background = PesosBackground,
    onBackground = PesosOnBackground,
    surface = PesosCardSurface,
    onSurface = PesosOnCardSurface,
    surfaceVariant = PesosBackground,
    onSurfaceVariant = PesosOnBackground,
    outline = PesosOnBackground,
)

@Composable
fun FivePesosTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PesosBackground.toArgb()
            window.navigationBarColor = PesosBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = FivePesosColorScheme,
        typography = FivePesosTypography,
        content = content,
    )
}
