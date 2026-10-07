package top.zwtx.daysmatter.widget

import android.content.Context
import org.json.JSONObject
import top.zwtx.daysmatter.EventTarget
import top.zwtx.daysmatter.data.AppearanceMode

/** Widget configuration belongs to an individual device instance. */
class WidgetStore(context: Context) {
  private val preferences = context.getSharedPreferences("desktop_widgets", Context.MODE_PRIVATE)
  fun binding(id: Int): EventTarget? = runCatching {
    EventTarget.fromJson(JSONObject(preferences.getString("instance_$id", null) ?: return null))
  }.getOrNull()
  fun bind(id: Int, target: EventTarget) { preferences.edit().putString("instance_$id", target.toJson().toString()).apply() }
  fun upcoming(id: Int): UpcomingBinding? = runCatching {
    val json = JSONObject(preferences.getString("upcoming_$id", null) ?: return null)
    UpcomingBinding(json.getString("backend"), json.getInt("user_id"), json.optInt("window", 30), json.optInt("category_id", 0))
  }.getOrNull()
  fun saveUpcoming(id: Int, binding: UpcomingBinding) {
    preferences.edit().putString("upcoming_$id", JSONObject().put("backend", binding.backend.trimEnd('/'))
      .put("user_id", binding.userId).put("window", binding.window).put("category_id", binding.categoryId).toString()).apply()
  }
  fun appearance(id: Int): AppearanceMode = AppearanceMode.entries.firstOrNull {
    it.name == preferences.getString("appearance_$id", null)
  } ?: AppearanceMode.SYSTEM
  fun saveAppearance(id: Int, mode: AppearanceMode) { preferences.edit().putString("appearance_$id", mode.name).apply() }
  fun remove(id: Int) { preferences.edit().remove("instance_$id").remove("upcoming_$id").remove("appearance_$id").apply() }
  fun recordSyncState(userId: Int, status: WidgetSyncState) { preferences.edit().putString("sync_$userId", status.name).apply() }
  fun syncState(userId: Int): WidgetSyncState {
    val stored = preferences.getString("sync_$userId", null)
    return WidgetSyncState.entries.firstOrNull { it.name == stored || it.label == stored } ?: WidgetSyncState.CACHED
  }
}

data class UpcomingBinding(val backend: String, val userId: Int, val window: Int = 30, val categoryId: Int = 0)

enum class WidgetSyncState(val label: String) { CACHED("缓存"), OFFLINE("离线缓存"), FAILED("同步失败，显示缓存") }
