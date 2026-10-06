package top.zwtx.daysmatter.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun Event.forDisplay(now: Instant = Instant.now()): Event {
  val today = now.atZone(ZoneId.of("Asia/Shanghai")).toLocalDate()
  return copy(
    daysDiff = occurrenceDays(nextOccurrence, today, repeatType != 0),
    subEvents = subEvents.map { sub ->
      sub.copy(daysDiff = occurrenceDays(sub.nextOccurrence, today, false))
    }
  )
}

private fun occurrenceDays(value: String?, today: LocalDate, repeating: Boolean): Int? {
  val confirmed = value?.trim()?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) } ?: return null
  val date = runCatching { LocalDate.parse(confirmed) }.getOrNull() ?: return null
  if (repeating && date.isBefore(today)) return null
  return ChronoUnit.DAYS.between(today, date).toInt()
}
