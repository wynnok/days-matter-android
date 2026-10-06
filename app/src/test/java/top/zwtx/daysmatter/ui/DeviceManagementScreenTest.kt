package top.zwtx.daysmatter.ui

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import top.zwtx.daysmatter.*
import top.zwtx.daysmatter.data.ApiClient
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class DeviceManagementScreenTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private val reminderTime = androidx.compose.runtime.mutableStateOf<java.time.Instant?>(null)
  private lateinit var activityContext: android.content.Context
  private lateinit var vm: MainViewModel
  private lateinit var server: HttpServer
  private lateinit var backend: String
  private val app: Application get() = RuntimeEnvironment.getApplication()
  @Volatile private var user = 7
  @Volatile private var empty = false
  @Volatile private var fail = false
  private var release = CountDownLatch(0)

  @Before fun start() {
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.executor = java.util.concurrent.Executors.newCachedThreadPool()
    server.createContext("/") { exchange ->
      val isEvents = exchange.requestURI.path == "/events"
      if (isEvents) release.await(5, TimeUnit.SECONDS)
      val data: Any = when (exchange.requestURI.path) {
        "/auth/login" -> JSONObject().put("user_id", user).put("name", "账号$user")
          .put("email", "test@example.com").put("token", "synthetic-token")
        "/user/info" -> JSONObject().put("nickname", "账号$user").put("email", "test@example.com")
        "/events" -> if (empty) JSONArray() else JSONArray().put(JSONObject().put("event_id", 11)
          .put("event_name", "目标事件").put("server_next_occurrence", "2099-10-20"))
          .put(JSONObject().put("event_id", 12).put("event_name", "过期重复事件").put("repeat_type", 4).put("server_next_occurrence", "2000-01-01"))
          .put(JSONObject().put("event_id", 13).put("event_name", "过去事件").put("server_next_occurrence", "2000-01-01"))
          .put(JSONObject().put("event_id", 14).put("event_name", "未知日期事件"))
        else -> JSONArray()
      }
      val code = if (isEvents && fail) 503 else 200
      val bytes = JSONObject().put("code", code).put("message", "测试同步失败").put("data", data).toString().toByteArray()
      exchange.sendResponseHeaders(code, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    backend = "http://127.0.0.1:${server.address.port}"
    compose.runOnIdle { vm = MainViewModel(app, ApiClient(backend)) }
    compose.setContent { activityContext = androidx.compose.ui.platform.LocalContext.current; DaysMatterTheme(false) {
      if (reminderTime.value == null) DaysMatterApp(vm) else {
        LocalReminderScreen(vm, {}, androidx.compose.foundation.layout.PaddingValues(), reminderTime.value!!)
      }
    } }
  }

  @After fun stop() {
    release.countDown()
    server.stop(0)
    (server.executor as java.util.concurrent.ExecutorService).shutdownNow()
  }
  private fun waitForRequest() = compose.waitUntil(5_000) {
    shadowOf(Looper.getMainLooper()).idle(); !vm.busy
  }
  private fun login() { compose.runOnIdle { vm.login("test@example.com", "password") }; waitForRequest() }
  private fun open(id: Int = 11, service: String = backend) {
    compose.runOnIdle { vm.openEvent(EventTarget(service, 7, id).intent(app)) }
  }

  @Test fun localReminderManagementIsReachableFromMyPage() {
    login()
    compose.onNodeWithText("我的").performClick()
    compose.onNodeWithText("本地提醒").performScrollTo().performClick()
    compose.onNodeWithText("通知权限：未授予").assertExists()
    compose.onNodeWithText("测试通知").performScrollTo().performClick()
    compose.onNodeWithText("请先授予通知权限").assertExists()
  }
  private fun openReminders() {
    compose.onNodeWithText("我的").performClick()
    compose.onNodeWithText("本地提醒").performScrollTo().performClick()
  }

  @Test fun enabledEventsShowKnownUpcomingPassedAndPendingTimingAndConfigure() {
    val store = top.zwtx.daysmatter.data.LocalStore(app)
    for (id in listOf(11, 12, 13, 14)) store.saveLocalReminder(7, id, top.zwtx.daysmatter.data.LocalReminder(true, 1, "09:00"))
    login()
    openReminders()
    compose.onNodeWithText("下一次已知提醒：2099-10-19 09:00（东八区）").performScrollTo().assertExists()
    compose.onAllNodesWithText("发生日期待同步，请联网确认下一次日期").assertCountEquals(2)
    compose.onNodeWithText("本次提醒时间已过，无法据此判断之前是否送达").performScrollTo().assertExists()
    compose.onNodeWithText("配置 目标事件 的本地提醒").performScrollTo().performClick()
    compose.onNodeWithText("编辑倒数日").assertExists()
    compose.onNodeWithText("本机通知").performScrollTo().assertExists()
  }

  @Test fun grantedPermissionCanSubmitActualTestNotificationAndOpenSystemSettings() {
    shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    login()
    openReminders()
    compose.onNodeWithText("通知权限：已授予").assertExists()
    compose.onNodeWithText("测试通知").performScrollTo().performClick()
    val notifications = app.getSystemService(android.app.NotificationManager::class.java)
    compose.runOnIdle {
      val notification = shadowOf(notifications).allNotifications.single()
      assertEquals("本地提醒测试", notification.extras.getString(android.app.Notification.EXTRA_TITLE))
    }
    compose.onNodeWithText("系统通知设置").performScrollTo().performClick()
    compose.runOnIdle {
      val settings = shadowOf(activityContext as android.app.Activity).nextStartedActivity
      assertEquals(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS, settings.action)
      assertEquals(app.packageName, settings.getStringExtra(android.provider.Settings.EXTRA_APP_PACKAGE))
    }
  }

  @Test fun disabledAppNotificationsExplainBlockAndDoNotSubmitTest() {
    shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    val notifications = app.getSystemService(android.app.NotificationManager::class.java)
    shadowOf(notifications).setNotificationsEnabled(false)
    login()
    openReminders()
    compose.onNodeWithText("系统应用通知：已关闭").assertExists()
    compose.onNodeWithText("测试通知").performScrollTo().performClick()
    compose.onNodeWithText("系统应用通知已关闭，请在系统设置中开启").assertExists()
    assertEquals(0, shadowOf(notifications).allNotifications.size)
  }

  @Test fun blockedReminderChannelExplainsWhyTestCannotDisplay() {
    shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    val notifications = app.getSystemService(android.app.NotificationManager::class.java)
    notifications.createNotificationChannel(android.app.NotificationChannel("event_reminders", "事件提醒", android.app.NotificationManager.IMPORTANCE_NONE))
    login()
    openReminders()
    compose.onNodeWithText("事件提醒渠道：已关闭").assertExists()
    compose.onNodeWithText("测试通知").performScrollTo().performClick()
    compose.onNodeWithText("事件提醒渠道已关闭，请在系统设置中开启").assertExists()
    assertEquals(0, shadowOf(notifications).allNotifications.size)
  }
  @Test fun visibleReminderOverviewAdvancesPastItsKnownDeadline() {
    top.zwtx.daysmatter.data.LocalStore(app).saveLocalReminder(7, 11, top.zwtx.daysmatter.data.LocalReminder(true, 1, "09:00"))
    compose.runOnIdle { reminderTime.value = java.time.Instant.parse("2099-10-19T00:59:00Z") }
    login()
    compose.onNodeWithText("下一次已知提醒：2099-10-19 09:00（东八区）").performScrollTo().assertExists()
    compose.runOnIdle { reminderTime.value = java.time.Instant.parse("2099-10-19T01:00:00Z") }
    compose.onNodeWithText("本次提醒时间已过，无法据此判断之前是否送达").assertExists()
    compose.onNodeWithText("下一次已知提醒：2099-10-19 09:00（东八区）").assertDoesNotExist()
  }
}
