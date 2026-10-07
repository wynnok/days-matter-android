package top.zwtx.daysmatter.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Looper
import android.widget.TextView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import top.zwtx.daysmatter.R
import top.zwtx.daysmatter.TestKeyStore
import top.zwtx.daysmatter.data.LocalStore
import top.zwtx.daysmatter.data.Session
import top.zwtx.daysmatter.data.Snapshot
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class WidgetConfigurationFlowTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private val app get() = RuntimeEnvironment.getApplication()
  private val manager get() = AppWidgetManager.getInstance(app)
  private lateinit var store: LocalStore

  @Before fun seedAccountSnapshot() {
    store = LocalStore(app)
    store.saveSession(Session(7, "Private user", "private@example.com", "synthetic-token"))
    val occurrence = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(3).toString()
    store.saveSnapshot(7, Snapshot.fromJson(JSONObject().put("synced_at", System.currentTimeMillis())
      .put("events", JSONArray().put(JSONObject().put("event_id", 11).put("event_name", "第一个固定事件")
        .put("server_next_occurrence", occurrence).put("category_name", "生活").put("top_flag", 1))
        .put(JSONObject().put("event_id", 12).put("event_name", "第二个固定事件")
          .put("server_next_occurrence", occurrence)))))
  }

  private fun allocate(): Int {
    val id = shadowOf(manager).createWidget(ImportantDayWidgetProvider::class.java, R.layout.important_day_widget)
    shadowOf(manager).putWidgetInfo(id, AppWidgetProviderInfo().apply {
      provider = ComponentName(app, ImportantDayWidgetProvider::class.java)
      configure = ComponentName(app, WidgetConfigurationActivity::class.java)
      initialLayout = R.layout.important_day_widget
    })
    return id
  }

  private fun configure(id: Int) = Robolectric.buildActivity(WidgetConfigurationActivity::class.java,
    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)).setup()

  private fun renderedName(id: Int) = shadowOf(manager).getViewFor(id).findViewById<TextView>(R.id.widget_name).text.toString()

  @Test fun systemConfigurationSavesFixedEventAndActualWidgetClickRetainsOwner() {
    val id = allocate()
    val activity = configure(id)
    compose.onNodeWithText("第一个固定事件").performClick()
    compose.onNodeWithText("保存到桌面").performScrollTo().performClick()
    compose.runOnIdle {
      assertEquals(android.app.Activity.RESULT_OK, shadowOf(activity.get()).resultCode)
      assertEquals(id, shadowOf(activity.get()).resultIntent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
      assertEquals("第一个固定事件", renderedName(id))
      val view = shadowOf(manager).getViewFor(id)
      assertEquals("还有 3 天", view.findViewById<TextView>(R.id.widget_days).text.toString())
      view.findViewById<android.view.View>(R.id.widget_root).performClick()
      val opened = shadowOf(app).nextStartedActivity
      assertEquals(11, opened.getIntExtra("event_id", 0))
      assertEquals(7, opened.getIntExtra("user_id", 0))
    }
    activity.pause().stop().destroy()
  }
  private fun chooseAndSave(id: Int, event: String) {
    val controller = configure(id)
    compose.onNodeWithText(event).performClick()
    compose.onNodeWithText("保存到桌面").performScrollTo().performClick()
    controller.pause().stop().destroy()
  }

  @Test fun independentlyConfiguredInstancesKeepTheirOwnEventsAndCancelledReconfiguration() {
    val first = allocate()
    val second = allocate()
    chooseAndSave(first, "第一个固定事件")
    chooseAndSave(second, "第二个固定事件")
    assertEquals("第一个固定事件", renderedName(first))
    assertEquals("第二个固定事件", renderedName(second))
    val reconfigure = configure(first)
    compose.onNodeWithText("第二个固定事件").performClick()
    compose.onNodeWithText("取消").performScrollTo().performClick()
    assertEquals(android.app.Activity.RESULT_CANCELED, shadowOf(reconfigure.get()).resultCode)
    reconfigure.pause().stop().destroy()
    ImportantDayWidgetProvider.updateAll(app)
    assertEquals("第一个固定事件", renderedName(first))
    assertEquals("第二个固定事件", renderedName(second))
    ImportantDayWidgetProvider().onDeleted(app, intArrayOf(first))
    ImportantDayWidgetProvider.update(app, second)
    assertEquals("第二个固定事件", renderedName(second))
  }

  @Test fun cancellingNewConfigurationLeavesNoEventContentOrBinding() {
    val id = allocate()
    val controller = configure(id)
    compose.onNodeWithText("第一个固定事件").performClick()
    compose.onNodeWithText("取消").performScrollTo().performClick()
    assertEquals(android.app.Activity.RESULT_CANCELED, shadowOf(controller.get()).resultCode)
    controller.pause().stop().destroy()
    ImportantDayWidgetProvider.update(app, id)
    assertEquals("重要日子", renderedName(id))
    assertEquals("添加一个重要日子", shadowOf(manager).getViewFor(id).findViewById<TextView>(R.id.widget_days).text.toString())
  }

  @Test fun changingAccountBeforeSavingRejectsStaleSelection() {
    val id = allocate()
    val controller = configure(id)
    compose.onNodeWithText("第一个固定事件").performClick()
    store.saveSession(Session(8, "Other", "other@example.com", "synthetic-other"))
    compose.onNodeWithText("保存到桌面").performScrollTo().performClick()
    compose.onNodeWithText("账号或事件已变化，请重新选择").assertExists()
    assertEquals(android.app.Activity.RESULT_CANCELED, shadowOf(controller.get()).resultCode)
    controller.pause().stop().destroy()
  }

  @Test fun emptyAccountGuidesCreationThroughAnActualApplicationIntent() {
    store.saveSnapshot(7, Snapshot.fromJson(JSONObject().put("events", JSONArray())))
    val id = allocate()
    val controller = configure(id)
    compose.onNodeWithText("添加一个重要日子").assertExists()
    compose.onNodeWithText("打开应用创建事件").performScrollTo().performClick()
    val opened = shadowOf(controller.get()).nextStartedActivity
    assertEquals(top.zwtx.daysmatter.MainActivity::class.java.name, opened.component?.className)
    assertTrue(opened.getBooleanExtra("create_event", false))
    assertEquals(7, opened.getIntExtra("user_id", 0))
    controller.pause().stop().destroy()
  }

  @Test fun crossDayRenderingRetainsKnownDatesAndNeverInventsRepeatingOccurrence() {
    val first = allocate()
    val second = allocate()
    chooseAndSave(first, "第一个固定事件")
    chooseAndSave(second, "第二个固定事件")
    val raw = JSONObject().put("synced_at", 1791331200000L).put("events", JSONArray()
      .put(JSONObject().put("event_id", 11).put("event_name", "第一个固定事件").put("server_next_occurrence", "2026-10-10"))
      .put(JSONObject().put("event_id", 12).put("event_name", "第二个固定事件").put("repeat_type", 4)
        .put("server_next_occurrence", "2026-10-10")))
    store.saveSnapshot(7, Snapshot.fromJson(raw))
    ImportantDayWidgetProvider.update(app, first, java.time.Instant.parse("2026-10-07T16:00:00Z"))
    assertEquals("还有 2 天", shadowOf(manager).getViewFor(first).findViewById<TextView>(R.id.widget_days).text.toString())
    ImportantDayWidgetProvider.update(app, first, java.time.Instant.parse("2026-10-10T16:00:00Z"))
    ImportantDayWidgetProvider.update(app, second, java.time.Instant.parse("2026-10-10T16:00:00Z"))
    assertEquals("已过 1 天", shadowOf(manager).getViewFor(first).findViewById<TextView>(R.id.widget_days).text.toString())
    val repeating = shadowOf(manager).getViewFor(second)
    assertEquals("待同步下一次日期", repeating.findViewById<TextView>(R.id.widget_days).text.toString())
    assertEquals("本次日期：2026-10-10", repeating.findViewById<TextView>(R.id.widget_date).text.toString())
  }

  @Test fun anotherAccountWithSameEventIdCannotExposeOldDesktopContent() {
    val id = allocate()
    chooseAndSave(id, "第一个固定事件")
    store.saveSession(Session(8, "Other", "other@example.com", "synthetic-other"))
    store.saveSnapshot(8, store.loadSnapshot(7)!!)
    ImportantDayWidgetProvider.updateAll(app)
    assertEquals("重要日子", renderedName(id))
    val view = shadowOf(manager).getViewFor(id)
    assertEquals("请登录所属账号或重新配置", view.findViewById<TextView>(R.id.widget_days).text.toString())
    assertFalse(view.contentDescription.toString().contains("第一个固定事件"))
    store.clearAccount(8)
    ImportantDayWidgetProvider.updateAll(app)
    assertEquals("请登录以查看重要日子", shadowOf(manager).getViewFor(id).findViewById<TextView>(R.id.widget_days).text.toString())
  }

  @Test fun deletedEventRequiresReselectionInsteadOfSilentlySwitchingObjects() {
    val id = allocate()
    chooseAndSave(id, "第一个固定事件")
    store.saveSnapshot(7, Snapshot.fromJson(JSONObject().put("events", JSONArray()
      .put(JSONObject().put("event_id", 12).put("event_name", "第二个固定事件")))))
    ImportantDayWidgetProvider.updateAll(app)
    assertEquals("重要日子", renderedName(id))
    assertEquals("事件已删除，请重新选择", shadowOf(manager).getViewFor(id).findViewById<TextView>(R.id.widget_days).text.toString())
    shadowOf(manager).getViewFor(id).performClick()
    assertEquals(WidgetConfigurationActivity::class.java.name, shadowOf(app).nextStartedActivity.component?.className)
  }
  @Test
  @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  fun resizedWidgetsKeepCoreTextWithinBoundsWithLargeFontsAndSystemNightTheme() {
    val id = allocate()
    chooseAndSave(id, "第一个固定事件")
    val snapshot = store.loadSnapshot(7)!!
    snapshot.raw.getJSONArray("events").getJSONObject(0).put("event_name", "一个很长很长但依然应该合理截断的主事件名称")
    store.saveSnapshot(7, Snapshot.fromJson(snapshot.raw))
    RuntimeEnvironment.setQualifiers("w411dp-h891dp-night")
    RuntimeEnvironment.setFontScale(1.5f)
    shadowOf(manager).setAlwaysRecreateViewsDuringUpdate(true)
    for ((width, height) in listOf(120 to 120, 320 to 80, 240 to 240)) {
      val options = android.os.Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
      }
      manager.updateAppWidgetOptions(id, options)
      ImportantDayWidgetProvider().onAppWidgetOptionsChanged(app, manager, id, options)
      val view = shadowOf(manager).getViewFor(id)
      view.measure(android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
        android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY))
      view.layout(0, 0, width, height)
      val days = view.findViewById<TextView>(R.id.widget_days)
      assertEquals("还有 3 天", days.text.toString())
      val date = view.findViewById<TextView>(R.id.widget_date)
      assertEquals("核心发生日期不能被省略", 0, date.layout.getEllipsisCount(0))
      assertEquals(android.graphics.Color.parseColor("#FFF1E9"), days.currentTextColor)
      assertTrue("天数的行宽必须适合尺寸 $width x $height", days.layout.getLineWidth(0) <= days.width - days.compoundPaddingLeft - days.compoundPaddingRight + 1)
      fun checkBounds(node: android.view.View, parentTop: Int = 0) {
        val top = parentTop + node.top
        if (node.visibility != android.view.View.VISIBLE) return
        if (node is TextView && node.text.isNotBlank()) {
          assertTrue("文字不得裁切：${node.text} ($width x $height)", top + node.height <= height)
        }
        if (node is android.view.ViewGroup) for (index in 0 until node.childCount) checkBounds(node.getChildAt(index), top)
      }
      checkBounds(view)
      val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
      view.draw(android.graphics.Canvas(bitmap))
      val output = java.io.File("build/widget-previews").apply { mkdirs() }
      java.io.File(output, "${width}x${height}-night-large-font.png").outputStream().use {
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
      }
    }
  }
}
