package top.zwtx.daysmatter.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import top.zwtx.daysmatter.BuildConfig
import top.zwtx.daysmatter.MainViewModel
import top.zwtx.daysmatter.widget.ImportantDayWidgetProvider
import top.zwtx.daysmatter.widget.WidgetStore

@Composable
fun WidgetManagementScreen(vm: MainViewModel, onConfigure: (Int) -> Unit, padding: PaddingValues) {
  val context = LocalContext.current
  val manager = remember(context) { AppWidgetManager.getInstance(context) }
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  var revision by remember { mutableIntStateOf(0) }
  DisposableEffect(lifecycle) {
    val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
    lifecycle.addObserver(observer)
    onDispose { lifecycle.removeObserver(observer) }
  }
  val ids = remember(revision) { ImportantDayWidgetProvider.instanceIds(context) }
  Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(AppDimens.pageGutter),
    verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
    Text("重要日子", style = MaterialTheme.typography.titleLarge)
    Text("长按桌面空白处，选择小组件 → Days Matter → 重要日子，再选择一个主事件。可添加多个实例，分别关注不同事件。")
    Text("内容会在桌面可见；每个实例默认跟随系统外观。通知权限关闭也可以显示。系统允许刷新时更新，不保证零点精确切换。")
    if (manager.isRequestPinAppWidgetSupported) Button(onClick = {
      if (!manager.requestPinAppWidget(ComponentName(context, ImportantDayWidgetProvider::class.java), null, null)) {
        vm.showMessage("当前桌面未接受添加请求，请长按桌面手动添加")
      }
    }) { Text("请求添加重要日子") }
    Text("已有实例", style = MaterialTheme.typography.titleMedium)
    if (ids.isEmpty()) Text("尚未放置重要日子小组件")
    ids.forEach { id ->
      val target = WidgetStore(context).binding(id)
      val owned = target?.userId == vm.session?.userId && target?.backend == BuildConfig.API_BASE_URL.trimEnd('/')
      val name = if (owned) vm.snapshot?.events?.find { it.id == target?.eventId }?.name ?: "事件不可用，请重新选择"
        else "请登录所属账号或重新配置"
      GlassPanel {
        Column(Modifier.fillMaxWidth().padding(AppDimens.cardInset), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("实例 $id · $name")
          TextButton(onClick = { onConfigure(id) }) { Text("配置实例 $id") }
        }
      }
    }
  }
}
