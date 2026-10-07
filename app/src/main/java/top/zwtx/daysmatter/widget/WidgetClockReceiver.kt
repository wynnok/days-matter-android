package top.zwtx.daysmatter.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WidgetClockReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) return
    WidgetUpdates.redraw(context)
    WidgetRefreshScheduler.requestNetwork(context)
  }
}
