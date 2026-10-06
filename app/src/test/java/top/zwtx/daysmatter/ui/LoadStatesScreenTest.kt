package top.zwtx.daysmatter.ui

import android.os.Looper
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class LoadStatesScreenTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private lateinit var vm: MainViewModel
  private lateinit var server: HttpServer
  @Volatile private var eventCode = 200
  @Volatile private var missingData = false
  @Volatile private var emptyEvents = false
  @Volatile private var closeEvents = false
  private var releaseEvents = CountDownLatch(0)
  private val profileView = mutableStateOf(false)
  @Volatile private var now = Instant.parse("2026-10-05T16:00:00Z")
  private val clock = object : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = Clock.fixed(instant(), zone)
    override fun instant(): Instant = now
  }

  @Before fun start() {
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val events = exchange.requestURI.path == "/events"
      if (events) {
        releaseEvents.await(5, TimeUnit.SECONDS)
        if (closeEvents) {
          exchange.close()
          return@createContext
        }
      }
      val code = if (events) eventCode else 200
      val data: Any = when (exchange.requestURI.path) {
        "/auth/login" -> JSONObject().put("user_id", 1).put("name", "测试")
          .put("email", "test@example.com").put("token", "test-token")
        "/user/info" -> JSONObject().put("nickname", "测试").put("email", "test@example.com")
        "/events" -> if (emptyEvents) JSONArray() else JSONArray().put(JSONObject()
          .put("event_id", 1).put("event_name", "已有日子").put("target_date", "2026-10-06")
          .put("server_next_occurrence", "2026-10-06").put("server_days_diff", 0))
        else -> JSONArray()
      }
      val body = JSONObject().put("code", code).put("message", "服务暂不可用")
      if (!events || !missingData) body.put("data", data)
      val bytes = body.toString().toByteArray()
      exchange.sendResponseHeaders(code, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    compose.runOnIdle {
      vm = MainViewModel(RuntimeEnvironment.getApplication(), ApiClient("http://127.0.0.1:${server.address.port}"), clock)
    }
    compose.setContent {
      DaysMatterTheme(false) {
        if (vm.session == null) AuthScreen(vm, Modifier, PaddingValues())
        else if (profileView.value) ProfileScreen(vm, {}, {}, {}, PaddingValues()) else {
          HomeScreen(vm.snapshot, false, vm.offline, vm.syncFailed, vm.refreshing, 0,
            vm::refresh, {}, Modifier, PaddingValues())
          if (vm.offline) OfflineBanner(vm.snapshot?.syncedAt)
        }
        vm.message?.let { Text(it) }
      }
    }
  }

  @After fun stop() { releaseEvents.countDown(); server.stop(0) }

  private fun login() {
    compose.runOnIdle { vm.login("test@example.com", "test-password") }
    waitForRequest()
  }

  private fun waitForRequest() {
    compose.waitUntil(5_000) {
      shadowOf(Looper.getMainLooper()).idle()
      !vm.busy
    }
  }

  @Test fun missingResponseDataIsNotASuccessfulEmptyAccount() {
    missingData = true
    login()
    compose.onNodeWithText("暂时无法同步，请下拉重试").assertExists()
    compose.onNodeWithText("还没有倒数日，点击下方 + 添加").assertDoesNotExist()
  }

  @Test fun realEmptyAccountDiffersFromFirstFailureAndCanRetry() {
    eventCode = 503
    login()
    compose.onNodeWithText("暂时无法同步，请下拉重试").assertExists()
    eventCode = 200
    emptyEvents = true
    compose.runOnIdle { vm.refresh() }
    waitForRequest()
    compose.onNodeWithText("还没有倒数日，点击下方 + 添加").assertExists()
  }

  @Test fun refreshFailureKeepsExistingDataAndOffersRetry() {
    login()
    eventCode = 503
    now = now.plusSeconds(3600)
    compose.runOnIdle { vm.refresh() }
    waitForRequest()
    compose.onAllNodesWithText("已有日子", substring = true).assertCountEquals(2)
    compose.onNodeWithText("同步失败，显示上次成功获取的数据").assertExists()
    compose.onNodeWithText("重试同步").assertExists()
    compose.runOnIdle { profileView.value = true }
    compose.onNodeWithText("上次成功获取：10月6日 00:00").assertExists()
    compose.runOnIdle { profileView.value = false }
    eventCode = 200
    compose.onNodeWithText("重试同步").performClick()
    waitForRequest()
    compose.onNodeWithText("同步失败，显示上次成功获取的数据").assertDoesNotExist()
    compose.runOnIdle { profileView.value = true }
    compose.onNodeWithText("上次成功获取：10月6日 01:00").assertExists()
  }

  @Test fun expiredSessionShowsLoginAndClearsOldAccountData() {
    login()
    eventCode = 401
    compose.runOnIdle { vm.refresh() }
    waitForRequest()
    compose.onNodeWithText("进入我的日子").assertExists()
    compose.onNodeWithText("登录已失效，请重新登录").assertExists()
    compose.onNodeWithText("已有日子", substring = true).assertDoesNotExist()
  }

  @Test fun firstRequestHasAVisibleLoadingState() {
    releaseEvents = CountDownLatch(1)
    compose.runOnIdle { vm.login("test@example.com", "test-password") }
    compose.waitUntil(5_000) {
      shadowOf(Looper.getMainLooper()).idle()
      vm.session != null
    }
    compose.onNodeWithText("正在同步倒数日…").assertExists()
    releaseEvents.countDown()
    waitForRequest()
  }

  @Test fun firstNetworkFailureDoesNotClaimACacheExists() {
    closeEvents = true
    login()
    compose.onNodeWithText("当前离线，暂无已同步数据。请联网后重试。").assertExists()
    compose.onNodeWithText("还没有倒数日，点击下方 + 添加").assertDoesNotExist()
  }
}
