package top.zwtx.daysmatter.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import top.zwtx.daysmatter.data.LocalStore

class RescheduleReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action !in setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_TIME_CHANGED
      )
    ) return
    val store = LocalStore(context)
    val session = store.loadSession() ?: return
    val snapshot = store.loadSnapshot(session.userId) ?: return
    ReminderScheduler(context, store).reschedule(session.userId, snapshot)
  }
}
