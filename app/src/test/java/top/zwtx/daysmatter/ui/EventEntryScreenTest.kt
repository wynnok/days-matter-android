package top.zwtx.daysmatter.ui

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sun.net.httpserver.HttpServer
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
import top.zwtx.daysmatter.data.ApiClient
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class EventEntryScreenTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private lateinit var vm: MainViewModel
  private lateinit var server: HttpServer
  private lateinit var backend: String
  private val app: Application get() = RuntimeEnvironment.getApplication()
  @Volatile private var user = 7
  @Volatile private var empty = false
  @Volatile private var fail = false
  private val exportStarted = CountDownLatch(1)
  private var releaseExport = CountDownLatch(0)
  private var release = CountDownLatch(0)

  @Before fun start() {
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.executor = java.util.concurrent.Executors.newCachedThreadPool()
    server.createContext("/") { exchange ->
      if (exchange.requestURI.path == "/data/export") { exportStarted.countDown(); releaseExport.await(5, TimeUnit.SECONDS) }
      val isEvents = exchange.requestURI.path == "/events"
      if (isEvents) release.await(5, TimeUnit.SECONDS)
      val data: Any = when (exchange.requestURI.path) {
        "/auth/login" -> JSONObject().put("user_id", user).put("name", "账号$user")
          .put("email", "test@example.com").put("token", "synthetic-token")
        "/user/info" -> JSONObject().put("nickname", "账号$user").put("email", "test@example.com")
        "/data/export" -> JSONObject().put("version", "1.0.0")
        "/events" -> if (empty) JSONArray() else JSONArray().put(JSONObject().put("event_id", 11)
          .put("event_name", "目标事件").put("server_next_occurrence", "2026-10-20"))
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
    compose.setContent { DaysMatterTheme(false) { DaysMatterApp(vm) } }
  }

  @After fun stop() {
    releaseExport.countDown()
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

  @Test fun loginRestoresOwnedTargetInActualNavigation() {
    open()
    login()
    compose.onNodeWithText("倒数日详情").assertExists()
    compose.onNodeWithText("目标事件").assertExists()
  }

  @Test fun differentAccountCannotOpenSameEventIdentifier() {
    open()
    user = 8
    login()
    compose.onNodeWithText("倒数日详情").assertDoesNotExist()
    compose.onNodeWithText("事件属于其他账号，已返回首页").assertExists()
  }

  @Test fun uncachedDeletedTargetSyncsThenExplainsHomeFallback() {
    login()
    empty = true
    open(99)
    waitForRequest()
    compose.onNodeWithText("倒数日详情").assertDoesNotExist()
    compose.onNodeWithText("事件已删除或不可用，已返回首页").assertExists()
  }

  @Test fun uncachedTargetWithFailedSyncExplainsUnavailable() {
    login()
    fail = true
    open(99)
    waitForRequest()
    compose.onNodeWithText("暂时无法获取事件，已返回首页，请联网后重试").assertExists()
  }

  @Test fun alreadyRunningEntryRejectsAnotherBackend() {
    login()
    open(service = "https://other.example/api")
    compose.onNodeWithText("倒数日详情").assertDoesNotExist()
    compose.onNodeWithText("事件属于其他服务，请在对应服务中打开").assertExists()
  }
  @Test fun uncachedEntryWhileExportIsBusyResumesSyncAfterExport() {
    login()
    releaseExport = CountDownLatch(1)
    compose.runOnIdle { vm.exportData {} }
    assertTrue(exportStarted.await(5, TimeUnit.SECONDS))
    open(99)
    releaseExport.countDown()
    waitForRequest()
    compose.onNodeWithText("事件已删除或不可用，已返回首页").assertExists()
  }
}
