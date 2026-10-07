package top.zwtx.daysmatter.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.os.Looper
import android.widget.TextView
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import top.zwtx.daysmatter.*
import top.zwtx.daysmatter.data.*
import top.zwtx.daysmatter.ui.AccountTestServer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetSyncIntegrationTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private val app get() = RuntimeEnvironment.getApplication()
  private val manager get() = AppWidgetManager.getInstance(app)
  private lateinit var server: AccountTestServer
  private lateinit var vm: MainViewModel
  private var widgetId = 0
  private var recentId = 0

  @Before fun start() {
    server = AccountTestServer()
    compose.runOnIdle { vm = MainViewModel(app, ApiClient(server.baseUrl)) }
    compose.setContent { androidx.compose.material3.Text(vm.message ?: "") }
    compose.runOnIdle { vm.login("test@example.com", "password") }
    waitForRequest()
    widgetId = shadowOf(manager).createWidget(ImportantDayWidgetProvider::class.java, R.layout.important_day_widget)
    shadowOf(manager).putWidgetInfo(widgetId, AppWidgetProviderInfo().apply {
      provider = ComponentName(app, ImportantDayWidgetProvider::class.java)
    })
    WidgetStore(app).bind(widgetId, EventTarget(BuildConfig.API_BASE_URL.trimEnd('/'), 7, 11))
    recentId = shadowOf(manager).createWidget(UpcomingWidgetProvider::class.java, R.layout.upcoming_widget)
    shadowOf(manager).putWidgetInfo(recentId, AppWidgetProviderInfo().apply {
      provider = ComponentName(app, UpcomingWidgetProvider::class.java)
    })
    WidgetStore(app).saveUpcoming(recentId, UpcomingBinding(BuildConfig.API_BASE_URL, 7))
    WidgetUpdates.redraw(app)
    assertEquals("目标事件", text(R.id.widget_name))
  }
  @After fun stop() { server.close() }
  private fun waitForRequest() = compose.waitUntil(5_000) { shadowOf(Looper.getMainLooper()).idle(); !vm.busy }
  private fun text(id: Int) = shadowOf(manager).getViewFor(widgetId).findViewById<TextView>(id).text.toString()

  @Test fun confirmedDeletionUpdatesDesktopEvenWhenSubsequentRefreshFails() {
    server.failRefreshAfterWrite = true
    compose.runOnIdle { vm.write("DELETE", "/events/11") }
    waitForRequest()
    assertEquals("事件已删除，请重新选择", text(R.id.widget_days))
    assertFalse(shadowOf(manager).getViewFor(widgetId).contentDescription.toString().contains("目标事件"))
  }

  @Test fun confirmedEditUpdatesDesktopAndDoesNotReuseChangedOccurrenceWhenRefreshFails() {
    server.failRefreshAfterWrite = true
    compose.runOnIdle {
      vm.saveEvent(11, JSONObject().put("event_name", "确认修改名称").put("target_date", "2099-01-01"), LocalReminder()) {}
    }
    waitForRequest()
    assertEquals("确认修改名称", text(R.id.widget_name))
    assertEquals("日期待同步", text(R.id.widget_days))
    assertEquals("本次日期待同步", text(R.id.widget_date))
  }

  @Test fun logoutImmediatelyReplacesExistingDesktopContent() {
    compose.runOnIdle { vm.logout() }
    assertEquals("请登录以查看重要日子", text(R.id.widget_days))
    assertEquals("重要日子", text(R.id.widget_name))
    assertEquals("请登录以查看近期日程", shadowOf(manager).getViewFor(recentId).findViewById<TextView>(R.id.upcoming_row_1).text.toString())
  }

  @Test fun expiredAuthenticationImmediatelyHidesDesktopAccountData() {
    server.unauthorizedEvents = true
    compose.runOnIdle { vm.refresh() }
    waitForRequest()
    assertNull(vm.session)
    assertEquals("请登录以查看近期日程", shadowOf(manager).getViewFor(recentId).findViewById<TextView>(R.id.upcoming_row_1).text.toString())
    assertEquals("请登录以查看重要日子", text(R.id.widget_days))
    assertFalse(shadowOf(manager).getViewFor(widgetId).contentDescription.toString().contains("目标事件"))
  }

  @Test fun failedSyncKeepsCachedEventAndShowsFailureAndLastSuccessfulTime() {
    server.failEvents = true
    compose.runOnIdle { vm.refresh() }
    waitForRequest()
    assertEquals("目标事件", text(R.id.widget_name))
    assertTrue(text(R.id.widget_status).contains("同步失败，显示缓存"))
    assertTrue(text(R.id.widget_status).contains("上次同步"))
  }

  @Test fun unrelatedExportPreservesBackgroundFailureUntilConfirmedAccountRecovery() {
    val before = vm.store.loadSnapshot(7)!!.syncedAt
    server.failEvents = true
    runBlocking {
      assertFalse(WidgetSyncJobService.synchronize(app, ApiClient(server.baseUrl),
        Clock.fixed(Instant.ofEpochMilli(before).plusSeconds(7200), ZoneOffset.UTC)))
    }
    assertTrue(text(R.id.widget_status).contains("同步失败"))
    compose.runOnIdle { vm.exportData {} }
    waitForRequest()
    assertTrue("导出没有获取新的账号数据，不能清除后台失败", text(R.id.widget_status).contains("同步失败"))
    assertTrue(shadowOf(manager).getViewFor(recentId).findViewById<TextView>(R.id.upcoming_status).text.contains("同步失败"))
    assertEquals(before, vm.store.loadSnapshot(7)!!.syncedAt)
    server.failEvents = false
    server.events.getJSONObject(0).put("event_name", "真正恢复后的事件")
    compose.runOnIdle { vm.refreshIfNeeded() }
    waitForRequest()
    assertEquals("真正恢复后的事件", text(R.id.widget_name))
    assertFalse(text(R.id.widget_status).contains("同步失败"))
  }

  @Test fun closingForegroundOwnerDoesNotInventADesktopSyncFailure() {
    val lifecycle = androidx.lifecycle.ViewModelStore().apply { put("account", vm) }
    val requestsBefore = server.eventRequests.get()
    server.eventsPaused = java.util.concurrent.CountDownLatch(1)
    compose.runOnIdle { vm.refresh() }
    compose.waitUntil(5_000) { server.eventRequests.get() > requestsBefore }
    compose.runOnIdle { lifecycle.clear() }
    server.eventsPaused.countDown()
    waitForRequest()
    assertFalse("正常结束前台读取不是账号同步失败", text(R.id.widget_status).contains("同步失败"))
    assertFalse(shadowOf(manager).getViewFor(recentId).findViewById<TextView>(R.id.upcoming_status).text.contains("同步失败"))
  }

  @Test fun successfulSyncUpdatesDesktopAndPermissionDenialDoesNotHideIt() {
    server.events.getJSONObject(0).put("event_name", "另一个设备修改的名称")
    compose.runOnIdle { vm.refresh() }
    waitForRequest()
    assertEquals("另一个设备修改的名称", text(R.id.widget_name))
    assertFalse(top.zwtx.daysmatter.reminder.ReminderNotifications(app).permissionGranted)
    assertTrue(text(R.id.widget_status).contains("上次同步"))
  }
}
