package top.zwtx.daysmatter.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import top.zwtx.daysmatter.MainActivity
import top.zwtx.daysmatter.R

class ReminderNotifications(private val context: Context) {
  private val manager = context.getSystemService(NotificationManager::class.java)
  val permissionGranted: Boolean get() = Build.VERSION.SDK_INT < 33 ||
    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
  val appEnabled: Boolean get() = manager.areNotificationsEnabled()
  val channelEnabled: Boolean get() = manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

  fun createChannel() {
    manager.createNotificationChannel(NotificationChannel(CHANNEL, "事件提醒", NotificationManager.IMPORTANCE_DEFAULT))
  }

  fun test(): String {
    createChannel()
    if (!permissionGranted) return "请先授予通知权限"
    if (!appEnabled) return "系统应用通知已关闭，请在系统设置中开启"
    if (!channelEnabled) return "事件提醒渠道已关闭，请在系统设置中开启"
    val open = PendingIntent.getActivity(context, -1, Intent(context, MainActivity::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    return try {
      manager.notify("local_reminder_test", 0, Notification.Builder(context, CHANNEL)
        .setSmallIcon(R.drawable.ic_notification).setContentTitle("本地提醒测试")
        .setContentText("仅验证当前展示，请检查通知栏；不保证未来定时送达")
        .setContentIntent(open).setAutoCancel(true).build())
      "测试通知已提交，请检查通知栏；不保证未来定时送达"
    } catch (_: SecurityException) { "无法显示通知，请检查系统通知权限" }
  }

  companion object { const val CHANNEL = "event_reminders" }
}
