package dev.nyaaneo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF00E5FF)
val NeoColors = darkColorScheme(
    primary = NeonCyan, onPrimary = Color.Black,
    primaryContainer = Color(0xFF00383F), onPrimaryContainer = Color(0xFF9CF1FF),
    secondary = Color(0xFF76D5E3), onSecondary = Color.Black,
    background = Color.Black, onBackground = Color(0xFFE5F4F6),
    surface = Color(0xFF080E11), onSurface = Color(0xFFE5F4F6),
    surfaceVariant = Color(0xFF162329), onSurfaceVariant = Color(0xFFA5BDC4),
    outline = Color(0xFF38515A), error = Color(0xFFFFB4AB),
)

@Composable
fun NeoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NeoColors, content = content)
}
