package top.zwtx.daysmatter.ui

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import top.zwtx.daysmatter.*
import top.zwtx.daysmatter.data.ApiClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class EventEntryScreenTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private lateinit var vm: MainViewModel
  private lateinit var server: AccountTestServer
  private val backend get() = server.baseUrl
  private val app: Application get() = RuntimeEnvironment.getApplication()

  @Before fun start() {
    server = AccountTestServer()
    compose.runOnIdle { vm = MainViewModel(app, ApiClient(backend)) }
    compose.setContent { DaysMatterTheme(false) { DaysMatterApp(vm) } }
  }

  @After fun stop() { server.close() }
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
    server.userId = 8
    login()
    compose.onNodeWithText("倒数日详情").assertDoesNotExist()
    compose.onNodeWithText("事件属于其他账号，已返回首页").assertExists()
  }

  @Test fun uncachedDeletedTargetSyncsThenExplainsHomeFallback() {
    login()
    server.emptyEvents = true
    open(99)
    waitForRequest()
    compose.onNodeWithText("倒数日详情").assertDoesNotExist()
    compose.onNodeWithText("事件已删除或不可用，已返回首页").assertExists()
  }

  @Test fun uncachedTargetWithFailedSyncExplainsUnavailable() {
    login()
    server.failEvents = true
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
    server.exportPaused = CountDownLatch(1)
    compose.runOnIdle { vm.exportData {} }
    assertTrue(server.exportStarted.await(5, TimeUnit.SECONDS))
    open(99)
    server.exportPaused.countDown()
    waitForRequest()
    compose.onNodeWithText("事件已删除或不可用，已返回首页").assertExists()
  }
  @Test fun lateAuthenticationCannotRestoreOldAccountAfterLogoutAndNewLogin() {
    server.loginPaused = CountDownLatch(1)
    val oldResponse = server.loginPaused
    compose.runOnIdle { vm.login("old@example.com", "password") }
    assertTrue(server.loginStarted.await(5, TimeUnit.SECONDS))
    compose.runOnIdle { vm.logout() }
    server.userId = 8
    server.loginPaused = CountDownLatch(0)
    login()
    oldResponse.countDown()
    compose.waitUntil(5_000) { server.loginResponses.get() == 2 }
    // Settle the external HTTP response before draining the UI dispatch queue.
    Thread.sleep(150)
    compose.runOnIdle {
      assertEquals(8, vm.session?.userId)
      assertEquals(8, top.zwtx.daysmatter.data.LocalStore(app).loadSession()?.userId)
    }
  }
}
