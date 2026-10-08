package top.zwtx.daysmatter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun GlassPanel(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit
) {
  val shape = MaterialTheme.shapes.large
  val colors = MaterialTheme.colorScheme
  val clickModifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
  Box(
    modifier.fillMaxWidth()
      .shadow(2.dp, shape, ambientColor = colors.onSurface.copy(alpha = 0.04f))
      .clip(shape)
      .background(colors.surface)
      .border(1.dp, colors.outlineVariant, shape)
      .then(clickModifier),
  ) {
    CompositionLocalProvider(LocalContentColor provides colors.onSurface) { content() }
  }
}
