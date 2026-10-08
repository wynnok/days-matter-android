package top.zwtx.daysmatter.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.zwtx.daysmatter.data.AppearanceMode
import top.zwtx.daysmatter.data.Snapshot
import top.zwtx.daysmatter.ui.DaysMatterTheme

/** Presentation shared by initial setup and reconfiguration; saving remains in the activity. */
@Composable
internal fun WidgetConfigurationScreen(
  upcoming: Boolean,
  signedIn: Boolean,
  snapshot: Snapshot?,
  selected: Int,
  onSelectEvent: (Int) -> Unit,
  window: Int,
  onSelectWindow: (Int) -> Unit,
  categoryId: Int,
  onSelectCategory: (Int) -> Unit,
  appearance: AppearanceMode,
  onSelectAppearance: (AppearanceMode) -> Unit,
  error: String?,
  canSave: Boolean,
  onSave: () -> Unit,
  onCancel: () -> Unit,
  onOpenApp: () -> Unit,
  preview: @Composable () -> Unit
) {
  val dark = isSystemInDarkTheme()
  DaysMatterTheme(dark) {
    val colors = MaterialTheme.colorScheme.copy(
      background = Color(if (dark) 0xFF11151C else 0xFFF2F4F8),
      surface = Color(if (dark) 0xFF1C2029 else 0xFFFFFFFF),
      surfaceVariant = Color(if (dark) 0xFF282E3A else 0xFFEDF0F5),
      onSurface = Color(if (dark) 0xFFF3F5FA else 0xFF182234),
      onSurfaceVariant = Color(if (dark) 0xFF939EB0 else 0xFF7C8798),
      outlineVariant = Color(if (dark) 0xFF303744 else 0xFFE8ECF2),
      primary = Color(if (dark) 0xFF8AB4FF else 0xFF3478F6)
    )
    MaterialTheme(colorScheme = colors) {
      Surface(Modifier.fillMaxSize(), color = colors.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
          Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) { Text("取消") }
            Text("小组件设置", Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleMedium)
          }
          Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(if (upcoming) "近期日程" else "重要日子", style = MaterialTheme.typography.headlineMedium)
              Text(if (upcoming) "把接下来的期待，放在桌面。" else "为一个值得记住的日子，留一个位置。",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("桌面预览", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                Text("随选择实时更新", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
              }
              Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(colors.surfaceVariant).padding(16.dp)) { preview() }
            }
            ConfigurationSection("显示内容") {
              when {
                !signedIn -> SetupMessage("请先登录，再回来配置", "打开应用登录或同步", onOpenApp)
                snapshot == null -> SetupMessage("尚未同步，请先在应用中获取事件", "打开应用登录或同步", onOpenApp)
                upcoming -> {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(20.dp), tint = colors.primary)
                    Text("时间范围", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleSmall)
                  }
                  ChoiceSegments(listOf(7, 30), window, { "近期 $it 天" }, onSelectWindow)
                  HorizontalDivider(color = colors.outlineVariant)
                  CategorySelector(snapshot, categoryId, onSelectCategory)
                  if (categoryId != 0 && snapshot.categories.none { it.id == categoryId }) {
                    Text("分类已删除，请重新选择", style = MaterialTheme.typography.bodySmall, color = colors.error)
                  }
                  if (snapshot.events.isEmpty()) SetupMessage("暂无事件，请在应用中创建", "打开应用创建事件", onOpenApp)
                }
                snapshot.events.isEmpty() -> SetupMessage("添加一个重要日子", "打开应用创建事件", onOpenApp)
                else -> {
                  Text("选择事件", style = MaterialTheme.typography.titleSmall)
                  Column(Modifier.selectableGroup()) {
                    snapshot.events.forEachIndexed { index, event ->
                      Row(Modifier.fillMaxWidth().selectable(selected == event.id, role = Role.RadioButton,
                        onClick = { onSelectEvent(event.id) }).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                          Text(event.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                          val detail = listOf(event.categoryName, event.nextOccurrence ?: "日期待同步").filter { it.isNotBlank() }.joinToString(" · ")
                          Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        RadioButton(selected == event.id, onClick = null, modifier = Modifier.size(32.dp))
                      }
                      if (index < snapshot.events.lastIndex) HorizontalDivider(color = colors.outlineVariant)
                    }
                  }
                }
              }
            }
            ConfigurationSection("外观") {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Palette, null, Modifier.size(20.dp), tint = colors.primary)
                Text("小组件配色", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleSmall)
              }
              ChoiceSegments(AppearanceMode.entries, appearance, { when (it) {
                AppearanceMode.SYSTEM -> "跟随系统"
                AppearanceMode.LIGHT -> "浅色"
                AppearanceMode.DARK -> "深色"
              } }, onSelectAppearance)
              Text("仅应用于当前小组件", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Text(if (upcoming) "所选范围仅影响桌面列表；轻点“全部”可查看分类下的所有事件。事件名称和日期会在桌面可见。"
              else "事件名称和日期会在桌面可见。轻点小组件可查看事件详情。",
              style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
          }
          Surface(color = colors.background) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              error?.let { Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall) }
              Button(onClick = onSave, enabled = canSave, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("保存到桌面") }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ConfigurationSection(title: String, content: @Composable ColumnScope.() -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(title, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
      Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
  }
}

@Composable
private fun <T> ChoiceSegments(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
  Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
    .selectableGroup().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    options.forEach { value ->
      Surface(modifier = Modifier.weight(1f).selectable(selected == value, role = Role.RadioButton, onClick = { onSelect(value) }),
        shape = RoundedCornerShape(9.dp), shadowElevation = if (selected == value) 2.dp else 0.dp,
        color = if (selected == value) MaterialTheme.colorScheme.surface else Color.Transparent) {
        Box(Modifier.heightIn(min = 42.dp).padding(horizontal = 4.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
          Text(label(value), style = MaterialTheme.typography.labelLarge, fontWeight = if (selected == value) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@Composable
private fun CategorySelector(snapshot: Snapshot, selected: Int, onSelect: (Int) -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  val name = if (selected == 0) "全部分类" else snapshot.categories.find { it.id == selected }?.name ?: "重新选择分类"
  Box {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { expanded = true }
      .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
      Text("分类", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
      Text(name, Modifier.widthIn(max = 180.dp), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Icon(Icons.Outlined.ExpandMore, "选择分类", Modifier.padding(start = 6.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.widthIn(min = 240.dp, max = 320.dp)) {
      (listOf(0 to "全部分类") + snapshot.categories.map { it.id to it.name }).forEach { (id, label) ->
        DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(id); expanded = false },
          trailingIcon = { if (selected == id) Icon(Icons.Outlined.Check, "已选", tint = MaterialTheme.colorScheme.primary) })
      }
    }
  }
}

@Composable
private fun SetupMessage(message: String, action: String, onOpenApp: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = onOpenApp, contentPadding = PaddingValues(0.dp)) { Text(action) }
  }
}
