package top.zwtx.daysmatter.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.zwtx.daysmatter.MainViewModel
import top.zwtx.daysmatter.TestKeyStore
import top.zwtx.daysmatter.data.LocalStore
import top.zwtx.daysmatter.data.ApiClient
import top.zwtx.daysmatter.data.Session
import top.zwtx.daysmatter.data.Snapshot

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-night")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsDesignTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private lateinit var vm: MainViewModel

  @Before fun seed() {
    val app = RuntimeEnvironment.getApplication()
    val store = LocalStore(app)
    store.saveSession(Session(7, "测试用户", "test@example.com", "synthetic-token"))
    store.saveSnapshot(7, Snapshot.fromJson(JSONObject().put("synced_at", System.currentTimeMillis())
      .put("profile", JSONObject().put("nickname", "测试用户").put("email", "test@example.com"))
      .put("categories", JSONArray().put(JSONObject().put("category_id", 1).put("category_name", "生活")
        .put("color", "#10B981").put("icon", "folder")))
      .put("events", JSONArray().put(JSONObject().put("event_id", 11).put("event_name", "下一次旅行")
        .put("category_id", 1).put("category_name", "生活").put("server_next_occurrence", java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).plusDays(6).toString())))))
    // Keep this rendering fixture on cached data; never call a production backend.
    vm = MainViewModel(app, ApiClient("http://127.0.0.1:1"))
  }

  @Test fun drawerManagementAndEditorUseThemeForegroundInNightMode() {
    var foreground = 0
    compose.setContent {
      DaysMatterTheme(true) {
        foreground = MaterialTheme.colorScheme.onSurface.toArgb()
        CategoryDrawer(vm, 0, {}, {}, true, 1f)
      }
    }
    compose.onNodeWithText("管理分类").performClick()
    capture("categories-night.png")
    assertUsesColor(compose.onNodeWithText("生活"), foreground)
    assertUsesColor(compose.onNodeWithContentDescription("编辑分类"), foreground)
    compose.onNodeWithContentDescription("编辑分类").performClick()
    compose.onNodeWithText("颜色").performScrollTo()
    assertUsesColor(compose.onNodeWithText("颜色"), foreground)
    assertUsesColor(compose.onNodeWithContentDescription("返回"), foreground)
    capture("category-editor-night.png")
  }

  private fun assertUsesColor(node: SemanticsNodeInteraction, expected: Int) {
    val bitmap = node.captureToImage().asAndroidBitmap()
    var pixels = 0
    for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
      if (bitmap.getPixel(x, y) == expected) pixels++
    }
    assertTrue("暗色分类页面的文字／图标必须使用主题前景色，实际匹配像素数：$pixels", pixels > 3)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test fun settingsPagesShareReadableDayAndNightHierarchy() {
    val page = mutableStateOf("reminders")
    val dark = mutableStateOf(false)
    compose.setContent {
      DaysMatterTheme(dark.value) {
        Scaffold(topBar = { if (page.value != "profile") TopAppBar(title = { Text(when (page.value) {
          "reminders" -> "本地提醒"
          "widgets" -> "桌面小组件"
          "channels" -> "站外提醒"
          "channel-editor" -> "添加渠道"
          else -> "帮助与关于"
        }) }) }) { padding ->
          when (page.value) {
            "reminders" -> LocalReminderScreen(vm, {}, padding)
            "widgets" -> WidgetManagementScreen(vm, {}, padding)
            "channels" -> ChannelListScreen(vm, {}, {}, padding)
            "channel-editor" -> ChannelEditorScreen(vm, null, {}, padding)
            "help" -> HelpScreen(padding)
            else -> ProfileScreen(vm, {}, {}, {}, padding)
          }
        }
      }
    }
    for (night in listOf(false, true)) {
      compose.runOnIdle { dark.value = night }
      for (screen in listOf("reminders", "widgets", "channels", "channel-editor", "help", "profile")) {
        compose.runOnIdle { page.value = screen }
        capture("$screen-${if (night) "night" else "light"}.png")
        when (screen) {
          "reminders" -> compose.onNodeWithText("同步事件").performScrollTo().assertIsDisplayed()
          "widgets" -> {
            compose.onNodeWithText("近期日程").performScrollTo().performClick()
            compose.onNodeWithText("重要日子").assertIsNotSelected()
            compose.onNodeWithText("近期日程").assertIsSelected()
          }
          "profile" -> {
            compose.onNodeWithText("分类管理").assertDoesNotExist()
            compose.onNodeWithText("设备偏好").performScrollTo().assertIsDisplayed()
          }
          "help" -> {
            compose.onNodeWithText("账号备份").performScrollTo().performClick()
            compose.onNodeWithText("JSON 备份包含", substring = true).assertExists()
            compose.onNodeWithText("查看第三方许可").performScrollTo().assertExists()
          }
        }
      }
    }
  }

  private fun capture(name: String) {
    val output = java.io.File("build/ui-previews").apply { mkdirs() }
    compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
      java.io.File(output, name).outputStream().use {
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
      }
    }
  }
}
