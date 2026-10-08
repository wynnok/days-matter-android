package top.zwtx.daysmatter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Shared spacing, card hierarchy and action treatment for device/account settings. */
@Composable
internal fun SettingsPage(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
  Column(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)
    .verticalScroll(rememberScrollState()).padding(AppDimens.pageGutter),
    verticalArrangement = Arrangement.spacedBy(AppDimens.sectionGap)) {
    content()
    Spacer(Modifier.height(4.dp))
  }
}

@Composable
internal fun SettingsHero(icon: ImageVector, label: String, title: String, description: String) {
  val colors = MaterialTheme.colorScheme
  GlassPanel {
    Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.13f),
      colors.primary.copy(alpha = 0.03f)))).padding(20.dp), verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)) {
      Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(25.dp), tint = colors.primary) }
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.primary)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
      }
    }
  }
}

@Composable
internal fun SettingsSection(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
      subtitle?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    GlassPanel {
      Column(Modifier.fillMaxWidth().padding(AppDimens.cardInset),
        verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap), content = content)
    }
  }
}

@Composable
internal fun SettingsStatus(label: String, value: String, ready: Boolean) {
  Row(Modifier.fillMaxWidth().heightIn(min = 36.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    Surface(shape = RoundedCornerShape(8.dp), color = (if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error).copy(alpha = 0.1f)) {
      Text(value, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium,
        color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    }
  }
}

@Composable
internal fun SettingsNote(text: String) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Icon(Icons.Outlined.Info, null, Modifier.padding(top = 2.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
internal fun SettingsEmpty(icon: ImageVector, title: String, description: String) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
internal fun SettingsDisclosure(title: String, summary: String, initiallyExpanded: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
  var expanded by remember { mutableStateOf(initiallyExpanded) }
  GlassPanel {
    Column {
      TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
          Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
          if (expanded) "收起$title" else "展开$title", tint = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (expanded) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
      }
    }
  }
}

@Composable
internal fun BackupScope() {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("包含账号数据", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Text("分类 · 提醒渠道 · 主事件 · 子事件", style = MaterialTheme.typography.bodyMedium)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Text("不包含", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("账号资料 · 本地提醒 · 外观 · 小组件实例", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
internal fun BackupCounts(data: org.json.JSONObject) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    val counts = listOf("分类" to "categories", "渠道" to "remind_channels", "主事件" to "events", "子事件" to "sub_events")
    counts.chunked(2).forEach { pair ->
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        pair.forEach { (label, key) ->
          Surface(modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text("${data.optJSONArray(key)?.length() ?: 0}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
              Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }
  }
}
