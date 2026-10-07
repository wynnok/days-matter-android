package top.zwtx.daysmatter.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.widget.TextView
import org.json.JSONArray
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
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpcomingWidgetTest {
  @get:Rule val keys = TestKeyStore()
  private val app get() = RuntimeEnvironment.getApplication()
  private val manager get() = AppWidgetManager.getInstance(app)
  private val now = Instant.parse("2026-10-07T00:00:00Z")
  private fun event(id: Int, name: String, date: String?, pinned: Boolean = false, category: Int = 1) =
    JSONObject().put("event_id", id).put("event_name", name).put("server_next_occurrence", date ?: JSONObject.NULL)
      .put("category_id", category).put("top_flag", if (pinned) 0 else 1)
  private fun seed() {
    LocalStore(app).saveSession(Session(7, "User", "test@example.com", "synthetic"))
    LocalStore(app).saveSnapshot(7, Snapshot.fromJson(JSONObject().put("synced_at", now.toEpochMilli())
      .put("categories", JSONArray().put(JSONObject().put("category_id", 1).put("category_name", "生活")))
      .put("events", JSONArray()
        .put(event(1, "过去", "2026-10-06"))
        .put(event(2, "今天", "2026-10-07"))
        .put(event(3, "Z置顶", "2026-10-13", true))
        .put(event(4, "A普通", "2026-10-13"))
        .put(event(5, "七天之外", "2026-10-14"))
        .put(event(6, "三十天边界", "2026-11-05"))
        .put(event(7, "远期置顶", "2026-11-06", true))
        .put(event(8, "未知", null)))))
  }
  private fun allocate(): Int {
    val id = shadowOf(manager).createWidget(UpcomingWidgetProvider::class.java, R.layout.upcoming_widget)
    shadowOf(manager).putWidgetInfo(id, AppWidgetProviderInfo().apply { provider = ComponentName(app, UpcomingWidgetProvider::class.java) })
    return id
  }
  @Test fun listUsesConfirmedWindowAndStableOrderAndClicksCarryOwner() {
    seed()
    val id = allocate()
    WidgetStore(app).saveUpcoming(id, UpcomingBinding(BuildConfig.API_BASE_URL, 7, 7, 1))
    UpcomingWidgetProvider.update(app, id, now)
    val view = shadowOf(manager).getViewFor(id)
    assertEquals("10/07 · 今天 · 今天", view.findViewById<TextView>(R.id.upcoming_row_1).text.toString())
    assertEquals("10/13 · Z置顶 · 还有 6 天", view.findViewById<TextView>(R.id.upcoming_row_2).text.toString())
    assertEquals("10/13 · A普通 · 还有 6 天", view.findViewById<TextView>(R.id.upcoming_row_3).text.toString())
    assertTrue(view.findViewById<TextView>(R.id.upcoming_status).text.contains("1 个日期待同步"))
    view.findViewById<TextView>(R.id.upcoming_row_2).performClick()
    val opened = shadowOf(app).nextStartedActivity
    assertEquals(3, opened.getIntExtra("event_id", 0))
    assertEquals(7, opened.getIntExtra("user_id", 0))
    view.findViewById<TextView>(R.id.upcoming_title).performClick()
    assertEquals(1, shadowOf(app).nextStartedActivity.getIntExtra("category_id", -1))
    manager.updateAppWidgetOptions(id, android.os.Bundle().apply { putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 400) })
    WidgetStore(app).saveUpcoming(id, UpcomingBinding(BuildConfig.API_BASE_URL, 7, 30, 1))
    UpcomingWidgetProvider.update(app, id, now)
    assertEquals("11/05 · 三十天边界 · 还有 29 天", shadowOf(manager).getViewFor(id).findViewById<TextView>(R.id.upcoming_row_5).text.toString())
    assertFalse(shadowOf(manager).getViewFor(id).contentDescription.toString().contains("远期置顶"))
  }
  @Test fun deletedCategoryRequiresReselectionAndNeverChangesImportantDayBinding() {
    seed()
    val id = allocate()
    WidgetStore(app).saveUpcoming(id, UpcomingBinding(BuildConfig.API_BASE_URL, 7, 30, 99))
    val fixed = shadowOf(manager).createWidget(ImportantDayWidgetProvider::class.java, R.layout.important_day_widget)
    shadowOf(manager).putWidgetInfo(fixed, AppWidgetProviderInfo().apply { provider = ComponentName(app, ImportantDayWidgetProvider::class.java) })
    WidgetStore(app).bind(fixed, EventTarget(BuildConfig.API_BASE_URL, 7, 2))
    UpcomingWidgetProvider.update(app, id, now)
    ImportantDayWidgetProvider.update(app, fixed, now)
    assertEquals("分类已删除，请重新选择", shadowOf(manager).getViewFor(id).findViewById<TextView>(R.id.upcoming_row_1).text.toString())
    assertEquals("今天", shadowOf(manager).getViewFor(fixed).findViewById<TextView>(R.id.widget_name).text.toString())
    shadowOf(manager).getViewFor(id).findViewById<TextView>(R.id.upcoming_title).performClick()
    assertEquals(WidgetConfigurationActivity::class.java.name, shadowOf(app).nextStartedActivity.component?.className)
    LocalStore(app).saveSession(Session(8, "Other", "other@example.com", "synthetic"))
    UpcomingWidgetProvider.update(app, id, now)
    assertEquals("请登录所属账号或重新配置", shadowOf(manager).getViewFor(id).findViewById<TextView>(R.id.upcoming_row_1).text.toString())
    assertFalse(shadowOf(manager).getViewFor(id).contentDescription.toString().contains("今天"))
  }

  @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
  fun resizedRecentListKeepsRowsAndClickAreasWithinBoundsWithLargeFonts() {
    seed()
    val id = allocate()
    WidgetStore(app).saveUpcoming(id, UpcomingBinding(BuildConfig.API_BASE_URL, 7))
    RuntimeEnvironment.setFontScale(1.5f)
    shadowOf(manager).setAlwaysRecreateViewsDuringUpdate(true)
    for ((width, height) in listOf(280 to 180, 320 to 360)) {
      manager.updateAppWidgetOptions(id, android.os.Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
      })
      UpcomingWidgetProvider.update(app, id, now)
      val view = shadowOf(manager).getViewFor(id)
      view.measure(android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
        android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY))
      view.layout(0, 0, width, height)
      val row = view.findViewById<TextView>(R.id.upcoming_row_1)
      assertTrue(row.height > 0)
      assertTrue(row.top + row.height <= height)
      assertTrue(row.text.toString().startsWith("10/07"))
      assertTrue(row.performClick())
      assertEquals(2, shadowOf(app).nextStartedActivity.getIntExtra("event_id", 0))
      val output = java.io.File("build/widget-previews").apply { mkdirs() }
      val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
      view.draw(android.graphics.Canvas(bitmap))
      java.io.File(output, "upcoming-${width}x${height}-large-font.png").outputStream().use {
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
      }
    }
  }

}
