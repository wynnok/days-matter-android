package top.zwtx.daysmatter.widget

import android.widget.FrameLayout
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import top.zwtx.daysmatter.R
import top.zwtx.daysmatter.data.AppearanceMode

@RunWith(RobolectricTestRunner::class)
class WidgetPlatformCompatibilityTest {
  @Test @Config(sdk = [26, 36])
  fun bothWidgetTypesRenderReadableExplicitModesAtMinimumAndTargetVersions() {
    val app = RuntimeEnvironment.getApplication()
    val parent = FrameLayout(app)
    val important = ImportantDayWidgetProvider.render(app, 400, appearance = AppearanceMode.LIGHT).apply(app, parent)
    val recent = UpcomingWidgetProvider.render(app, 401, appearance = AppearanceMode.DARK).apply(app, parent)
    assertEquals("请登录以查看重要日子", important.findViewById<TextView>(R.id.widget_days).text.toString())
    assertEquals("请登录以查看近期日程", recent.findViewById<TextView>(R.id.upcoming_empty_title).text.toString())
    assertEquals(android.graphics.Color.parseColor("#182234"), important.findViewById<TextView>(R.id.widget_days).currentTextColor)
    assertEquals(android.graphics.Color.parseColor("#F3F5FA"), recent.findViewById<TextView>(R.id.upcoming_row_1).currentTextColor)
    assertTrue(important.performClick())
    assertTrue(recent.findViewById<TextView>(R.id.upcoming_title).performClick())
  }

  @Test @Config(sdk = [26, 36], qualifiers = "notnight")
  fun returningToSystemAppearanceResetsColorsWhenHostReusesViews() {
    val app = RuntimeEnvironment.getApplication()
    val parent = FrameLayout(app)
    val view = UpcomingWidgetProvider.render(app, 401, appearance = AppearanceMode.DARK).apply(app, parent)
    UpcomingWidgetProvider.render(app, 401, appearance = AppearanceMode.SYSTEM).reapply(app, view)
    assertEquals(android.graphics.Color.parseColor("#182234"), view.findViewById<TextView>(R.id.upcoming_title).currentTextColor)
    assertEquals(android.graphics.Color.parseColor("#7C8798"), view.findViewById<TextView>(R.id.upcoming_scope).currentTextColor)
    assertEquals(android.graphics.Color.parseColor("#3478F6"), view.findViewById<TextView>(R.id.upcoming_all).currentTextColor)
    assertEquals(android.graphics.Color.parseColor("#FBFCFE"),
      (view.background as android.graphics.drawable.GradientDrawable).color?.defaultColor)
  }
}
