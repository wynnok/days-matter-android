package top.zwtx.daysmatter.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import top.zwtx.daysmatter.BuildConfig
import top.zwtx.daysmatter.R

@Composable
fun HelpScreen(contentPadding: PaddingValues) {
  val context = LocalContext.current
  var showNotices by remember { mutableStateOf(false) }
  val notices = remember { context.resources.openRawResource(R.raw.third_party_notices).bufferedReader().use { it.readText() } }
  SettingsPage(contentPadding) {
    SettingsHero(Icons.Outlined.HelpOutline, "使用指南", "让日子更好用", "日期、备份与提醒，按需了解。")
    Text("日子与日期", style = MaterialTheme.typography.titleSmall)
    SettingsDisclosure("分类", "在首页侧栏管理和筛选事件") {
      Text("分类用于组织事件。首页筛选不改变账号事件总数；删除仍有事件的分类前，需要先移动或删除这些事件。", style = MaterialTheme.typography.bodyMedium)
    }
    SettingsDisclosure("日期、农历与重复", "今天、还有 N 天、已过 N 天，以东八区为准") {
      Text("按东八区的日历日期计算天数。近期 7 天与 30 天，是从今天开始的滚动窗口。", style = MaterialTheme.typography.bodyMedium)
      SettingsNote("农历转换和下一重复周期日期由服务端确认。过期的重复日期或未知日期会显示待同步；联网同步后才会确认下一次日期。")
    }
    Text("数据与提醒", style = MaterialTheme.typography.titleSmall)
    SettingsDisclosure("数据状态", "失败时保留缓存，成功时间代表已取得的数据") {
      Text("上次成功获取时间代表已取得的账号数据。其他设备的修改，可能需要再次同步后才会显示。", style = MaterialTheme.typography.bodyMedium)
      SettingsNote("日期天数的跨日推进，不代表已同步新数据，也不代表已生成新提醒。")
    }
    SettingsDisclosure("账号备份", "导入是追加，不覆盖现有数据") {
      Text("JSON 备份包含分类、渠道信息、主事件和子事件。导入会追加数据，可能重复；文件预览数量不是实际导入数量。", style = MaterialTheme.typography.bodyMedium)
      SettingsNote("可能包含 Webhook 地址与凭据，请妥善保管。不包含账号资料、本地提醒、外观或小组件实例。")
    }
    SettingsDisclosure("站外提醒", "Webhook 渠道属于账号，本地提醒属于设备") {
      Text("Webhook 渠道与设备本地提醒分别配置。停用渠道会暂停站外发送；被开启提醒的事件引用时，不可删除。", style = MaterialTheme.typography.bodyMedium)
      SettingsNote("测试请求完成，只表示请求已处理。实际送达请检查接收端。")
    }
    SettingsDisclosure("桌面小组件", "添加、调整内容、配色与更新说明") {
      Text("长按桌面空白处 → 小组件 → Days Matter，选择重要日子或近期日程并完成配置。无事件时，先回首页创建。", style = MaterialTheme.typography.bodyMedium)
      Text("支持的桌面可长按已有小组件重新配置，也可从我的 → 桌面小组件调整。取消保留原配置；事件或分类删除后需重新选择。每个小组件有独立外观，跟随系统以系统主题为准。", style = MaterialTheme.typography.bodyMedium)
      SettingsNote("名称和日期会在桌面可见；账号退出或切换会遮蔽旧内容。长名称会截断，轻点可查看完整内容。无需通知权限。")
      SettingsNote("失败保留缓存和上次成功时间；联网后可手动刷新。系统允许时共享账号同步，重启和时间变化会重新读取。省电、后台限制及不同桌面可能延迟刷新，不保证零点或实时更新。")
    }
    SettingsSection("版本与声明") {
      Text("Days Matter Android", style = MaterialTheme.typography.titleMedium)
      Text("版本 ${BuildConfig.VERSION_NAME} · 构建 ${BuildConfig.VERSION_CODE}", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
      OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/wynnok/days-matter-android"))) },
        modifier = Modifier.fillMaxWidth().heightIn(min = AppDimens.controlHeight)) { Text("打开项目与许可声明") }
      OutlinedButton(onClick = { showNotices = !showNotices }, modifier = Modifier.fillMaxWidth().heightIn(min = AppDimens.controlHeight)) {
        Text(if (showNotices) "收起第三方许可" else "查看第三方许可")
      }
      if (showNotices) Text(notices, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      SettingsNote("AndroidX、Kotlin／kotlinx.coroutines、Haze 使用 Apache 2.0；cn.6tail:lunar 使用 MIT；分类图标源自 Lucide（ISC）。版权归各作者。")
    }
  }
}
