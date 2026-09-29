package io.github.wynnok.daysmatter.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.wynnok.daysmatter.data.Event
import io.github.wynnok.daysmatter.data.LocalStore
import io.github.wynnok.daysmatter.data.Snapshot
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ReminderScheduler(private val context: Context, private val store: LocalStore) {
  private val alarms = context.getSystemService(AlarmManager::class.java)
  private val businessZone = ZoneId.of("Asia/Shanghai")

  private fun pendingIntent(userId: Int, eventId: Int, event: Event? = null): PendingIntent {
    val intent = Intent(context, ReminderReceiver::class.java).apply {
      data = Uri.parse("daysmatter://reminder/$userId/$eventId")
      if (event != null) {
        putExtra("event_name", event.name)
        putExtra("event_id", event.id)
      }
    }
    return PendingIntent.getBroadcast(
      context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  fun cancel(userId: Int, eventId: Int) {
    alarms.cancel(pendingIntent(userId, eventId))
  }

  fun cancelAll(userId: Int) {
    store.reminderEventIds(userId).forEach { cancel(userId, it) }
  }

  fun reschedule(userId: Int, snapshot: Snapshot) {
    val currentIds = snapshot.events.mapTo(mutableSetOf()) { it.id }
    store.reminderEventIds(userId).forEach { id ->
      cancel(userId, id)
      if (id !in currentIds) store.removeLocalReminder(userId, id)
    }
    snapshot.events.forEach { event ->
      val preference = store.localReminder(userId, event.id)
      if (!preference.enabled) return@forEach
      val occurrence = event.nextOccurrence ?: return@forEach
      val triggerAt = try {
        LocalDate.parse(occurrence)
          .minusDays(preference.advanceDays.toLong())
          .atTime(LocalTime.parse(preference.time))
          .atZone(businessZone)
          .toInstant()
          .toEpochMilli()
      } catch (_: Exception) {
        return@forEach
      }
      if (triggerAt <= System.currentTimeMillis()) return@forEach
      alarms.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(userId, event.id, event)
      )
    }
  }
}
