package top.zwtx.daysmatter.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import top.zwtx.daysmatter.BuildConfig
import top.zwtx.daysmatter.EventTarget
import top.zwtx.daysmatter.MainActivity
import top.zwtx.daysmatter.data.LocalStore
import top.zwtx.daysmatter.ui.DaysMatterTheme

class WidgetConfigurationActivity : ComponentActivity() {
  private var revision by mutableIntStateOf(0)
  override fun onResume() { super.onResume(); revision++ }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    setResult(RESULT_CANCELED, result)
    val manager = AppWidgetManager.getInstance(this)
    if (id == AppWidgetManager.INVALID_APPWIDGET_ID ||
      manager.getAppWidgetInfo(id)?.provider != ComponentName(this, ImportantDayWidgetProvider::class.java)) {
      finish()
      return
    }
    val store = LocalStore(this)
    val widgets = WidgetStore(this)
    val original = widgets.binding(id)
    setContent {
      val session = remember(revision) { store.loadSession() }
      val snapshot = remember(revision, session?.userId) { session?.let { store.loadSnapshot(it.userId) } }
      var selected by rememberSaveable { mutableIntStateOf(
        if (original?.userId == session?.userId && original?.backend == BuildConfig.API_BASE_URL.trimEnd('/')) original.eventId else 0) }
      var ownerId by rememberSaveable { mutableIntStateOf(session?.userId ?: 0) }
      var error by remember { mutableStateOf<String?>(null) }
      LaunchedEffect(session?.userId) {
        if (ownerId != (session?.userId ?: 0)) { selected = 0; ownerId = session?.userId ?: 0 }
      }
      DaysMatterTheme(isSystemInDarkTheme()) {
        Surface(Modifier.fillMaxSize()) {
          Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("配置重要日子", style = MaterialTheme.typography.headlineSmall)
            Text("此事件的名称和日期会在桌面可见。每个实例固定关注一个主事件，默认跟随系统外观。")
            when {
              session == null -> Text("请先登录，再回来配置")
              snapshot == null -> Text("尚未同步，请先在应用中获取事件")
              snapshot.events.isEmpty() -> Text("添加一个重要日子")
              else -> snapshot.events.forEach { event ->
                Row(Modifier.fillMaxWidth().selectable(selected == event.id, role = Role.RadioButton,
                  onClick = { selected = event.id }).padding(vertical = 8.dp)) {
                  RadioButton(selected == event.id, onClick = null)
                  Text(event.name, modifier = Modifier.weight(1f).padding(top = 12.dp))
                }
              }
            }
            if (session != null && selected != 0 && snapshot?.events?.any { it.id == selected } == true) {
              Text("桌面预览", style = MaterialTheme.typography.titleMedium)
              AndroidView(factory = { FrameLayout(it) }, modifier = Modifier.fillMaxWidth().height(180.dp), update = { frame ->
                val preview = ImportantDayWidgetProvider.render(this@WidgetConfigurationActivity, id,
                  target = EventTarget(BuildConfig.API_BASE_URL.trimEnd('/'), session.userId, selected))
                frame.removeAllViews()
                frame.addView(preview.apply(frame.context, frame))
              })
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
              val current = store.loadSession()
              val exists = current?.let { store.loadSnapshot(it.userId)?.events?.any { event -> event.id == selected } } == true
              if (current == null || current.userId != ownerId || !exists ||
                manager.getAppWidgetInfo(id)?.provider != ComponentName(this@WidgetConfigurationActivity, ImportantDayWidgetProvider::class.java)) {
                error = "账号或事件已变化，请重新选择"
              } else {
                widgets.bind(id, EventTarget(BuildConfig.API_BASE_URL.trimEnd('/'), current.userId, selected))
                ImportantDayWidgetProvider.update(this@WidgetConfigurationActivity, id)
                setResult(RESULT_OK, result)
                finish()
              }
            }, enabled = session != null && selected != 0 && snapshot?.events?.any { it.id == selected } == true) { Text("保存到桌面") }
            if (session == null || snapshot == null || snapshot.events.isEmpty()) TextButton(onClick = {
              startActivity(Intent(this@WidgetConfigurationActivity, MainActivity::class.java).apply {
                if (session != null && snapshot?.events?.isEmpty() == true) {
                  putExtra("create_event", true); putExtra("user_id", session.userId)
                  putExtra("backend", BuildConfig.API_BASE_URL.trimEnd('/'))
                }
              })
            }) { Text(if (session != null && snapshot?.events?.isEmpty() == true) "打开应用创建事件" else "打开应用登录或同步") }
            TextButton(onClick = { finish() }) { Text("取消") }
          }
        }
      }
    }
  }
}
