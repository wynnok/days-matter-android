package top.zwtx.daysmatter.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import top.zwtx.daysmatter.*
import top.zwtx.daysmatter.data.ApiClient
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class BackupSelectionLifecycleTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  private lateinit var vm: MainViewModel
  private lateinit var server: AccountTestServer
  private lateinit var activity: ComponentActivity
  private lateinit var restoration: StateRestorationTester
  private val backup = """{"version":"1.0.0","data":{"categories":[],"events":[],"sub_events":[],"remind_channels":[]}}"""

  @Before fun start() {
    server = AccountTestServer()
    compose.runOnIdle {
      vm = MainViewModel(RuntimeEnvironment.getApplication(), ApiClient(server.baseUrl))
      vm.login("test@example.com", "password")
    }
    waitIdle()
    restoration = StateRestorationTester(compose)
    restoration.setContent {
      activity = LocalContext.current as ComponentActivity
      DaysMatterTheme(false) { DaysMatterApp(vm) }
    }
  }
  @After fun stop() { server.close() }
  private fun waitIdle() = compose.waitUntil(5_000) { shadowOf(Looper.getMainLooper()).idle(); !vm.busy }
  private fun openSelection(export: Boolean): Int {
    compose.onNodeWithText("我的").performClick()
    compose.onNodeWithText("退出登录").performScrollTo()
    compose.onNodeWithText(if (export) "导出数据" else "导入数据").performScrollTo().performClick()
    if (export) { compose.onNodeWithText("选择保存位置").performClick(); waitIdle() }
    var requestCode = 0
    compose.runOnIdle {
      val request = shadowOf(activity).nextStartedActivityForResult
      assertEquals(if (export) Intent.ACTION_CREATE_DOCUMENT else Intent.ACTION_OPEN_DOCUMENT, request.intent.action)
      requestCode = request.requestCode
    }
    return requestCode
  }
  private fun deliver(code: Int, uri: Uri?) {
    compose.runOnIdle {
      activity.activityResultRegistry.dispatchResult(code, if (uri == null) Activity.RESULT_CANCELED else Activity.RESULT_OK,
        uri?.let { Intent().setData(it) })
    }
  }
  @Test fun exportDocumentStillWritesConfirmedContentAfterCompositionRecreation() {
    val code = openSelection(true)
    restoration.emulateSavedInstanceStateRestore()
    val file = File(RuntimeEnvironment.getApplication().cacheDir, "recreated-export.json")
    deliver(code, Uri.fromFile(file))
    compose.waitUntil(5_000) { shadowOf(Looper.getMainLooper()).idle(); file.exists() && vm.lastExport > 0 }
    assertEquals("1.0.0", org.json.JSONObject(file.readText()).getString("version"))
  }
  @Test fun selectedImportStillShowsPreviewAfterCompositionRecreation() {
    val code = openSelection(false)
    restoration.emulateSavedInstanceStateRestore()
    val file = File(RuntimeEnvironment.getApplication().cacheDir, "recreated-import.json").apply { writeText(backup) }
    deliver(code, Uri.fromFile(file))
    compose.waitUntil(5_000) { shadowOf(Looper.getMainLooper()).idle(); vm.importPreview != null }
    compose.onNodeWithText("确认追加导入").assertExists()
    assertEquals("分类 0 · 渠道 0 · 主事件 0 · 子事件 0", vm.importPreview?.summary)
  }
  @Test fun oldImportResultCannotConsumeNewAccountSelection() {
    val code = openSelection(false)
    compose.runOnIdle { vm.logout(); server.userId = 8; vm.login("next@example.com", "password") }
    waitIdle()
    compose.runOnIdle { assertFalse(vm.beginImportSelection()) }
    val file = File(RuntimeEnvironment.getApplication().cacheDir, "old-import.json").apply { writeText(backup) }
    deliver(code, Uri.fromFile(file))
    compose.runOnIdle {
      assertNull(vm.importPreview)
    }
    val nextCode = openSelection(false)
    deliver(nextCode, Uri.fromFile(file))
    compose.waitUntil(5_000) { shadowOf(Looper.getMainLooper()).idle(); vm.importPreview != null }
  }
  @Test fun oldExportResultCannotConsumeNewAccountSelection() {
    val code = openSelection(true)
    compose.runOnIdle { vm.logout(); server.userId = 8; vm.login("next@example.com", "password") }
    waitIdle()
    var relaunched = false
    compose.runOnIdle { vm.exportData { relaunched = true } }
    waitIdle()
    assertFalse(relaunched)
    val file = File(RuntimeEnvironment.getApplication().cacheDir, "old-result.json")
    deliver(code, Uri.fromFile(file))
    assertFalse(file.exists())
    val nextCode = openSelection(true)
    deliver(nextCode, Uri.fromFile(file))
    compose.waitUntil(5_000) { shadowOf(Looper.getMainLooper()).idle(); file.exists() && vm.lastExport > 0 }
  }

  @Test fun cancelledSelectionAfterRecreationDoesNotReportSaved() {
    val code = openSelection(true)
    restoration.emulateSavedInstanceStateRestore()
    deliver(code, null)
    assertEquals("已取消保存备份", vm.message)
    assertEquals(0L, vm.lastExport)
  }
  @Test fun oldAccountSelectionNeverWritesFileAfterLogout() {
    val code = openSelection(true)
    compose.runOnIdle { vm.logout() }
    restoration.emulateSavedInstanceStateRestore()
    val file = File(RuntimeEnvironment.getApplication().cacheDir, "old-account-export.json")
    deliver(code, Uri.fromFile(file))
    assertFalse(file.exists())
    assertEquals(0L, vm.lastExport)
  }
}
