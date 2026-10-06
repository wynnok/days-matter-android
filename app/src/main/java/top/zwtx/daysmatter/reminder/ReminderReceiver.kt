package top.zwtx.daysmatter.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import top.zwtx.daysmatter.MainActivity
import top.zwtx.daysmatter.R

class ReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (Build.VERSION.SDK_INT >= 33 &&
      context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return

    val target = top.zwtx.daysmatter.EventTarget.fromIntent(intent) ?: return
    val session = top.zwtx.daysmatter.data.LocalStore(context).loadSession() ?: return
    if (session.userId != target.userId || target.backend != top.zwtx.daysmatter.BuildConfig.API_BASE_URL.trimEnd('/')) return
    val eventName = intent.getStringExtra("event_name") ?: return
    val eventId = intent.getIntExtra("event_id", 0)
    val notifications = context.getSystemService(NotificationManager::class.java)
    val channelId = "event_reminders"
    notifications.createNotificationChannel(
      NotificationChannel(channelId, "事件提醒", NotificationManager.IMPORTANCE_DEFAULT)
    )
    val openApp = PendingIntent.getActivity(
      context, 0, target.intent(context),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = Notification.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(eventName)
      .setContentText("你设定的倒数日提醒已到达")
      .setAutoCancel(true)
      .setContentIntent(openApp)
      .build()
    notifications.notify(eventId, notification)
  }
}
