package top.zwtx.daysmatter.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val ink = Color(0xFF242B36)
private val slate = Color(0xFF425A78)

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
  primary = slate,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE4EBF2),
  onPrimaryContainer = ink,
  secondary = Color(0xFFA7664C),
  background = Color(0xFFF6F5F2),
  surface = Color(0xFFFFFFFF),
  surfaceVariant = Color(0xFFF0EFEC),
  outlineVariant = Color(0xFFE2E1DD),
  onBackground = ink,
  onSurface = ink,
  onSurfaceVariant = Color(0xFF646B74)
)

private val darkColors = darkColorScheme(
  primary = Color(0xFFB9CDE8),
  onPrimary = Color(0xFF22354F),
  primaryContainer = Color(0xFF33465D),
  onPrimaryContainer = Color(0xFFEAF2FF),
  secondary = Color(0xFFE4AB91),
  background = Color(0xFF151A21),
  surface = Color(0xFF202731),
  surfaceVariant = Color(0xFF29313D),
  outlineVariant = Color(0xFF3A4552),
  onBackground = Color(0xFFF1F3F5),
  onSurface = Color(0xFFF1F3F5),
  onSurfaceVariant = Color(0xFFBBC4CF)
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
    typography = appTypography,
    content = content
  )
}
