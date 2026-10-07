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
    val provider = manager.getAppWidgetInfo(id)?.provider
    val upcoming = provider == ComponentName(this, UpcomingWidgetProvider::class.java)
    if (id == AppWidgetManager.INVALID_APPWIDGET_ID ||
      (provider != ComponentName(this, ImportantDayWidgetProvider::class.java) && !upcoming)) {
      finish()
      return
    }
    val store = LocalStore(this)
    val widgets = WidgetStore(this)
    val original = widgets.binding(id)
    val originalUpcoming = widgets.upcoming(id)
    setContent {
      val session = remember(revision) { store.loadSession() }
      val snapshot = remember(revision, session?.userId) { session?.let { store.loadSnapshot(it.userId) } }
      var selected by rememberSaveable { mutableIntStateOf(
        if (original?.userId == session?.userId && original?.backend == BuildConfig.API_BASE_URL.trimEnd('/')) original.eventId else 0) }
      var window by rememberSaveable { mutableIntStateOf(originalUpcoming?.window ?: 30) }
      var categoryId by rememberSaveable { mutableIntStateOf(
        if (originalUpcoming?.userId == session?.userId && originalUpcoming?.backend == BuildConfig.API_BASE_URL.trimEnd('/')) originalUpcoming.categoryId else 0) }
      var ownerId by rememberSaveable { mutableIntStateOf(session?.userId ?: 0) }
      var error by remember { mutableStateOf<String?>(null) }
      LaunchedEffect(session?.userId) {
        if (ownerId != (session?.userId ?: 0)) { selected = 0; categoryId = 0; ownerId = session?.userId ?: 0 }
      }
      DaysMatterTheme(isSystemInDarkTheme()) {
        Surface(Modifier.fillMaxSize()) {
          Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (upcoming) "配置近期日程" else "配置重要日子", style = MaterialTheme.typography.headlineSmall)
            Text(if (upcoming) "窗口只限制桌面列表；查看全部进入分类首页。内容会在桌面可见。" else "此事件的名称和日期会在桌面可见。每个实例固定关注一个主事件，默认跟随系统外观。")
            if (upcoming && session != null && snapshot != null) {
              listOf(30, 7).forEach { value ->
                TextButton(onClick = { window = value }) { Text("近期 $value 天${if (window == value) "（已选）" else ""}") }
              }
              TextButton(onClick = { categoryId = 0 }) { Text("全部分类${if (categoryId == 0) "（已选）" else ""}") }
              snapshot.categories.forEach { category ->
                TextButton(onClick = { categoryId = category.id }) { Text("${category.name}${if (categoryId == category.id) "（已选）" else ""}") }
              }
              if (categoryId != 0 && snapshot.categories.none { it.id == categoryId }) Text("分类已删除，请重新选择")
            }
            when {
              session == null -> Text("请先登录，再回来配置")
              snapshot == null -> Text("尚未同步，请先在应用中获取事件")
              snapshot.events.isEmpty() -> Text(if (upcoming) "暂无事件，请在应用中创建" else "添加一个重要日子")
              upcoming -> Unit
              else -> snapshot.events.forEach { event ->
                Row(Modifier.fillMaxWidth().selectable(selected == event.id, role = Role.RadioButton,
                  onClick = { selected = event.id }).padding(vertical = 8.dp)) {
                  RadioButton(selected == event.id, onClick = null)
                  Text(event.name, modifier = Modifier.weight(1f).padding(top = 12.dp))
                }
              }
            }
            if (session != null && snapshot != null && (upcoming || (selected != 0 && snapshot.events.any { it.id == selected }))) {
              Text("桌面预览", style = MaterialTheme.typography.titleMedium)
              AndroidView(factory = { FrameLayout(it) }, modifier = Modifier.fillMaxWidth().height(180.dp), update = { frame ->
                val preview = if (upcoming) UpcomingWidgetProvider.render(this@WidgetConfigurationActivity, id,
                  binding = UpcomingBinding(BuildConfig.API_BASE_URL, session.userId, window, categoryId))
                else ImportantDayWidgetProvider.render(this@WidgetConfigurationActivity, id,
                  target = EventTarget(BuildConfig.API_BASE_URL.trimEnd('/'), session.userId, selected))
                frame.removeAllViews()
                frame.addView(preview.apply(frame.context, frame))
              })
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
              val current = store.loadSession()
              val exists = current?.let { owner -> store.loadSnapshot(owner.userId)?.let { data ->
                if (upcoming) categoryId == 0 || data.categories.any { it.id == categoryId }
                else data.events.any { it.id == selected }
              } } == true
              if (current == null || current.userId != ownerId || !exists ||
                manager.getAppWidgetInfo(id)?.provider != provider) {
                error = "账号或事件已变化，请重新选择"
              } else {
                if (upcoming) {
                  widgets.saveUpcoming(id, UpcomingBinding(BuildConfig.API_BASE_URL, current.userId, window, categoryId))
                  UpcomingWidgetProvider.update(this@WidgetConfigurationActivity, id)
                } else {
                  widgets.bind(id, EventTarget(BuildConfig.API_BASE_URL.trimEnd('/'), current.userId, selected))
                  ImportantDayWidgetProvider.update(this@WidgetConfigurationActivity, id)
                }
                setResult(RESULT_OK, result)
                finish()
              }
            }, enabled = session != null && snapshot != null &&
              (if (upcoming) categoryId == 0 || snapshot.categories.any { it.id == categoryId } else selected != 0 && snapshot.events.any { it.id == selected })) { Text("保存到桌面") }
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
