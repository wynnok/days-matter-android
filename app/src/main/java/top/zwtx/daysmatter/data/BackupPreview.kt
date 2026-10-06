package top.zwtx.daysmatter.data

import org.json.JSONArray
import org.json.JSONObject

/** 数量仅来自备份文件，不代表服务端实际追加数量。 */
data class BackupPreview(val data: JSONObject, val summary: String) {
  companion object {
    fun parse(text: String): BackupPreview {
      val backup = JSONObject(text)
      require(!backup.has("version") || backup.opt("version") == "1.0.0") { "备份版本不兼容" }
      val data = backup.optJSONObject("data") ?: error("备份格式不正确")
      fun count(name: String, required: Boolean = false): Int {
        val items = if (!data.has(name) && !required) JSONArray()
          else data.optJSONArray(name) ?: error("备份格式不正确")
        for (index in 0 until items.length()) require(items.opt(index) is JSONObject) { "备份格式不正确" }
        return items.length()
      }
      val categories = count("categories", true)
      val channels = count("remind_channels")
      val events = count("events", true)
      val subEvents = count("sub_events")
      return BackupPreview(backup, "分类 $categories · 渠道 $channels · 主事件 $events · 子事件 $subEvents")
    }
  }
}

enum class ImportOutcome { UNKNOWN, REFRESH_FAILED, COMPLETE }
