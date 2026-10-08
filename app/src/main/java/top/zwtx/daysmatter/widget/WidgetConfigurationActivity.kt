package top.zwtx.daysmatter.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import top.zwtx.daysmatter.BuildConfig
import top.zwtx.daysmatter.EventTarget
import top.zwtx.daysmatter.MainActivity
import top.zwtx.daysmatter.data.LocalStore

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
      var appearance by rememberSaveable { mutableStateOf(widgets.appearance(id)) }
      var error by remember { mutableStateOf<String?>(null) }
      LaunchedEffect(session?.userId) {
        if (ownerId != (session?.userId ?: 0)) { selected = 0; categoryId = 0; ownerId = session?.userId ?: 0 }
      }
      WidgetConfigurationScreen(
        upcoming = upcoming, signedIn = session != null, snapshot = snapshot,
        selected = selected, onSelectEvent = { selected = it },
        window = window, onSelectWindow = { window = it },
        categoryId = categoryId, onSelectCategory = { categoryId = it },
        appearance = appearance, onSelectAppearance = { appearance = it }, error = error,
        canSave = session != null && snapshot != null &&
          (if (upcoming) categoryId == 0 || snapshot.categories.any { it.id == categoryId }
          else selected != 0 && snapshot.events.any { it.id == selected }),
        onSave = {
          val current = store.loadSession()
          val exists = current?.let { owner -> store.loadSnapshot(owner.userId)?.let { data ->
            if (upcoming) categoryId == 0 || data.categories.any { it.id == categoryId }
            else data.events.any { it.id == selected }
          } } == true
          if (current == null || current.userId != ownerId || !exists ||
            manager.getAppWidgetInfo(id)?.provider != provider) {
            error = "账号或事件已变化，请重新选择"
          } else {
            widgets.saveAppearance(id, appearance)
            if (upcoming) {
              widgets.saveUpcoming(id, UpcomingBinding(BuildConfig.API_BASE_URL, current.userId, window, categoryId))
              UpcomingWidgetProvider.update(this@WidgetConfigurationActivity, id)
            } else {
              widgets.bind(id, EventTarget(BuildConfig.API_BASE_URL.trimEnd('/'), current.userId, selected))
              ImportantDayWidgetProvider.update(this@WidgetConfigurationActivity, id)
            }
            WidgetRefreshScheduler.requestNetwork(this@WidgetConfigurationActivity)
            setResult(RESULT_OK, result)
            finish()
          }
        },
        onCancel = { finish() },
        onOpenApp = {
          startActivity(Intent(this@WidgetConfigurationActivity, MainActivity::class.java).apply {
            if (session != null && snapshot?.events?.isEmpty() == true) {
              putExtra("create_event", true); putExtra("user_id", session.userId)
              putExtra("backend", BuildConfig.API_BASE_URL.trimEnd('/'))
            }
          })
        },
        preview = {
          AndroidView(factory = { FrameLayout(it).apply {
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
          } }, modifier = Modifier.fillMaxWidth().height(180.dp), update = { frame ->
            val preview = if (upcoming) UpcomingWidgetProvider.render(this@WidgetConfigurationActivity, id,
              binding = session?.let { UpcomingBinding(BuildConfig.API_BASE_URL, it.userId, window, categoryId) },
              appearance = appearance, previewHeight = 180)
            else ImportantDayWidgetProvider.render(this@WidgetConfigurationActivity, id,
              target = session?.takeIf { selected != 0 }?.let { EventTarget(BuildConfig.API_BASE_URL.trimEnd('/'), it.userId, selected) },
              appearance = appearance, previewSize = 320 to 180)
            frame.removeAllViews()
            frame.addView(preview.apply(frame.context, frame))
          })
        }
      )
    }
  }
}
