package io.github.wynnok.daysmatter.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val orange = Color(0xFFFF6B35)

private val lightColors = lightColorScheme(
  primary = orange,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFFFE3D5),
  onPrimaryContainer = Color(0xFF632000),
  secondary = Color(0xFF0C8599),
  background = Color(0xFFF6F7F9),
  surface = Color.White,
  surfaceVariant = Color(0xFFEEF0F3),
  onBackground = Color(0xFF1A1D21),
  onSurface = Color(0xFF1A1D21)
)

private val darkColors = darkColorScheme(
  primary = Color(0xFFFF966F),
  onPrimary = Color(0xFF52200E),
  primaryContainer = Color(0xFF763820),
  onPrimaryContainer = Color(0xFFFFDACC),
  secondary = Color(0xFF79D2E0),
  background = Color(0xFF101114),
  surface = Color(0xFF1A1C20),
  surfaceVariant = Color(0xFF26282D),
  onBackground = Color(0xFFF0F1F3),
  onSurface = Color(0xFFF0F1F3)
)

@Composable
fun DaysMatterTheme(dark: Boolean, content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = if (dark) darkColors else lightColors, content = content)
}
