package top.zwtx.daysmatter.ui

import android.os.Looper
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
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
import top.zwtx.daysmatter.data.Snapshot
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChannelScreenTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private lateinit var server: HttpServer
  private lateinit var vm: MainViewModel
  private var active = true
  private var writeCode = 200
  private var failRefresh = false
  private var failReads = false
  private var releaseWrite = CountDownLatch(0)
  private val writes = AtomicInteger()
  private var payload = JSONObject()

  @Before fun start() {
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val path = exchange.requestURI.path
      var code = 200
      val secondAccount = exchange.requestHeaders.getFirst("X-User-Id") == "2"
      val data: Any = when {
        path == "/auth/login" -> {
          val email = JSONObject(exchange.requestBody.bufferedReader().readText()).getString("email")
          JSONObject().put("user_id", if (email.startsWith("second")) 2 else 1)
            .put("name", email).put("email", email).put("token", "test-token")
        }
        exchange.requestMethod != "GET" -> {
          payload = JSONObject(exchange.requestBody.bufferedReader().readText().ifBlank { "{}" })
          writes.incrementAndGet()
          releaseWrite.await(5, TimeUnit.SECONDS)
          code = writeCode
          if (code == 200 && payload.has("is_active")) active = payload.getInt("is_active") == 1
          JSONObject()
        }
        path == "/user/info" -> {
          if (writes.get() > 0 && failRefresh) code = 503
          JSONObject().put("nickname", "测试账号").put("email", "test@example.com")
        }
        path == "/remind-channels" -> JSONArray().put(JSONObject()
          .put("channel_id", 7).put("channel_name", if (secondAccount) "新账号渠道" else "测试渠道")
          .put("account", "https://example.com/hook").put("is_active", if (secondAccount || active) 1 else 0))
        path == "/events" -> JSONArray().put(JSONObject().put("event_id", 1)
          .put("remind_channel_id", 7).put("remind_flag", 0))
          .put(JSONObject().put("event_id", 2).put("remind_channel_id", 7).put("remind_flag", 1))
        else -> JSONArray()
      }
      if (failReads && exchange.requestMethod == "GET") code = 503
      val bytes = JSONObject().put("code", code).put("message", "请求被拒绝")
        .put("data", data).toString().toByteArray()
      exchange.sendResponseHeaders(code, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    compose.runOnIdle {
      vm = MainViewModel(RuntimeEnvironment.getApplication(), ApiClient("http://127.0.0.1:${server.address.port}"))
      vm.login("first@example.com", "test-password")
    }
    waitForIdle()
  }

  @After fun stop() { releaseWrite.countDown(); server.stop(0) }

  private fun showList() {
    compose.setContent {
      DaysMatterTheme(false) {
        ChannelListScreen(vm, {}, {}, PaddingValues())
        vm.message?.let { Text(it) }
      }
    }
  }

  private fun showEditor(onSaved: () -> Unit = {}) {
    compose.setContent {
      DaysMatterTheme(false) {
        ChannelEditorScreen(vm, 7, onSaved, PaddingValues())
        vm.message?.let { Text(it) }
      }
    }
  }

  private fun waitForIdle() {
    compose.waitUntil(5_000) {
      shadowOf(Looper.getMainLooper()).idle()
      !vm.busy
    }
  }

  @Test fun toggleShowsReferencesAndConfirmedStateWhenRefreshFails() {
    showList()
    compose.onNodeWithText("站外提醒").assertExists()
    compose.onNodeWithText("被 1 个事件引用").assertExists()
    failRefresh = true
    releaseWrite = CountDownLatch(1)
    compose.onNodeWithContentDescription("停用测试渠道").performClick()
    compose.waitUntil(5_000) { writes.get() == 1 }
    compose.onNodeWithContentDescription("停用测试渠道").assertIsNotEnabled().performClick()
    assertEquals(1, writes.get())
    releaseWrite.countDown()
    waitForIdle()
    assertEquals(0, payload.getInt("is_active"))
    compose.onNodeWithText("generic · 已停用").assertExists()
    compose.onNodeWithText("渠道已停用，但同步失败，请稍后刷新").assertExists()
    assertFalse(vm.store.loadSnapshot(1)!!.channels.single().active)
  }

  @Test fun rejectedToggleKeepsPreviousState() {
    showList()
    writeCode = 400
    compose.onNodeWithContentDescription("停用测试渠道").performClick()
    waitForIdle()
    compose.onNodeWithText("generic · 已启用").assertExists()
    compose.onNodeWithText("请求被拒绝").assertExists()
  }

  @Test fun unauthorizedToggleClearsAccountInsteadOfClaimingSuccess() {
    showList()
    writeCode = 401
    compose.onNodeWithContentDescription("停用测试渠道").performClick()
    waitForIdle()
    compose.onNodeWithText("generic · 已启用").assertDoesNotExist()
    compose.onNodeWithText("登录已失效，请重新登录").assertExists()
    assertEquals(null, vm.session)
  }

  @Test fun referencedChannelCannotBeDeletedEvenWhenInactive() {
    showList()
    compose.onNodeWithContentDescription("停用测试渠道").performClick()
    waitForIdle()
    compose.onNodeWithContentDescription("删除渠道").performClick()
    compose.onNodeWithText("删除", useUnmergedTree = true).performClick()
    compose.onNodeWithText("请先关闭使用此渠道的事件提醒").assertExists()
    assertEquals(1, writes.get())
  }

  @Test fun testRequestCannotSubmitTwiceAndUsesAccurateSuccessMessage() {
    showEditor()
    releaseWrite = CountDownLatch(1)
    compose.onNode(hasText("测试渠道") and !hasSetTextAction()).performScrollTo().performClick()
    compose.waitUntil(5_000) { writes.get() == 1 }
    compose.onNodeWithText("测试中…").assertIsNotEnabled().performClick()
    compose.onNodeWithText("保存渠道").assertIsNotEnabled()
    assertEquals(1, writes.get())
    releaseWrite.countDown()
    waitForIdle()
    compose.onNodeWithText("测试请求已完成，请检查接收端").assertExists()
  }

  @Test fun failedSaveKeepsDraftAndConfirmedSaveReportsRefreshFailure() {
    var saved = false
    showEditor { saved = true }
    compose.onNode(hasText("测试渠道") and hasSetTextAction()).performTextReplacement("新渠道名称")
    writeCode = 400
    compose.onNodeWithText("保存渠道").performScrollTo().performClick()
    waitForIdle()
    assertFalse(saved)
    compose.onNodeWithText("新渠道名称").assertTextContains("新渠道名称")
    compose.onNodeWithText("请求被拒绝").assertExists()
    writeCode = 200
    failRefresh = true
    compose.onNodeWithText("保存渠道").performClick()
    waitForIdle()
    assertEquals(true, saved)
    assertEquals("新渠道名称", payload.getString("channel_name"))
    compose.onNodeWithText("保存成功，但同步失败，请稍后刷新").assertExists()
  }

  @Test fun oldToggleCannotChangeNextAccountChannelWithSameId() {
    showList()
    releaseWrite = CountDownLatch(1)
    compose.onNodeWithContentDescription("停用测试渠道").performClick()
    compose.waitUntil(5_000) { writes.get() == 1 }
    compose.runOnIdle {
      vm.logout()
      vm.login("second@example.com", "test-password")
    }
    releaseWrite.countDown()
    waitForIdle()
    compose.onNodeWithText("新账号渠道").assertExists()
    compose.onNodeWithText("generic · 已启用").assertExists()
    compose.onNodeWithText("渠道已停用").assertDoesNotExist()
  }

  @Test fun missingEventDataShowsPendingReferenceCountInsteadOfZero() {
    compose.runOnIdle {
      val raw = JSONObject(vm.snapshot!!.raw.toString()).also { it.remove("events") }
      vm.store.saveSnapshot(1, Snapshot.fromJson(raw))
      failReads = true
      vm = MainViewModel(RuntimeEnvironment.getApplication(), ApiClient("http://127.0.0.1:${server.address.port}"))
    }
    waitForIdle()
    showList()
    compose.onNodeWithText("引用数量待获取").assertExists()
    compose.onNodeWithText("被 0 个事件引用").assertDoesNotExist()
  }

  @Test fun rejectedTestRequestDoesNotClaimCompletion() {
    showEditor()
    writeCode = 400
    compose.onNode(hasText("测试渠道") and !hasSetTextAction()).performScrollTo().performClick()
    waitForIdle()
    compose.onNodeWithText("请求被拒绝").assertExists()
    compose.onNodeWithText("测试请求已完成，请检查接收端").assertDoesNotExist()
  }
}
