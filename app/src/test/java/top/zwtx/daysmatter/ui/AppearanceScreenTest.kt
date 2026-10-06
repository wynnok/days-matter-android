package top.zwtx.daysmatter.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import top.zwtx.daysmatter.MainViewModel
import top.zwtx.daysmatter.TestKeyStore
import top.zwtx.daysmatter.data.AppearanceMode
import top.zwtx.daysmatter.data.LocalStore

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppearanceScreenTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()

  @Test fun chooseAppearanceFromProfileAndReopenDevicePreference() {
    val application = RuntimeEnvironment.getApplication()
    val vm = MainViewModel(application)
    assertEquals(AppearanceMode.SYSTEM, vm.appearanceMode)
    compose.setContent { DaysMatterTheme(false) { ProfileScreen(vm, {}, {}, {}, PaddingValues()) } }
    for ((label, mode) in listOf("深色模式" to AppearanceMode.DARK,
      "浅色模式" to AppearanceMode.LIGHT, "跟随系统" to AppearanceMode.SYSTEM)) {
      compose.onNodeWithText("外观").performScrollTo().performClick()
      compose.onNodeWithText(label, useUnmergedTree = true).performClick()
      compose.runOnIdle {
        assertEquals(mode, vm.appearanceMode)
        assertEquals(mode, LocalStore(application).appearanceMode())
        assertEquals(mode, MainViewModel(application).appearanceMode)
      }
    }
  }
}
