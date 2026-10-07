package top.zwtx.daysmatter.ui

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class DeviceManagementScreenTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private val reminderTime = androidx.compose.runtime.mutableStateOf<java.time.Instant?>(null)
  private lateinit var activityContext: android.content.Context
  private lateinit var vm: MainViewModel
  private lateinit var server: AccountTestServer
  private val app: Application get() = RuntimeEnvironment.getApplication()

  @Before fun start() {
    server = AccountTestServer().apply {
      events = JSONArray().put(JSONObject().put("event_id", 11).put("event_name", "目标事件").put("server_next_occurrence", "2099-10-20"))
        .put(JSONObject().put("event_id", 12).put("event_name", "过期重复事件").put("repeat_type", 4).put("server_next_occurrence", "2000-01-01"))
        .put(JSONObject().put("event_id", 13).put("event_name", "过去事件").put("server_next_occurrence", "2000-01-01"))
        .put(JSONObject().put("event_id", 14).put("event_name", "未知日期事件"))
    }
    compose.runOnIdle { vm = MainViewModel(app, ApiClient(server.baseUrl)) }
    compose.setContent { activityContext = androidx.compose.ui.platform.LocalContext.current; DaysMatterTheme(false) {
      if (reminderTime.value == null) DaysMatterApp(vm) else {
        LocalReminderScreen(vm, {}, androidx.compose.foundation.layout.PaddingValues(), reminderTime.value!!)
      }
    } }
  }

  @After fun stop() { server.close() }
  private fun waitForRequest() = compose.waitUntil(5_000) {
    shadowOf(Looper.getMainLooper()).idle(); !vm.busy
  }
  private fun login() { compose.runOnIdle { vm.login("test@example.com", "password") }; waitForRequest() }

  @Test fun localReminderManagementIsReachableFromMyPage() {
    login()
    compose.onNodeWithText("我的").performClick()
    compose.onNodeWithText("本地提醒").performScrollTo().performClick()
    compose.onNodeWithText("通知权限：未授予").assertExists()
    compose.onNodeWithText("测试通知").performScrollTo().performClick()
    compose.onNodeWithText("请先授予通知权限").assertExists()
  }
  @Test fun desktopManagementOpensSystemConfigurationForSelectedInstance() {
    login()
    val manager = android.appwidget.AppWidgetManager.getInstance(activityContext)
    val id = shadowOf(manager).createWidget(top.zwtx.daysmatter.widget.ImportantDayWidgetProvider::class.java,
      R.layout.important_day_widget)
    shadowOf(manager).putWidgetInfo(id, android.appwidget.AppWidgetProviderInfo().apply {
      provider = android.content.ComponentName(app, top.zwtx.daysmatter.widget.ImportantDayWidgetProvider::class.java)
    })
    assertTrue(top.zwtx.daysmatter.widget.ImportantDayWidgetProvider.instanceIds(activityContext).contains(id))
    top.zwtx.daysmatter.widget.WidgetStore(app).bind(id,
      EventTarget(BuildConfig.API_BASE_URL.trimEnd('/'), 7, 11))
    compose.onNodeWithText("我的").performClick()
    compose.onNodeWithText("退出登录").performScrollTo()
    compose.onNodeWithText("重要日子").performClick()
    compose.onNodeWithText("实例 $id · 目标事件").performScrollTo().assertExists()
    compose.onNodeWithText("配置实例 $id").performScrollTo().performClick()
    compose.runOnIdle {
      val opened = shadowOf(activityContext as android.app.Activity).nextStartedActivity
      assertEquals(top.zwtx.daysmatter.widget.WidgetConfigurationActivity::class.java.name, opened.component?.className)
      assertEquals(id, opened.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
    }
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
  @Test fun profileUsesSharedSummaryAndProvidesGroupedCategoryManagement() {
    login()
    compose.onNodeWithText("我的").performClick()
    compose.onNodeWithText("事件总数 4 · 近期 7 天 0").assertExists()
    compose.onNodeWithText("其中 2 个事件日期待同步").assertExists()
    for (group in listOf("提醒与权限", "数据与同步", "分类与偏好", "关于与帮助")) {
      compose.onNodeWithText(group).performScrollTo().assertExists()
    }
    compose.onNodeWithText("分类管理").performScrollTo().performClick()
    compose.onNodeWithText("添加分类").assertExists()
    compose.onNodeWithText("添加分类").performClick()
    compose.onNodeWithText("分类名称", substring = true).assertExists()
  }
}
