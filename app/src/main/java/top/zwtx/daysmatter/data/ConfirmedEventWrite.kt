package top.zwtx.daysmatter.data

import org.json.JSONArray
import org.json.JSONObject

/** Retain confirmed writes if the following full fetch fails; never invent an occurrence. */
fun Snapshot.withSavedEvent(id: Int, body: JSONObject, result: JSONObject): Snapshot {
  val existing = raw.optJSONArray("events")?.let { array ->
    (0 until array.length()).map { array.getJSONObject(it) }.find { it.optInt("event_id") == id }
  }
  val saved = JSONObject(existing?.toString() ?: "{}")
  body.keys().forEach { saved.put(it, body.get(it)) }
  result.keys().forEach { saved.put(it, result.get(it)) }
  saved.put("event_id", id)
  val occurrenceChanged = listOf("target_date", "date_type", "repeat_type", "repeat_value")
    .any { saved.opt(it) != existing?.opt(it) }
  if (occurrenceChanged && !result.has("server_next_occurrence")) {
    saved.put("server_next_occurrence", JSONObject.NULL).put("server_days_diff", JSONObject.NULL)
  }
  categories.find { it.id == saved.optInt("category_id") }?.let {
    saved.put("category_name", it.name).put("category_color", it.color).put("category_icon", it.icon)
  }
  return replaceEvent(id, saved)
}

fun Snapshot.withDeletedEvent(id: Int): Snapshot = replaceEvent(id, null)

private fun Snapshot.replaceEvent(id: Int, replacement: JSONObject?): Snapshot {
  val updated = JSONObject(raw.toString())
  val events = updated.optJSONArray("events") ?: JSONArray()
  val kept = JSONArray()
  var replaced = false
  for (index in 0 until events.length()) {
    val event = events.getJSONObject(index)
    if (event.optInt("event_id") == id) {
      replacement?.let { kept.put(it) }
      replaced = true
    } else kept.put(event)
  }
  if (!replaced && replacement != null) kept.put(replacement)
  return Snapshot.fromJson(updated.put("events", kept))
}
