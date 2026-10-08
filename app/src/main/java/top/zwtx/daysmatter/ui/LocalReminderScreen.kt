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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Sync
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
  val events = vm.snapshot?.events?.filter { vm.localReminder(it.id).enabled }
  val ready = granted && enabled && channel
  val openSettings: () -> Unit = {
    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
  }
  val testNotification: () -> Unit = { vm.showMessage(notifications.test()); revision++ }
  SettingsPage(contentPadding) {
    SettingsHero(Icons.Outlined.NotificationsActive, "当前设备", if (ready) "通知已就绪" else "通知需要设置",
      "为重要的日子，留一声及时的提醒。")
    SettingsSection("通知状态") {
      SettingsStatus("通知权限", if (granted) "已授予" else "未授予", granted)
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      SettingsStatus("系统应用通知", if (enabled) "已开启" else "已关闭", enabled)
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      SettingsStatus("事件提醒渠道", if (channel) "已开启" else "已关闭", channel)
      if (!granted && Build.VERSION.SDK_INT >= 33) {
        Button(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) },
          modifier = Modifier.fillMaxWidth().heightIn(min = AppDimens.controlHeight)) { Text("申请通知权限") }
      }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = openSettings, modifier = Modifier.weight(1f).heightIn(min = AppDimens.controlHeight),
          contentPadding = PaddingValues(horizontal = 8.dp)) { Text("系统通知设置") }
        if (ready) Button(onClick = testNotification, modifier = Modifier.weight(1f).heightIn(min = AppDimens.controlHeight)) { Text("测试通知") }
        else OutlinedButton(onClick = testNotification, modifier = Modifier.weight(1f).heightIn(min = AppDimens.controlHeight)) { Text("测试通知") }
      }
    }
    SettingsSection("已启用提醒的事件", "${events?.size ?: 0} 个") {
      when {
        events == null -> SettingsEmpty(Icons.Outlined.NotificationsNone, "尚无已同步数据", "同步事件后，可查看本地提醒。")
        events.isEmpty() -> SettingsEmpty(Icons.Outlined.NotificationsNone, "还没有启用提醒", "在事件编辑页，为需要的日子开启本地提醒。")
        else -> events.forEachIndexed { index, event ->
          if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(event.name, style = MaterialTheme.typography.titleMedium)
            Text(reminderTiming(event, vm.localReminder(event.id), now).label(), style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { onConfigure(event.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = AppDimens.controlHeight)) {
              Text("配置 ${event.name} 的本地提醒")
            }
          }
        }
      }
      OutlinedButton(onClick = vm::refresh, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().heightIn(min = AppDimens.controlHeight)) {
        Icon(Icons.Outlined.Sync, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (vm.refreshing) "同步中…" else "同步事件")
      }
    }
    SettingsDisclosure("提醒为什么没有出现？", "检查通知设置、后台运行与省电限制") {
      Text("测试只验证当前通知展示，不保证未来精确送达。", style = MaterialTheme.typography.bodyMedium)
      SettingsNote("省电、后台限制或系统延迟可能影响定时提醒；请检查通知权限、事件提醒渠道和电池设置。")
    }
    SettingsNote("本地提醒只属于当前设备；站外提醒渠道属于账号，分别管理。")
  }
}
