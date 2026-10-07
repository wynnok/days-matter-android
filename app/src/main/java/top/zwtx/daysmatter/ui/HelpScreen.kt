package top.zwtx.daysmatter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.zwtx.daysmatter.BuildConfig
import top.zwtx.daysmatter.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember

@Composable
fun HelpScreen(contentPadding: PaddingValues) {
  val context = LocalContext.current
  val notices = remember { context.resources.openRawResource(R.raw.third_party_notices).bufferedReader().use { it.readText() } }
  Column(Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text("帮助与关于", style = MaterialTheme.typography.headlineMedium)
    listOf(
      "分类" to "分类用于组织事件。首页筛选不改变账号事件总数；删除仍有事件的分类前需要处理这些事件。",
      "日期、农历与重复" to "按东八区日历日期显示天数：今天、还有 N 天或已过 N 天。近期 7 天与 30 天是从今天开始的滚动窗口。农历转换和下一重复周期日期由服务端确认，过期重复日期或未知日期显示待同步。",
      "数据状态" to "上次成功获取时间代表已取得的账号数据。失败时保留缓存，其他设备的修改可能尚未获取；日期天数推进不代表已同步新数据或新提醒。",
      "账号备份" to "JSON 备份包含分类、渠道信息、主事件和子事件，可能包含 Webhook 地址与凭据，请妥善保管。它不包含账号资料、本地提醒、外观或小组件实例。导入是追加，不覆盖，可能重复；预览数量不是实际导入数量。",
      "站外提醒" to "Webhook 渠道属于账号，与设备本地提醒分别配置。停用渠道会暂停站外发送；被开启提醒的事件引用时不可删除。测试请求完成仅表示请求已处理，请检查接收端。",
      "桌面小组件" to "长按桌面空白处 → 小组件 → Days Matter，选择重要日子或近期日程并完成配置。名称和日期会在桌面可见；无事件时先创建。支持的桌面可长按已有实例重新配置，也可从我的 → 桌面小组件调整对应实例，适用于低版本或无系统重配入口的桌面。取消保留原配置。事件或分类删除后需重新选择；每个实例有独立外观，跟随系统以系统主题为准。拖动边框调整尺寸，长名称会截断，点击进入应用查看完整内容。失败保留缓存和上次成功时间；联网后手动刷新。账号退出或切换会遮蔽旧内容。系统省电、后台限制及不同桌面可能延迟刷新，不保证零点或实时更新；无需通知权限。",
      "版本与声明" to "Days Matter Android ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）。项目：https://github.com/wynnok/days-matter-android。AndroidX、Kotlin／kotlinx.coroutines、Haze 使用 Apache 2.0；cn.6tail:lunar 使用 MIT；分类图标源自 Lucide（ISC）。第三方库版权归其作者；许可正文及版权声明见项目 THIRD_PARTY_NOTICES.md。"
    ).forEach { (title, text) ->
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(text, style = MaterialTheme.typography.bodyMedium)
    }
    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/wynnok/days-matter-android"))) }) { Text("打开项目与许可声明") }
    Text(notices, style = MaterialTheme.typography.bodySmall)
  }
}
