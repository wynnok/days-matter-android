package top.zwtx.daysmatter.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import top.zwtx.daysmatter.data.Event
import top.zwtx.daysmatter.data.LocalStore
import top.zwtx.daysmatter.data.Snapshot

class ReminderScheduler(private val context: Context, private val store: LocalStore) {
  private val alarms = context.getSystemService(AlarmManager::class.java)

  private fun pendingIntent(userId: Int, eventId: Int, event: Event? = null): PendingIntent {
    val intent = Intent(context, ReminderReceiver::class.java).apply {
      data = Uri.parse("daysmatter://reminder/$userId/$eventId")
      if (event != null) {
        putExtra("event_name", event.name)
        putExtra("event_id", event.id)
        putExtra("user_id", userId)
        putExtra("backend", top.zwtx.daysmatter.BuildConfig.API_BASE_URL.trimEnd('/'))
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
      val timing = reminderTiming(event, preference) as? ReminderTiming.Upcoming ?: return@forEach
      val triggerAt = timing.timestamp
      alarms.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(userId, event.id, event)
      )
    }
  }
}
