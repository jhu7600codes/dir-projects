package app.lineagifier.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Teal = Color(0xFF167C80)
val TealLight = Color(0xFF6FC7C9)
val Bg = Color(0xFF0E1213)
val Surface1 = Color(0xFF172021)
val Bad = Color(0xFFE57373)
val Good = Color(0xFF81C784)
val Dim = Color(0xFF8FA3A4)

private val scheme = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF00201F),
    primaryContainer = Teal,
    onPrimaryContainer = Color.White,
    secondary = TealLight,
    background = Bg,
    onBackground = Color(0xFFE1E6E6),
    surface = Surface1,
    onSurface = Color(0xFFE1E6E6),
    surfaceVariant = Color(0xFF1F2B2C),
    onSurfaceVariant = Dim,
    error = Bad,
)

@Composable
fun LineageTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
