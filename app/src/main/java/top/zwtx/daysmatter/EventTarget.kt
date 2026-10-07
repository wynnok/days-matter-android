package top.zwtx.daysmatter

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject

/** Device entry points retain the service and account, never credentials. */
data class EventTarget(val backend: String, val userId: Int, val eventId: Int) {
  fun intent(context: Context): Intent = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    data = Uri.Builder().scheme("daysmatter").authority("event")
      .appendPath(backend.trimEnd('/')).appendPath(userId.toString()).appendPath(eventId.toString()).build()
    putExtra("backend", backend.trimEnd('/'))
    putExtra("user_id", userId)
    putExtra("event_id", eventId)
  }

  fun toJson() = JSONObject().put("backend", backend.trimEnd('/')).put("user_id", userId).put("event_id", eventId)

  companion object {
    fun fromIntent(intent: Intent): EventTarget? {
      val backend = intent.getStringExtra("backend")?.trimEnd('/') ?: return null
      val user = intent.getIntExtra("user_id", 0)
      val event = intent.getIntExtra("event_id", 0)
      return if (user > 0 && event > 0) EventTarget(backend, user, event) else null
    }
    fun fromJson(json: JSONObject): EventTarget = EventTarget(json.getString("backend"), json.getInt("user_id"), json.getInt("event_id"))
  }
}
