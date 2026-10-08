package top.zwtx.daysmatter.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import top.zwtx.daysmatter.BuildConfig
import top.zwtx.daysmatter.EventTarget
import top.zwtx.daysmatter.MainViewModel
import top.zwtx.daysmatter.widget.*

@Composable
fun WidgetManagementScreen(vm: MainViewModel, onConfigure: (Int) -> Unit, padding: PaddingValues) {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  var revision by remember { mutableIntStateOf(0) }
  var recentPreview by remember { mutableStateOf(false) }
  DisposableEffect(lifecycle) {
    val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
    lifecycle.addObserver(observer)
    onDispose { lifecycle.removeObserver(observer) }
  }
  val manager = AppWidgetManager.getInstance(context)
  val importantIds = remember(revision) { ImportantDayWidgetProvider.instanceIds(context) }
  val upcomingIds = remember(revision) { UpcomingWidgetProvider.instanceIds(context) }
  val widgets = remember { WidgetStore(context) }
  val session = vm.session
  val snapshot = vm.snapshot
  val count = importantIds.size + upcomingIds.size
  SettingsPage(padding) {
    SettingsHero(Icons.Outlined.Widgets, "桌面小组件", if (count > 0) "已添加 $count 个小组件" else "把期待放在桌面",
      "一个日子的倒数，或接下来的一份日程。")
    SettingsSection("样式预览") {
      Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(false to "重要日子", true to "近期日程").forEach { (recent, label) ->
          Surface(modifier = Modifier.weight(1f).selectable(recentPreview == recent, role = Role.RadioButton,
            onClick = { recentPreview = recent }), shape = MaterialTheme.shapes.small,
            color = if (recentPreview == recent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
            Box(Modifier.heightIn(min = 44.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
              Text(label, style = MaterialTheme.typography.labelLarge,
                color = if (recentPreview == recent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
      if (session == null || snapshot == null) {
        SettingsEmpty(Icons.Outlined.Widgets, "先同步，看看你的日子", "请先登录并同步，再添加小组件。")
      } else if (!recentPreview && snapshot.events.isEmpty()) {
        SettingsEmpty(Icons.Outlined.Widgets, "还没有可预览的事件", "暂无主事件，请先回首页创建事件。")
      } else {
        WidgetPreview(Modifier.fillMaxWidth().height(180.dp)) {
          if (recentPreview) UpcomingWidgetProvider.render(context, -2,
            binding = UpcomingBinding(BuildConfig.API_BASE_URL, session.userId), previewHeight = 180)
          else ImportantDayWidgetProvider.render(context, -1,
            target = EventTarget(BuildConfig.API_BASE_URL, session.userId, snapshot.events.first().id), previewSize = 320 to 180)
        }
      }
      Text(if (recentPreview) "未来 7 天或 30 天，可按分类筛选。" else "固定关注一个主事件，显示日期与倒数。",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SettingsSection("添加到桌面") {
      listOf("长按桌面空白处" to "打开桌面的“小组件”列表。",
        "找到 Days Matter" to "选择重要日子或近期日程。",
        "完成配置" to "选择事件或分类，保存到桌面。可添加多个小组件。").forEachIndexed { index, (title, detail) ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.size(28.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
              Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
          }
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
    SettingsSection("已添加的小组件", "$count 个") {
      if (count == 0) SettingsEmpty(Icons.Outlined.Widgets, "尚未放置桌面小组件", "添加后，可在这里分别调整内容与配色。")
      (importantIds + upcomingIds).forEachIndexed { index, id ->
        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val recent = upcomingIds.contains(id)
        val target = widgets.binding(id)
        val binding = widgets.upcoming(id)
        val owned = if (recent) binding?.userId == session?.userId && binding?.backend == BuildConfig.API_BASE_URL.trimEnd('/')
          else target?.userId == session?.userId && target?.backend == BuildConfig.API_BASE_URL.trimEnd('/')
        val name = when {
          !owned -> "请登录所属账号或重新配置"
          recent -> "未来 ${binding!!.window} 天 · ${if (binding.categoryId == 0) "全部分类" else snapshot?.categories?.find { it.id == binding.categoryId }?.name ?: "分类已删除，请重选"}"
          else -> snapshot?.events?.find { it.id == target?.eventId }?.name ?: "事件不可用，请重新选择"
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(name, style = MaterialTheme.typography.titleSmall)
          val mode = when (widgets.appearance(id)) {
            top.zwtx.daysmatter.data.AppearanceMode.SYSTEM -> "跟随系统"
            top.zwtx.daysmatter.data.AppearanceMode.LIGHT -> "浅色"
            top.zwtx.daysmatter.data.AppearanceMode.DARK -> "深色"
          }
          Text("${if (recent) "近期日程" else "重要日子"} · $mode", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
          val info = manager.getAppWidgetInfo(id)
          val systemReconfigure = Build.VERSION.SDK_INT >= 28 && info != null &&
            info.widgetFeatures and android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE != 0
          OutlinedButton(onClick = {
            if (systemReconfigure && info?.configure != null) context.startActivity(
              ImportantDayWidgetProvider.configurationIntent(context, id).setComponent(info.configure))
            else onConfigure(id)
          }, modifier = Modifier.fillMaxWidth().heightIn(min = AppDimens.controlHeight)) { Text("调整内容与外观") }
        }
      }
    }
    SettingsDisclosure("显示与更新", "每个小组件独立配色，支持调整尺寸") {
      SettingsNote("名称和日期会在桌面可见。通知权限关闭也可以显示。每个小组件独立选择浅色、深色或跟随系统，不改变应用外观与事件置顶。")
      SettingsNote("拖动桌面边框调整尺寸；长名称可点击查看完整内容。省电和后台限制可能延迟刷新，不保证零点或实时更新。")
      SettingsNote("支持的桌面可长按已有小组件重新配置；也可以在上方调整内容。")
    }
  }
}

/** Apply actual widget layouts without letting a preview launch their PendingIntents. */
@Composable
private fun WidgetPreview(modifier: Modifier, render: () -> RemoteViews) {
  AndroidView(factory = { FrameLayout(it).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS } },
    modifier = modifier, update = { frame ->
      val preview = render().apply(frame.context, frame)
      fun disableClicks(view: View) {
        view.setOnClickListener(null)
        view.isClickable = false
        if (view is ViewGroup) for (index in 0 until view.childCount) disableClicks(view.getChildAt(index))
      }
      disableClicks(preview)
      frame.removeAllViews()
      frame.addView(preview)
    })
}
