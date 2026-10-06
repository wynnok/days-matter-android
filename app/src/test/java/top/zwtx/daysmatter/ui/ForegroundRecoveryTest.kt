package top.zwtx.daysmatter.ui

import android.os.Looper
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import top.zwtx.daysmatter.MainViewModel
import top.zwtx.daysmatter.TestKeyStore
import top.zwtx.daysmatter.data.ApiClient
import top.zwtx.daysmatter.data.forDisplay
import java.net.InetSocketAddress
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForegroundRecoveryTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private lateinit var server: HttpServer
  private lateinit var vm: MainViewModel
  private val executor = Executors.newCachedThreadPool()
  private val reads = AtomicInteger()
  private val writes = AtomicInteger()
  private var rejectReads = false
  private var unauthorized = false
  private var holdRead = CountDownLatch(0)
  private val clock = object : Clock() {
    var now: Instant = Instant.parse("2026-10-05T16:00:00Z")
    override fun instant() = now
    override fun getZone() = ZoneId.of("UTC")
    override fun withZone(zone: ZoneId): Clock = this
  }
  @Before fun start() {
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val rejectThisRead = rejectReads
      val data: Any = when (exchange.requestURI.path) {
        "/auth/login" -> JSONObject().put("user_id", 1).put("name", "测试").put("token", "test-token")
        "/user/info" -> JSONObject().put("nickname", "测试")
        "/events" -> { reads.incrementAndGet(); JSONArray().put(JSONObject().put("event_id", 1).put("event_name", "固定事件")
          .put("server_next_occurrence", "2026-10-06").put("repeat_type", 0))
          .put(JSONObject().put("event_id", 2).put("event_name", "重复事件").put("server_next_occurrence", "2026-10-06").put("repeat_type", 4)) }
        else -> JSONArray()
      }
      if (exchange.requestMethod == "GET") holdRead.await(5, TimeUnit.SECONDS)
      if (exchange.requestMethod != "GET" && exchange.requestURI.path != "/auth/login") writes.incrementAndGet()
      val code = if (exchange.requestMethod == "GET" && unauthorized) 401 else if (exchange.requestMethod == "GET" && rejectThisRead) 503 else 200
      val bytes = JSONObject().put("code", code).put("data", data).toString().toByteArray()
      exchange.sendResponseHeaders(code, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.executor = executor
    server.start()
    compose.runOnIdle {
      vm = MainViewModel(RuntimeEnvironment.getApplication(), ApiClient("http://127.0.0.1:${server.address.port}"), clock)
      vm.login("test@example.com", "password")
    }
    waitIdle()
  }
  private fun waitIdle() = compose.waitUntil(5_000) { shadowOf(Looper.getMainLooper()).idle(); !vm.busy }
  @After fun stop() { holdRead.countDown(); server.stop(0); executor.shutdownNow() }
  @Test fun foregroundSkipsFreshDataAndRefreshesStaleSnapshot() {
    val before = reads.get()
    compose.runOnIdle { vm.refreshIfNeeded() }; waitIdle(); assertEquals(before, reads.get())
    clock.now = clock.now.plusSeconds(120)
    compose.runOnIdle { vm.refreshIfNeeded() }; waitIdle()
    assertEquals(before + 1, reads.get())
    assertEquals(clock.millis(), vm.snapshot!!.syncedAt)
  }
  @Test fun offlineAcrossDaysKeepsSuccessTimeAndOnlyAdvancesConfirmedDates() {
    val successTime = vm.snapshot!!.syncedAt
    compose.runOnIdle { vm.networkChanged(false) }
    clock.now = clock.now.plusSeconds(5 * 86400)
    compose.setContent {
      val display = vm.snapshot!!.copy(events = vm.snapshot!!.events.map { it.forDisplay(clock.instant()) })
      DaysMatterTheme(false) { HomeScreen(display, vm.gridMode, vm.offline, vm.syncFailed, vm.refreshing, 0, {}, {}, Modifier, PaddingValues()) }
    }
    compose.onAllNodesWithText("5")[0].assertExists()
    compose.onAllNodesWithText("天前")[0].assertExists()
    compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("重复事件"))
    compose.onNodeWithText("待同步下一次日期", substring = true).assertExists()
    compose.runOnIdle {
      vm.refreshIfNeeded()
      vm.saveEvent(null, JSONObject().put("event_name", "草稿"), top.zwtx.daysmatter.data.LocalReminder()) { error("离线不应保存") }
      vm.write("POST", "/events", JSONObject().put("event_name", "草稿"))
      vm.prepareImport("""{"data":{"categories":[],"events":[]}}""")
      vm.confirmImport()
    }
    waitIdle()
    assertEquals(successTime, vm.snapshot!!.syncedAt)
    assertEquals(0, writes.get())
    assertNotNull(vm.importPreview)
    compose.runOnIdle { vm.networkChanged(true) }; waitIdle()
    assertEquals(clock.millis(), vm.snapshot!!.syncedAt)
    assertFalse(vm.offline)
  }
  @Test fun recoveringNetworkWhileOldRefreshFailsIsNotLost() {
    val before = reads.get()
    rejectReads = true; holdRead = CountDownLatch(1)
    compose.runOnIdle { vm.refresh() }
    compose.waitUntil(5_000) { reads.get() > before }
    compose.runOnIdle { vm.networkChanged(false); vm.networkChanged(true) }
    rejectReads = false; holdRead.countDown()
    waitIdle()
    assertFalse(vm.syncFailed)
    assertEquals(before + 2, reads.get())
  }
  @Test fun failedForegroundRetainsCacheAndRecoveryRefreshesOrInvalidatesAccount() {
    val original = vm.snapshot
    rejectReads = true; clock.now = clock.now.plusSeconds(120)
    compose.runOnIdle { vm.refreshIfNeeded() }; waitIdle()
    assertEquals(original, vm.snapshot)
    assertTrue(vm.syncFailed)
    rejectReads = false
    compose.runOnIdle { vm.networkChanged(true) }; waitIdle()
    assertFalse(vm.syncFailed)
    unauthorized = true; clock.now = clock.now.plusSeconds(120)
    compose.runOnIdle { vm.refreshIfNeeded() }; waitIdle()
    assertNull(vm.session)
    assertNull(vm.snapshot)
  }
}
