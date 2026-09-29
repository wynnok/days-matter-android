package top.zwtx.daysmatter.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Seam under test: [sanitizePasswordInput] — the character filter applied to
 * password inputs on the auth screen.
 */
class PasswordInputTest {

  @Test
  fun `valid password with letters digits and halfwidth symbols passes through unchanged`() {
    val input = "Abc123!@#\$%^&*()-_=+[]{}"
    assertEquals(input, sanitizePasswordInput(input))
  }

  @Test
  fun `chinese, fullwidth and emoji characters are stripped`() {
    assertEquals("abc", sanitizePasswordInput("中文abc"))
    assertEquals("", sanitizePasswordInput("密码测试"))
    assertEquals("a", sanitizePasswordInput("ａa"))
    assertEquals("", sanitizePasswordInput("😀"))
  }

  @Test
  fun `spaces are stripped`() {
    assertEquals("ab", sanitizePasswordInput("a b"))
    assertEquals("", sanitizePasswordInput("   "))
  }

  @Test
  fun `all printable non-space ASCII characters are accepted`() {
    val input = ('!'..'~').joinToString("")
    assertEquals(input, sanitizePasswordInput(input))
  }

  @Test
  fun `mixed pasted text removes controls and preserves valid character order`() {
    assertEquals("Ab9!~", sanitizePasswordInput("中A\nb\t９9！!\r~\u007f"))
    assertEquals("", sanitizePasswordInput(""))
  }
}
