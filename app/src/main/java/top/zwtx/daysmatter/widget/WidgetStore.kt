package top.zwtx.daysmatter.widget

import android.content.Context
import org.json.JSONObject
import top.zwtx.daysmatter.EventTarget

/** Widget configuration belongs to an individual device instance. */
class WidgetStore(context: Context) {
  private val preferences = context.getSharedPreferences("desktop_widgets", Context.MODE_PRIVATE)
  fun binding(id: Int): EventTarget? = runCatching {
    EventTarget.fromJson(JSONObject(preferences.getString("instance_$id", null) ?: return null))
  }.getOrNull()
  fun bind(id: Int, target: EventTarget) { preferences.edit().putString("instance_$id", target.toJson().toString()).apply() }
  fun remove(id: Int) { preferences.edit().remove("instance_$id").apply() }
  fun recordSyncState(userId: Int, status: String) { preferences.edit().putString("sync_$userId", status).apply() }
  fun syncState(userId: Int): String = preferences.getString("sync_$userId", "缓存") ?: "缓存"
}
