package top.zwtx.daysmatter.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
  DisposableEffect(lifecycle) {
    val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
    lifecycle.addObserver(observer)
    onDispose { lifecycle.removeObserver(observer) }
  }
  val manager = AppWidgetManager.getInstance(context)
  val importantIds = remember(revision) { ImportantDayWidgetProvider.instanceIds(context) }
  val upcomingIds = remember(revision) { UpcomingWidgetProvider.instanceIds(context) }
  val widgets = remember { WidgetStore(context) }
  Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(AppDimens.pageGutter),
    verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
    Text("桌面小组件", style = MaterialTheme.typography.titleLarge)
    Text("长按桌面空白处 → 小组件 → Days Matter，选择重要日子或近期日程并完成配置。可添加多个实例。")
    Text("名称和日期会在桌面可见。通知权限关闭也可以显示。每个实例独立选择浅色、深色或跟随系统，不改变应用外观与事件置顶。")
    val session = vm.session
    val snapshot = vm.snapshot
    if (session == null || snapshot == null) Text("请先登录并同步，再添加小组件")
    else {
      val event = snapshot.events.firstOrNull()
      Text("重要日子预览", style = MaterialTheme.typography.titleMedium)
      if (event == null) Text("暂无主事件，请先回首页创建事件")
      else AndroidView(factory = { FrameLayout(it) }, modifier = Modifier.fillMaxWidth().height(180.dp), update = { frame ->
        val preview = ImportantDayWidgetProvider.render(context, -1,
          target = EventTarget(BuildConfig.API_BASE_URL, session.userId, event.id))
        frame.removeAllViews(); frame.addView(preview.apply(frame.context, frame))
      })
      Text("近期日程预览 · 默认近期 30 天", style = MaterialTheme.typography.titleMedium)
      AndroidView(factory = { FrameLayout(it) }, modifier = Modifier.fillMaxWidth().height(200.dp), update = { frame ->
        val preview = UpcomingWidgetProvider.render(context, -2, binding = UpcomingBinding(BuildConfig.API_BASE_URL, session.userId))
        frame.removeAllViews(); frame.addView(preview.apply(frame.context, frame))
      })
    }
    Text("已有实例", style = MaterialTheme.typography.titleMedium)
    if (importantIds.isEmpty() && upcomingIds.isEmpty()) Text("尚未放置桌面小组件")
    (importantIds + upcomingIds).forEach { id ->
      val recent = upcomingIds.contains(id)
      val target = widgets.binding(id)
      val binding = widgets.upcoming(id)
      val owned = if (recent) binding?.userId == session?.userId && binding?.backend == BuildConfig.API_BASE_URL.trimEnd('/')
        else target?.userId == session?.userId && target?.backend == BuildConfig.API_BASE_URL.trimEnd('/')
      val name = when {
        !owned -> "请登录所属账号或重新配置"
        recent -> "近期 ${binding!!.window} 天 · ${if (binding.categoryId == 0) "全部" else snapshot?.categories?.find { it.id == binding.categoryId }?.name ?: "分类已删除，请重选"}"
        else -> snapshot?.events?.find { it.id == target?.eventId }?.name ?: "事件不可用，请重新选择"
      }
      GlassPanel {
        Column(Modifier.fillMaxWidth().padding(AppDimens.cardInset), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("${if (recent) "近期日程" else "重要日子"} · 实例 $id · $name")
          val info = manager.getAppWidgetInfo(id)
          val systemReconfigure = Build.VERSION.SDK_INT >= 28 && info != null &&
            info.widgetFeatures and android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE != 0
          if (systemReconfigure) Text("支持的桌面可长按此实例，选择重新配置。")
          TextButton(onClick = {
            if (systemReconfigure && info?.configure != null) context.startActivity(
              ImportantDayWidgetProvider.configurationIntent(context, id).setComponent(info.configure))
            else onConfigure(id)
          }) { Text("配置实例 $id") }
        }
      }
    }
    Text("拖动桌面边框调整尺寸；长名称可点击查看完整内容。省电和后台限制可能延迟刷新，上次同步时间以显示为准，不保证零点精确切换。更多排查见帮助与关于 → 桌面小组件。")
  }
}
