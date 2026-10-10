package com.simulasim.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0E0F14)
val Surface1 = Color(0xFF181A22)
val Surface2 = Color(0xFF22252F)
val Accent = Color(0xFF7C5CFF)
val Green = Color(0xFF2EE59D)
val Red = Color(0xFFFF4D5E)
val Yellow = Color(0xFFFFC94D)
val Blue = Color(0xFF4DA8FF)
val Orange = Color(0xFFFF8A3D)
val TextMain = Color(0xFFF2F3F7)
val TextDim = Color(0xFF9AA0B4)

// dark only, same call as spy: it's a toy box, it should look like one at 2am too
@Composable
fun SimTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color.White,
            secondary = Green,
            background = Bg,
            onBackground = TextMain,
            surface = Surface1,
            onSurface = TextMain,
            surfaceVariant = Surface2,
            onSurfaceVariant = TextDim,
            error = Red,
        ),
        content = content,
    )
}
