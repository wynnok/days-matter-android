package top.zwtx.daysmatter.ui

import android.os.Looper
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
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
@Config(sdk = [34])
class BackupImportScreenTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private lateinit var server: HttpServer
  private lateinit var vm: MainViewModel
  private val writes = AtomicInteger()
  private val backup = """{"version":"1.0.0","data":{"categories":[{"category_id":1}],"remind_channels":[{"auth_token":"private-token"}],"events":[{"event_id":2}],"sub_events":[]}}"""
  @Before fun start() {
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val data: Any = when (exchange.requestURI.path) {
        "/auth/login" -> JSONObject().put("user_id", 1).put("name", "测试").put("token", "test-token")
        "/data/import" -> { writes.incrementAndGet(); JSONObject() }
        "/user/info" -> JSONObject().put("nickname", "测试")
        "/categories" -> if (writes.get() == 0) JSONArray() else JSONArray().put(JSONObject().put("category_id", 9).put("category_name", "新分类"))
        "/remind-channels" -> if (writes.get() == 0) JSONArray() else JSONArray().put(JSONObject().put("channel_id", 8).put("channel_name", "新渠道"))
        "/events" -> if (writes.get() == 0) JSONArray() else JSONArray().put(JSONObject().put("event_id", 7).put("event_name", "新事件"))
        else -> JSONArray()
      }
      val bytes = JSONObject().put("code", 200).put("data", data).toString().toByteArray()
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    compose.runOnIdle {
      vm = MainViewModel(RuntimeEnvironment.getApplication(), ApiClient("http://127.0.0.1:${server.address.port}"))
      vm.login("test@example.com", "password")
    }
    waitIdle()
    compose.setContent { DaysMatterTheme(false) { BackupImportDialog(vm); vm.message?.let { Text(it) } } }
  }
  private fun waitIdle() = compose.waitUntil(5_000) { shadowOf(Looper.getMainLooper()).idle(); vm.session != null && !vm.busy }
  @After fun stop() { server.stop(0) }
  @Test fun filePreviewNeedsConfirmationThenRefreshesEveryObject() {
    val file = java.io.File(RuntimeEnvironment.getApplication().cacheDir, "backup.json").apply { writeText(backup) }
    compose.runOnIdle { vm.prepareImport(file.readText()) }
    compose.onNodeWithText("确认追加导入").assertExists()
    assertEquals(0, writes.get())
    compose.onNodeWithText("确认追加").performClick()
    waitIdle()
    compose.runOnIdle {
      assertEquals(1, writes.get())
      assertEquals("新分类", vm.snapshot!!.categories.single().name)
      assertEquals("新渠道", vm.snapshot!!.channels.single().name)
      assertEquals("新事件", vm.snapshot!!.events.single().name)
    }
    compose.onNodeWithText("导入请求已完成，账号数据已刷新").assertExists()
  }
  @Test fun canceledOrIncompatibleFilesNeverWrite() {
    compose.runOnIdle { vm.prepareImport(backup) }
    compose.onNodeWithText("取消").performClick()
    compose.runOnIdle { vm.confirmImport(); vm.prepareImport("{}"); vm.confirmImport() }
    assertEquals(0, writes.get())
  }
}
