package top.zwtx.daysmatter.ui

import android.os.Looper
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileSaveScreenTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private lateinit var server: HttpServer
  private lateinit var vm: MainViewModel
  private var rejectSave = false
  private var failRefresh = false
  private var saved = false
  private var releaseSave = CountDownLatch(0)
  private val writes = AtomicInteger()

  @Before fun start() {
    server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      var code = 200
      val data: Any = when {
        exchange.requestURI.path == "/auth/login" -> {
          val email = JSONObject(exchange.requestBody.bufferedReader().readText()).getString("email")
          val secondAccount = email == "second@example.com"
          JSONObject().put("user_id", if (secondAccount) 2 else 1)
            .put("name", if (secondAccount) "新账号" else "旧昵称").put("email", email).put("token", "test-token")
        }
        exchange.requestMethod == "PUT" -> {
          writes.incrementAndGet()
          releaseSave.await(5, TimeUnit.SECONDS)
          if (rejectSave) code = 400 else saved = true
          JSONObject().put("nickname", "新昵称").put("email", "new@example.com")
        }
        exchange.requestURI.path == "/user/info" -> {
          if (saved && failRefresh) code = 503
          val secondAccount = exchange.requestHeaders.getFirst("X-User-Id") == "2"
          JSONObject().put("nickname", if (secondAccount) "新账号" else if (saved) "新昵称" else "旧昵称")
            .put("email", if (secondAccount) "second@example.com" else if (saved) "new@example.com" else "old@example.com")
        }
        else -> JSONArray()
      }
      val bytes = JSONObject().put("code", code).put("message", if (code == 400) "该邮箱已被其他账号使用" else "同步失败")
        .put("data", data).toString().toByteArray()
      exchange.sendResponseHeaders(code, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    compose.runOnIdle {
      vm = MainViewModel(RuntimeEnvironment.getApplication(), ApiClient("http://127.0.0.1:${server.address.port}"))
      vm.login("old@example.com", "test-password")
    }
    compose.waitUntil(5_000) {
      shadowOf(Looper.getMainLooper()).idle()
      if (!vm.busy && vm.snapshot == null) error(vm.message ?: "未获得账号数据")
      vm.snapshot != null && !vm.busy
    }
    compose.setContent {
      DaysMatterTheme(false) {
        ProfileScreen(vm, {}, {}, {}, PaddingValues())
        vm.message?.let { Text(it) }
      }
    }
    compose.onNodeWithText("编辑").performClick()
    compose.onNode(hasText("旧昵称") and hasSetTextAction()).performTextReplacement("新昵称")
    compose.onNode(hasText("old@example.com") and hasSetTextAction()).performTextReplacement("new@example.com")
  }

  @After fun stop() { releaseSave.countDown(); server.stop(0) }

  @Test fun refusalKeepsDraftAndReason() {
    rejectSave = true
    compose.onNodeWithText("保存").performClick()
    waitForSave()
    compose.onNodeWithText("编辑资料").assertExists()
    compose.onNodeWithText("新昵称").assertTextContains("新昵称")
    compose.onNodeWithText("new@example.com").assertTextContains("new@example.com")
    compose.onNodeWithText("该邮箱已被其他账号使用").assertExists()
  }

  @Test fun savedProfileRemainsVisibleWhenRefreshFails() {
    failRefresh = true
    compose.onNodeWithText("保存").performClick()
    waitForSave()
    compose.onNodeWithText("新昵称").assertExists()
    compose.onNodeWithText("new@example.com").assertExists()
    compose.onNodeWithText("资料已保存，但同步失败，请稍后刷新").assertExists()
  }

  @Test fun waitingSaveIsVisibleAndCannotSubmitTwice() {
    releaseSave = CountDownLatch(1)
    compose.onNodeWithText("保存").performClick()
    compose.waitUntil(5_000) { writes.get() == 1 }
    compose.onNodeWithText("保存中…").assertIsNotEnabled().performClick()
    org.junit.Assert.assertEquals(1, writes.get())
    releaseSave.countDown()
    waitForSave()
    compose.onNodeWithText("新昵称").assertExists()
    compose.onNodeWithText("资料保存成功").assertExists()
  }

  private fun waitForSave() {
    compose.waitUntil(5_000) {
      shadowOf(Looper.getMainLooper()).idle()
      !vm.busy
    }
  }

  @Test fun oldSaveCannotOverwriteTheNextAccount() {
    releaseSave = CountDownLatch(1)
    compose.onNodeWithText("保存").performClick()
    compose.waitUntil(5_000) { writes.get() == 1 }
    compose.runOnIdle {
      vm.logout()
      vm.login("second@example.com", "test-password")
    }
    releaseSave.countDown()
    compose.waitUntil(5_000) {
      shadowOf(Looper.getMainLooper()).idle()
      vm.session?.userId == 2 && !vm.busy && vm.snapshot != null
    }
    compose.onNodeWithText("新账号").assertExists()
    compose.onNodeWithText("second@example.com").assertExists()
    compose.onNodeWithText("资料保存成功").assertDoesNotExist()
  }
}
