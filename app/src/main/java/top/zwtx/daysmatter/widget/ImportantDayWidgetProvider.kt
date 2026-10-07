package top.zwtx.daysmatter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import top.zwtx.daysmatter.BuildConfig
import top.zwtx.daysmatter.EventTarget
import top.zwtx.daysmatter.MainActivity
import top.zwtx.daysmatter.R
import top.zwtx.daysmatter.data.LocalStore
import top.zwtx.daysmatter.data.forDisplay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ImportantDayWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
    ids.forEach { update(context, it) }
    WidgetRefreshScheduler.requestNetwork(context)
  }
  override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
    update(context, id)
  }
  override fun onDeleted(context: Context, ids: IntArray) { ids.forEach { WidgetStore(context).remove(it) } }
  override fun onDisabled(context: Context) { WidgetRefreshScheduler.ensureScheduled(context) }

  companion object {
    fun instanceIds(context: Context): IntArray = AppWidgetManager.getInstance(context)
      .getAppWidgetIds(ComponentName(context, ImportantDayWidgetProvider::class.java))
    fun updateAll(context: Context) { instanceIds(context).forEach { update(context, it) } }
    fun update(context: Context, id: Int, now: Instant = Instant.now()) {
      AppWidgetManager.getInstance(context).updateAppWidget(id, render(context, id, now))
    }
    fun configurationIntent(context: Context, id: Int): Intent = Intent(context, WidgetConfigurationActivity::class.java)
      .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
      .setData(android.net.Uri.parse("daysmatter://widget/configure/$id"))

    fun render(context: Context, id: Int, now: Instant = Instant.now(),
      target: EventTarget? = WidgetStore(context).binding(id),
      appearance: top.zwtx.daysmatter.data.AppearanceMode = WidgetStore(context).appearance(id)): RemoteViews {
      val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(id)
      val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160)
      val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160)
      val compact = height < 130 && width >= 240
      val small = !compact && height < 200
      val views = RemoteViews(context.packageName,
        when { compact -> R.layout.important_day_widget_compact; small -> R.layout.important_day_widget_small; else -> R.layout.important_day_widget })
      val store = LocalStore(context)
      val session = store.loadSession()
      val sameAccount = target != null && session?.userId == target.userId &&
        target.backend.trimEnd('/') == BuildConfig.API_BASE_URL.trimEnd('/')
      val snapshot = if (sameAccount) store.loadSnapshot(target!!.userId) else null
      val event = snapshot?.events?.find { it.id == target?.eventId }?.forDisplay(now)
      var name = "重要日子"
      var days = ""
      var date = ""
      var category = ""
      var status = ""
      val open: Intent
      when {
        session == null -> {
          days = "请登录以查看重要日子"
          open = target?.intent(context) ?: Intent(context, MainActivity::class.java)
        }
        target == null -> {
          days = "添加一个重要日子"
          open = if (store.loadSnapshot(session.userId)?.events?.isEmpty() == true) {
            Intent(context, MainActivity::class.java).putExtra("create_event", true)
              .putExtra("user_id", session.userId).putExtra("backend", BuildConfig.API_BASE_URL.trimEnd('/'))
          } else configurationIntent(context, id)
        }
        !sameAccount -> {
          days = "请登录所属账号或重新配置"
          open = target.intent(context)
        }
        snapshot == null -> {
          days = "尚未同步，请打开应用同步"
          open = target.intent(context)
        }
        event == null -> {
          days = "事件已删除，请重新选择"
          open = configurationIntent(context, id)
        }
        else -> {
          name = event.name
          days = when {
            event.daysDiff == null -> if (compact || (small && width < 140)) "待同步" else if (event.repeatType != 0) "待同步下一次日期" else "日期待同步"
            event.daysDiff == 0 -> "今天"
            event.daysDiff > 0 -> "还有 ${event.daysDiff} 天"
            else -> "已过 ${-event.daysDiff} 天"
          }
          date = event.nextOccurrence?.let { if (small && width < 140) it else "本次日期：$it" } ?: "本次日期待同步"
          category = event.categoryName + if (event.dateType == 1) " · 农历事件" else ""
          val syncTime = if (snapshot.syncedAt > 0) Instant.ofEpochMilli(snapshot.syncedAt)
            .atZone(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern(if (compact || small) "MM-dd HH:mm" else "yyyy-MM-dd HH:mm"))
            else "尚未获取"
          status = "${WidgetStore(context).syncState(session.userId).label} · 上次同步 $syncTime"
          open = target.intent(context)
        }
      }
      views.setTextViewText(R.id.widget_name, name)
      views.setTextViewText(R.id.widget_days, days)
      views.setTextViewText(R.id.widget_date, date)
      views.setTextViewText(R.id.widget_category, category)
      views.setTextViewText(R.id.widget_status, status)
      views.setViewVisibility(R.id.widget_category, if (!compact && category.isNotBlank() && height >= 220) View.VISIBLE else View.GONE)
      val pending = PendingIntent.getActivity(context, id, open,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
      views.setOnClickPendingIntent(R.id.widget_root, pending)
      views.setContentDescription(R.id.widget_root, listOf(name, days, date, category, status).filter { it.isNotBlank() }.joinToString("，"))
      views.applyWidgetAppearance(appearance, intArrayOf(R.id.widget_name, R.id.widget_days, R.id.widget_date, R.id.widget_category, R.id.widget_status))
      return views
    }
  }
}
