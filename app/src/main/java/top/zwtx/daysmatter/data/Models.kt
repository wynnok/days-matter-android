package top.zwtx.daysmatter.data

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.stringOrNull(key: String): String? =
  if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

data class Session(val userId: Int, val name: String, val email: String, val token: String) {
  companion object {
    fun fromJson(json: JSONObject) = Session(
      json.getInt("user_id"), json.optString("name", ""),
      json.optString("email", ""), json.getString("token")
    )
  }

  fun toJson() = JSONObject()
    .put("user_id", userId)
    .put("name", name)
    .put("email", email)
    .put("token", token)
}

data class Profile(val nickname: String, val email: String, val avatar: String?) {
  companion object {
    fun fromJson(json: JSONObject) = Profile(
      json.stringOrNull("nickname") ?: "", json.stringOrNull("email") ?: "", json.stringOrNull("avatar")
    )
  }
}

data class Category(
  val id: Int,
  val name: String,
  val color: String,
  val icon: String,
  val sortOrder: Int
) {
  companion object {
    fun fromJson(json: JSONObject) = Category(
      json.getInt("category_id"), json.optString("category_name", ""),
      json.optString("color", "#6366f1"), json.optString("icon", "folder"),
      json.optInt("sort_order", 0)
    )
  }
}

data class ReminderChannel(
  val id: Int,
  val name: String,
  val account: String,
  val format: String,
  val template: String?,
  val active: Boolean,
  val hasAuthToken: Boolean
) {
  companion object {
    fun fromJson(json: JSONObject) = ReminderChannel(
      json.getInt("channel_id"), json.stringOrNull("channel_name") ?: "Webhook 提醒",
      json.optString("account", ""), json.optString("format_type", "generic"),
      json.stringOrNull("body_template"), json.optInt("is_active", 1) == 1,
      json.optInt("has_auth_token", 0) == 1
    )
  }
}

data class SubEvent(
  val id: Int,
  val parentId: Int,
  val name: String,
  val targetDate: String,
  val dateType: Int,
  val daysDiff: Int?,
  val nextOccurrence: String?
) {
  companion object {
    fun fromJson(json: JSONObject, fallbackParentId: Int) = SubEvent(
      json.getInt("sub_event_id"), json.optInt("parent_event_id", fallbackParentId),
      json.optString("event_name", ""), json.optString("target_date", ""),
      json.optInt("date_type", 0),
      if (json.isNull("server_days_diff")) null else json.optInt("server_days_diff"),
      json.stringOrNull("server_next_occurrence")
    )
  }
}

data class Event(
  val id: Int,
  val name: String,
  val targetDate: String,
  val dateType: Int,
  val categoryId: Int,
  val categoryName: String,
  val categoryColor: String,
  val categoryIcon: String,
  val repeatType: Int,
  val repeatValue: Int,
  val webhookEnabled: Boolean,
  val channelId: Int?,
  val remindTitle: String?,
  val remindContent: String?,
  val advanceDays: Int,
  val remindTime: String,
  val pinned: Boolean,
  val daysDiff: Int?,
  val nextOccurrence: String?,
  val subEvents: List<SubEvent>
) {
  companion object {
    fun fromJson(json: JSONObject): Event {
      val id = json.getInt("event_id")
      val subEventsJson = json.optJSONArray("sub_event_list") ?: JSONArray()
      return Event(
        id, json.optString("event_name", ""), json.optString("target_date", ""),
        json.optInt("date_type", 0), json.optInt("category_id", 0),
        json.optString("category_name", ""), json.optString("category_color", "#6366f1"),
        json.optString("category_icon", "folder"), json.optInt("repeat_type", 0),
        json.optInt("repeat_value", 1), json.optInt("remind_flag", 1) == 0,
        if (json.isNull("remind_channel_id")) null else json.optInt("remind_channel_id"),
        json.stringOrNull("remind_title"), json.stringOrNull("remind_content"),
        json.optInt("advance_days", 0), json.optString("remind_time", "09:00:00"),
        json.optInt("top_flag", 1) == 0,
        if (json.isNull("server_days_diff")) null else json.optInt("server_days_diff"),
        json.stringOrNull("server_next_occurrence"),
        (0 until subEventsJson.length()).map { SubEvent.fromJson(subEventsJson.getJSONObject(it), id) }
      )
    }
  }
}

data class Snapshot(
  val profile: Profile?,
  val categories: List<Category>,
  val channels: List<ReminderChannel>,
  val events: List<Event>,
  val syncedAt: Long,
  val raw: JSONObject
) {
  companion object {
    fun fromJson(json: JSONObject): Snapshot {
      fun <T> parse(array: JSONArray?, converter: (JSONObject) -> T): List<T> =
        if (array == null) emptyList() else (0 until array.length()).map { converter(array.getJSONObject(it)) }

      return Snapshot(
        json.optJSONObject("profile")?.let(Profile::fromJson),
        parse(json.optJSONArray("categories"), Category::fromJson),
        parse(json.optJSONArray("channels"), ReminderChannel::fromJson),
        parse(json.optJSONArray("events"), Event::fromJson),
        json.optLong("synced_at", 0L), json
      )
    }
  }
}

data class LocalReminder(val enabled: Boolean = false, val advanceDays: Int = 0, val time: String = "09:00") {
  fun toJson() = JSONObject().put("enabled", enabled).put("advance_days", advanceDays).put("time", time)

  companion object {
    fun fromJson(json: JSONObject?) = if (json == null) LocalReminder() else LocalReminder(
      json.optBoolean("enabled", false), json.optInt("advance_days", 0),
      json.optString("time", "09:00")
    )
  }
}
