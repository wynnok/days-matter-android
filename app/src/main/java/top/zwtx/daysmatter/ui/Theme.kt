package top.zwtx.daysmatter.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val ink = Color(0xFF1D3044)
private val blue = Color(0xFF1769C2)

object AppDimens {
  val pageGutter = 20.dp
  val cardInset = 18.dp
  val itemGap = 12.dp
  val sectionGap = 20.dp
  val controlHeight = 48.dp
  val fieldHeight = 56.dp
  val drawerOuterGutter = 12.dp
  val drawerInnerGutter = 8.dp
  val drawerCardInset = 12.dp
}

private val lightColors = lightColorScheme(
  primary = blue,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE2F1FF),
  onPrimaryContainer = ink,
  secondary = Color(0xFFC64F3E),
  background = Color(0xFFF8FBFF),
  surface = Color(0xFFFFFFFF),
  surfaceVariant = Color(0xFFEDF6FF),
  outlineVariant = Color(0xFFD8E7F4),
  onBackground = ink,
  onSurface = ink,
  onSurfaceVariant = Color(0xFF52677D)
)

private val darkColors = darkColorScheme(
  primary = Color(0xFF89C9FF),
  onPrimary = Color(0xFF123251),
  primaryContainer = Color(0xFF254861),
  onPrimaryContainer = Color(0xFFE2F2FF),
  secondary = Color(0xFFFFAE90),
  background = Color(0xFF152A3D),
  surface = Color(0xFF1D3549),
  surfaceVariant = Color(0xFF254157),
  outlineVariant = Color(0xFF3B5B70),
  onBackground = Color(0xFFF0F8FF),
  onSurface = Color(0xFFF0F8FF),
  onSurfaceVariant = Color(0xFFC3D6E5)
)

private val appShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp)
)

private val appTypography = Typography(
  headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold),
  headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold),
  titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
  titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
  titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
  bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
  bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
  bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
  labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
  labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun DaysMatterTheme(dark: Boolean, content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = if (dark) darkColors else lightColors,
    shapes = appShapes,
    typography = appTypography
  ) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) { content() }
  }
}
