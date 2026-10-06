package top.zwtx.daysmatter.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import top.zwtx.daysmatter.MainViewModel
import top.zwtx.daysmatter.TestKeyStore

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HelpScreenTest {
  @get:Rule val keys = TestKeyStore()
  @get:Rule val compose = createComposeRule()
  @Test fun profileOpensReadableClientHelp() {
    val help = mutableStateOf(false)
    val vm = MainViewModel(RuntimeEnvironment.getApplication())
    compose.setContent {
      DaysMatterTheme(false) {
        if (help.value) HelpScreen(PaddingValues())
        else ProfileScreen(vm, {}, {}, {}, PaddingValues(), onHelp = { help.value = true })
      }
    }
    compose.onNodeWithText("帮助与关于").performScrollTo().performClick()
    compose.onNodeWithText("日期、农历与重复").assertExists()
    compose.onNodeWithText("账号备份").performScrollTo().assertExists()
    compose.onNodeWithText("版本与声明").performScrollTo().assertExists()
    compose.onNodeWithText("打开项目与许可声明").performScrollTo().assertExists()
  }
}
