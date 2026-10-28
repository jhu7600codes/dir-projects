package dev.jhu.crossi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

val CrossiTypography = base.copy(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 6.sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
)

/** Letters inside grid cells. Size gets scaled by the grid composable. */
val CellLetter = TextStyle(fontWeight = FontWeight.Bold)
