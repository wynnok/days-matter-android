package top.zwtx.daysmatter.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import top.zwtx.daysmatter.data.ApiClient
import top.zwtx.daysmatter.data.AppRepository
import top.zwtx.daysmatter.data.LocalStore
import top.zwtx.daysmatter.data.Session
import top.zwtx.daysmatter.data.Snapshot
import top.zwtx.daysmatter.data.forDisplay
import java.net.InetSocketAddress
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountOverviewScreenTest {
  @get:Rule val compose = createComposeRule()

  @Test fun accountSummarySurvivesLoadedCategoryChanges() {
    val snapshot = mutableStateOf<Snapshot?>(null)
    val category = mutableIntStateOf(0)
    compose.setContent {
      DaysMatterTheme(false) {
        HomeScreen(snapshot.value, false, false, false, false, category.intValue,
          {}, {}, Modifier, PaddingValues())
      }
    }
    summary("事件总数", "—")
    val dates = listOf("2026-10-06", "2026-10-12", "2026-10-13", "2026-11-04",
      "2026-11-05", "2026-10-05", null, "2026-10-05")
    val events = JSONArray()
    dates.forEachIndexed { index, date ->
      events.put(JSONObject().put("event_id", index + 1).put("event_name", "日子${index + 1}")
        .put("category_id", index % 2 + 1).put("repeat_type", if (index == 7) 4 else 0)
        .put("server_next_occurrence", date ?: JSONObject.NULL)
        .put("server_days_diff", 99).put("sub_event_list", JSONArray().put(JSONObject()
          .put("sub_event_id", index + 100).put("server_next_occurrence", "2026-10-06"))))
    }
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val data: Any = when (exchange.requestURI.path) {
        "/events" -> events
        "/user/info" -> JSONObject().put("nickname", "测试账号")
        "/categories" -> JSONArray().put(JSONObject().put("category_id", 1).put("category_name", "分类一"))
          .put(JSONObject().put("category_id", 2).put("category_name", "分类二"))
        else -> JSONArray()
      }
      val bytes = JSONObject().put("code", 200).put("data", data).toString().toByteArray()
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val store = LocalStore(RuntimeEnvironment.getApplication())
      val repository = AppRepository(store, ApiClient("http://127.0.0.1:${server.address.port}"))
      val loaded = runBlocking { repository.refresh(Session(1, "测试", "test@example.com", "test-token")) }
      compose.runOnIdle {
        snapshot.value = loaded.copy(events = loaded.events.map { it.forDisplay(Instant.parse("2026-10-05T16:00:00Z")) })
      }
      summary("事件总数", "8")
      summary("近期 7 天", "2")
      compose.onNodeWithText("其中 2 个事件日期待同步").assertExists()
      for (selected in listOf(1, 2, 0)) {
        compose.runOnIdle { category.intValue = selected }
        summary("事件总数", "8")
        summary("近期 7 天", "2")
        compose.onNodeWithText(if (selected == 0) "8 个日子" else "4 个日子").assertExists()
      }
    } finally {
      server.stop(0)
    }
  }

  private fun summary(label: String, value: String) {
    compose.onNode(hasText(value) and hasText(label)).assertExists()
  }
}
