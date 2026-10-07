package top.zwtx.daysmatter.reminder

import top.zwtx.daysmatter.data.Event
import top.zwtx.daysmatter.data.LocalReminder
import top.zwtx.daysmatter.data.forDisplay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed interface ReminderTiming {
  data class Upcoming(val timestamp: Long) : ReminderTiming
  data object NeedsSync : ReminderTiming
  data object Passed : ReminderTiming
  data object Invalid : ReminderTiming
}

fun reminderTiming(event: Event, preference: LocalReminder, now: Instant = Instant.now()): ReminderTiming {
  if (event.forDisplay(now).daysDiff == null) return ReminderTiming.NeedsSync
  val timestamp = runCatching {
    require(preference.advanceDays >= 0)
    LocalDate.parse(event.nextOccurrence).minusDays(preference.advanceDays.toLong())
      .atTime(LocalTime.parse(preference.time)).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
  }.getOrNull() ?: return ReminderTiming.Invalid
  return if (timestamp <= now.toEpochMilli()) ReminderTiming.Passed else ReminderTiming.Upcoming(timestamp)
}

fun ReminderTiming.label(): String = when (this) {
  is ReminderTiming.Upcoming -> "下一次已知提醒：" + Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("Asia/Shanghai"))
    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "（东八区）"
  ReminderTiming.NeedsSync -> "发生日期待同步，请联网确认下一次日期"
  ReminderTiming.Passed -> "本次提醒时间已过，无法据此判断之前是否送达"
  ReminderTiming.Invalid -> "提醒参数无效，请重新配置"
}
