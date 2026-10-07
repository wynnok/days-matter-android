package top.zwtx.daysmatter.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import top.zwtx.daysmatter.MainViewModel
import top.zwtx.daysmatter.reminder.ReminderNotifications
import top.zwtx.daysmatter.reminder.reminderTiming
import top.zwtx.daysmatter.reminder.label
import java.time.Instant

@Composable
fun LocalReminderScreen(vm: MainViewModel, onConfigure: (Int) -> Unit, contentPadding: PaddingValues, displayInstant: Instant = Instant.now()) {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val notifications = remember(context) { ReminderNotifications(context).also { it.createChannel() } }
  var revision by remember { mutableIntStateOf(0) }
  val now = displayInstant
  DisposableEffect(lifecycle) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) revision++
    }
    lifecycle.addObserver(observer)
    onDispose { lifecycle.removeObserver(observer) }
  }
  val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { revision++ }
  val granted = remember(revision) { notifications.permissionGranted }
  val enabled = remember(revision) { notifications.appEnabled }
  val channel = remember(revision) { notifications.channelEnabled }
  Column(Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
    .padding(AppDimens.pageGutter), verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
    Text("当前设备的本地提醒", style = MaterialTheme.typography.titleLarge)
    Text("通知权限：${if (granted) "已授予" else "未授予"}")
    Text("系统应用通知：${if (enabled) "已开启" else "已关闭"}")
    Text("事件提醒渠道：${if (channel) "已开启" else "已关闭"}")
    if (!granted && Build.VERSION.SDK_INT >= 33) {
      OutlinedButton(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("申请通知权限") }
    }
    OutlinedButton(onClick = {
      context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }) { Text("系统通知设置") }
    Button(onClick = { vm.showMessage(notifications.test()); revision++ }) { Text("测试通知") }
    Text("测试只验证当前通知展示，不保证未来精确送达。省电、后台限制或系统延迟可能影响定时提醒；请检查通知权限、事件提醒渠道和电池设置。")
    HorizontalDivider()
    Text("已启用提醒的事件", style = MaterialTheme.typography.titleMedium)
    val events = vm.snapshot?.events?.filter { vm.localReminder(it.id).enabled }
    if (events == null) Text("尚无已同步数据，请先同步以查看本地提醒")
    else if (events.isEmpty()) Text("尚未启用本地提醒，可在事件编辑页逐事件设置")
    events?.forEach { event ->
      GlassPanel {
        Column(Modifier.fillMaxWidth().padding(AppDimens.cardInset), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(event.name, style = MaterialTheme.typography.titleMedium)
          Text(reminderTiming(event, vm.localReminder(event.id), now).label())
          TextButton(onClick = { onConfigure(event.id) }) { Text("配置 ${event.name} 的本地提醒") }
        }
      }
    }
    Text("本地提醒只属于当前设备；站外提醒渠道属于账号，分别管理。")
    TextButton(onClick = vm::refresh, enabled = !vm.busy) { Text("同步事件") }
  }
}
