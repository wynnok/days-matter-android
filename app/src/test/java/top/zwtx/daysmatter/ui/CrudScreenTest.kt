package top.zwtx.daysmatter.ui

import android.os.Looper
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import top.zwtx.daysmatter.MainViewModel
import top.zwtx.daysmatter.TestKeyStore
import top.zwtx.daysmatter.data.ApiClient
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class CrudScreenTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private lateinit var server: HttpServer
  private lateinit var vm: MainViewModel
  @Volatile private var failRefresh = false
  @Volatile private var rejectWrite = false
  private val writes = AtomicInteger()
  private val category = JSONObject().put("category_id", 1).put("category_name", "分类")
    .put("color", "#6366f1").put("icon", "heart")
  private val sub = JSONObject().put("sub_event_id", 11).put("parent_event_id", 1)
    .put("event_name", "旧子事件").put("target_date", "2026-10-06").put("server_next_occurrence", "2026-10-06")
  private val event = JSONObject().put("event_id", 1).put("event_name", "主事件").put("category_id", 1)
    .put("target_date", "2026-10-06").put("server_next_occurrence", "2026-10-06")
    .put("sub_event_list", JSONArray().put(sub))

  @Before fun start() {
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      var code = 200
      val data: Any = when {
        exchange.requestURI.path == "/auth/login" -> JSONObject().put("user_id", 1)
          .put("name", "测试").put("email", "test@example.com").put("token", "test-token")
        exchange.requestMethod != "GET" -> {
          writes.incrementAndGet()
          val target = if (exchange.requestURI.path.startsWith("/categories")) category
            else if (exchange.requestURI.path.startsWith("/sub-events")) sub else event
          val body = JSONObject(exchange.requestBody.bufferedReader().readText().ifBlank { "{}" })
          if (rejectWrite) {
            code = 400
          } else if (exchange.requestMethod == "DELETE" && exchange.requestURI.path == "/sub-events/11") {
            event.put("sub_event_list", JSONArray())
          } else {
            body.keys().forEach { target.put(it, body.get(it)) }
          }
          JSONObject().put("event_id", 1)
        }
        else -> {
          if (writes.get() > 0 && failRefresh) code = 503
          when (exchange.requestURI.path) {
            "/user/info" -> JSONObject().put("nickname", "测试").put("email", "test@example.com")
            "/categories" -> JSONArray().put(category)
            "/events" -> JSONArray().put(event)
            else -> JSONArray()
          }
        }
      }
      val bytes = JSONObject().put("code", code).put("data", data)
        .put("message", if (code == 400) "名称被拒绝" else "同步故障").toString().toByteArray()
      exchange.sendResponseHeaders(code, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    compose.runOnIdle {
      vm = MainViewModel(RuntimeEnvironment.getApplication(), ApiClient("http://127.0.0.1:${server.address.port}"))
      vm.login("test@example.com", "test-password")
    }
    waitForData()
  }

  @After fun stop() { server.stop(0) }

  private fun content(screen: String) {
    val submitted = mutableStateOf(false)
    compose.setContent {
      DaysMatterTheme(false) {
        if (submitted.value) Text("已提交") else when (screen) {
          "category" -> CategoryEditorScreen(vm, 1, { submitted.value = true }, PaddingValues())
          "event" -> EventEditorScreen(vm, 1, { submitted.value = true }, PaddingValues())
          "sub" -> SubEventEditorScreen(vm, 1, 11, { submitted.value = true }, PaddingValues())
          "detail" -> EventDetailScreen(vm, 1, {}, {}, PaddingValues(), vm.snapshot)
          else -> CategoryListScreen(vm, {}, {}, PaddingValues())
        }
        vm.message?.let { Text(it) }
      }
    }
  }

  @Test fun categoryWriteAndRefreshFailureAreSeparateResults() {
    content("category")
    compose.onNode(hasText("分类") and hasSetTextAction()).performTextReplacement("新分类")
    failRefresh = true
    compose.onNodeWithText("保存分类").performScrollTo().performClick()
    waitForData()
    compose.onNodeWithText("已提交").assertExists()
    compose.onNodeWithText("保存成功，但同步失败，请稍后刷新").assertExists()
    org.junit.Assert.assertEquals("新分类", category.getString("category_name"))
    org.junit.Assert.assertEquals("heart", category.getString("icon"))
  }

  @Test fun mainEventSaveDoesNotInviteResubmissionAfterRefreshFailure() {
    content("event")
    compose.onNode(hasText("主事件") and hasSetTextAction()).performTextReplacement("新主事件")
    failRefresh = true
    compose.onNodeWithText("保存倒数日").performScrollTo().performClick()
    waitForData()
    compose.onNodeWithText("已提交").assertExists()
    compose.onNodeWithText("保存成功，但同步失败，请稍后刷新").assertExists()
    org.junit.Assert.assertEquals(1, writes.get())
  }

  @Test fun subEventEditUpdatesTheRealLoadedSnapshot() {
    content("sub")
    compose.onNode(hasText("旧子事件") and hasSetTextAction()).performTextReplacement("新子事件")
    compose.onNodeWithText("保存子事件").performScrollTo().performClick()
    waitForData()
    org.junit.Assert.assertEquals("新子事件", vm.snapshot?.events?.single()?.subEvents?.single()?.name)
    compose.onNodeWithText("已提交").assertExists()
  }

  @Test fun categoryListBlocksDeletionWhileAnEventUsesIt() {
    content("list")
    compose.onNodeWithContentDescription("删除分类").performClick()
    compose.onNodeWithText("删除", substring = false).performClick()
    compose.onNodeWithText("请先移动或删除该分类下的事件").assertExists()
    org.junit.Assert.assertEquals(0, writes.get())
  }

  @Test fun categoryRefusalKeepsTheEditableDraft() {
    content("category")
    compose.onNode(hasText("分类") and hasSetTextAction()).performTextReplacement("未保存草稿")
    rejectWrite = true
    compose.onNodeWithText("保存分类").performScrollTo().performClick()
    waitForData()
    compose.onNodeWithText("未保存草稿").assertExists()
    compose.onNodeWithText("名称被拒绝").assertExists()
    compose.onNodeWithText("已提交").assertDoesNotExist()
  }

  @Test fun deletingASubEventFromTheDetailUpdatesThePage() {
    content("detail")
    compose.onNodeWithContentDescription("删除子事件").performClick()
    compose.onNodeWithText("删除", substring = false).performClick()
    waitForData()
    compose.onNodeWithText("旧子事件").assertDoesNotExist()
    compose.onNodeWithText("删除成功").assertExists()
  }

  private fun waitForData() {
    compose.waitUntil(5_000) {
      shadowOf(Looper.getMainLooper()).idle()
      !vm.busy && vm.session != null
    }
  }
}
