package top.zwtx.daysmatter.ui

/** Half-width printable ASCII: letters, digits and symbols, excluding space and control chars. */
private val PasswordAllowedChars: CharRange = '!'..'~'

/**
 * Strips characters that are not allowed in password fields.
 * Only half-width ASCII characters are kept: English letters, digits and symbols
 * (code points 33..126). Chinese, full-width characters, emoji and spaces are removed.
 */
fun sanitizePasswordInput(text: String): String = text.filter { it in PasswordAllowedChars }
