package top.zwtx.daysmatter.data

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

enum class AppearanceMode { SYSTEM, LIGHT, DARK }

class LocalStore(private val context: Context) {
  private val preferences = context.getSharedPreferences("days_matter", Context.MODE_PRIVATE)
  private val keyAlias = "days_matter_session_v1"

  private fun secretKey(): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val existing = keyStore.getKey(keyAlias, null) as? SecretKey
    if (existing != null) return existing
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
      KeyGenParameterSpec.Builder(
        keyAlias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .build()
    )
    return generator.generateKey()
  }

  fun saveSession(session: Session) {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey())
    val encrypted = cipher.doFinal(session.toJson().toString().toByteArray(Charsets.UTF_8))
    val bytes = cipher.iv + encrypted
    preferences.edit().putString("session", Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
  }

  fun loadSession(): Session? {
    val encoded = preferences.getString("session", null) ?: return null
    return try {
      val bytes = Base64.decode(encoded, Base64.NO_WRAP)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
      val plain = cipher.doFinal(bytes.copyOfRange(12, bytes.size))
      Session.fromJson(JSONObject(String(plain, Charsets.UTF_8)))
    } catch (_: Exception) {
      preferences.edit().remove("session").apply()
      null
    }
  }

  private fun cacheFile(userId: Int) = context.getFileStreamPath("snapshot_$userId.json")

  fun saveSnapshot(userId: Int, snapshot: Snapshot) {
    cacheFile(userId).writeText(snapshot.raw.toString(), Charsets.UTF_8)
  }

  fun loadSnapshot(userId: Int): Snapshot? = try {
    Snapshot.fromJson(JSONObject(cacheFile(userId).readText(Charsets.UTF_8)))
  } catch (_: Exception) {
    null
  }

  fun gridMode() = preferences.getBoolean("grid_mode", false)

  fun setGridMode(enabled: Boolean) {
    preferences.edit().putBoolean("grid_mode", enabled).apply()
  }

  fun appearanceMode(): AppearanceMode =
    AppearanceMode.entries.firstOrNull { it.name == preferences.getString("appearance_mode", null) }
      ?: AppearanceMode.SYSTEM

  fun setAppearanceMode(mode: AppearanceMode) {
    preferences.edit().putString("appearance_mode", mode.name).apply()
  }

  private fun remindersKey(userId: Int) = "reminders_$userId"

  private fun remindersJson(userId: Int): JSONObject = try {
    JSONObject(preferences.getString(remindersKey(userId), "{}") ?: "{}")
  } catch (_: Exception) {
    JSONObject()
  }

  fun localReminder(userId: Int, eventId: Int): LocalReminder =
    LocalReminder.fromJson(remindersJson(userId).optJSONObject(eventId.toString()))

  fun reminderEventIds(userId: Int): List<Int> {
    val all = remindersJson(userId)
    return all.keys().asSequence().mapNotNull(String::toIntOrNull).toList()
  }

  fun saveLocalReminder(userId: Int, eventId: Int, reminder: LocalReminder) {
    val all = remindersJson(userId)
    all.put(eventId.toString(), reminder.toJson())
    preferences.edit().putString(remindersKey(userId), all.toString()).apply()
  }

  fun removeLocalReminder(userId: Int, eventId: Int) {
    val all = remindersJson(userId)
    all.remove(eventId.toString())
    preferences.edit().putString(remindersKey(userId), all.toString()).apply()
  }

  fun clearAccount(userId: Int) {
    preferences.edit().remove("session").remove(remindersKey(userId)).apply()
    cacheFile(userId).delete()
  }
}
