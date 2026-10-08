package top.zwtx.daysmatter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.RemoteViews
import top.zwtx.daysmatter.*
import top.zwtx.daysmatter.data.LocalStore
import top.zwtx.daysmatter.data.forDisplay
import top.zwtx.daysmatter.data.isUpcomingWithin
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class UpcomingWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { ids.forEach { update(context, it) }; WidgetRefreshScheduler.requestNetwork(context) }
  override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) { update(context, id) }
  override fun onDeleted(context: Context, ids: IntArray) { ids.forEach { WidgetStore(context).remove(it) } }
  override fun onDisabled(context: Context) { WidgetRefreshScheduler.ensureScheduled(context) }

  companion object {
    private val rows = intArrayOf(R.id.upcoming_row_1, R.id.upcoming_row_2, R.id.upcoming_row_3, R.id.upcoming_row_4, R.id.upcoming_row_5)
    private val items = intArrayOf(R.id.upcoming_item_1, R.id.upcoming_item_2, R.id.upcoming_item_3, R.id.upcoming_item_4, R.id.upcoming_item_5)
    private val dates = intArrayOf(R.id.upcoming_date_1, R.id.upcoming_date_2, R.id.upcoming_date_3, R.id.upcoming_date_4, R.id.upcoming_date_5)
    private val countdowns = intArrayOf(R.id.upcoming_days_1, R.id.upcoming_days_2, R.id.upcoming_days_3, R.id.upcoming_days_4, R.id.upcoming_days_5)
    fun instanceIds(context: Context) = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, UpcomingWidgetProvider::class.java))
    fun updateAll(context: Context) { instanceIds(context).forEach { update(context, it) } }
    fun update(context: Context, id: Int, now: Instant = Instant.now()) {
      AppWidgetManager.getInstance(context).updateAppWidget(id, render(context, id, now))
    }
    fun homeIntent(context: Context, binding: UpcomingBinding, create: Boolean = false, refresh: Boolean = false): Intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        data = Uri.parse("daysmatter://home/${binding.userId}/${binding.categoryId}/${if (create) "add" else if (refresh) "refresh" else "view"}")
        putExtra("widget_home", true); putExtra("backend", binding.backend.trimEnd('/')); putExtra("user_id", binding.userId)
        putExtra("category_id", binding.categoryId); putExtra("create_event", create); putExtra("widget_refresh", refresh)
      }
    fun render(context: Context, id: Int, now: Instant = Instant.now(), binding: UpcomingBinding? = WidgetStore(context).upcoming(id),
      appearance: top.zwtx.daysmatter.data.AppearanceMode = WidgetStore(context).appearance(id), previewHeight: Int? = null): RemoteViews {
      val views = RemoteViews(context.packageName, R.layout.upcoming_widget)
      val store = LocalStore(context)
      val session = store.loadSession()
      val owned = binding != null && binding.userId == session?.userId && binding.backend.trimEnd('/') == BuildConfig.API_BASE_URL.trimEnd('/')
      val snapshot = if (owned) store.loadSnapshot(binding!!.userId) else null
      val category = snapshot?.categories?.find { it.id == binding?.categoryId }
      val deleted = snapshot != null && binding!!.categoryId != 0 && category == null
      val height = previewHeight ?: AppWidgetManager.getInstance(context).getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
      val limit = when { height >= 300 -> 5; height < 180 -> 2; else -> 3 }
      val events = if (deleted) emptyList() else snapshot?.events?.filter { binding!!.categoryId == 0 || it.categoryId == binding.categoryId }
        ?.map { it.forDisplay(now) } ?: emptyList()
      val upcoming = events.filter { it.isUpcomingWithin(binding?.window ?: 30) }
        .sortedWith(compareBy<top.zwtx.daysmatter.data.Event> { it.daysDiff }.thenByDescending { it.pinned }.thenBy { it.name }.thenBy { it.id })
      val unknown = events.count { it.daysDiff == null }
      val message = when {
        session == null -> "请登录以查看近期日程"
        binding == null -> "请选择窗口和分类"
        !owned -> "请登录所属账号或重新配置"
        snapshot == null -> "尚未同步，请打开应用同步"
        deleted -> "分类已删除，请重新选择"
        upcoming.isEmpty() -> "近期无已确认日程"
        else -> ""
      }
      val title = "近期 ${binding?.window ?: 30} 天 · ${category?.name ?: "全部"}"
      views.setTextViewText(R.id.upcoming_title, "近期日程")
      views.setTextViewText(R.id.upcoming_scope, if (owned && !deleted) "未来 ${binding!!.window} 天 · ${category?.name ?: "全部分类"}" else "把值得期待的日子放在桌面")
      val open = if (owned && !deleted) homeIntent(context, binding!!) else if (session == null || (!owned && binding != null))
        Intent(context, MainActivity::class.java) else ImportantDayWidgetProvider.configurationIntent(context, id)
      fun click(viewId: Int, intent: Intent, key: Int) {
        views.setOnClickPendingIntent(viewId, PendingIntent.getActivity(context, id * 10 + key, intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
      }
      click(R.id.upcoming_title, open, 0)
      click(R.id.upcoming_scope, open, 0)
      click(R.id.upcoming_all, open, 0)
      views.setViewVisibility(R.id.upcoming_all, if (owned && !deleted) View.VISIBLE else View.GONE)
      val labels = mutableListOf<String>()
      rows.forEachIndexed { index, row ->
        val event = upcoming.getOrNull(index).takeIf { index < limit }
        val date = event?.nextOccurrence?.substring(5)?.replace('-', '/') ?: ""
        val days = event?.let { if (it.daysDiff == 0) "今天" else "${it.daysDiff} 天" } ?: ""
        val text = event?.let { "$date · ${if (it.daysDiff == 0) "今天" else "还有 ${it.daysDiff} 天"} · ${it.name}" } ?: ""
        labels.add(text)
        views.setTextViewText(row, event?.name ?: "")
        views.setTextViewText(dates[index], date)
        val countdown = SpannableString(days).apply {
          if (days.endsWith(" 天")) setSpan(RelativeSizeSpan(0.6f), days.length - 2, days.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
          else if (days == "今天") setSpan(RelativeSizeSpan(0.8f), 0, days.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        views.setTextViewText(countdowns[index], countdown)
        views.setViewVisibility(items[index], if (event != null) View.VISIBLE else View.GONE)
        views.setContentDescription(items[index], text)
        val destination = event?.let { EventTarget(binding!!.backend, binding.userId, it.id).intent(context) } ?: open
        // Attach the action to both the full row and its text for all launcher hosts.
        listOf(items[index], row, dates[index], countdowns[index]).forEach { click(it, destination, index + 1) }
      }
      views.setViewVisibility(R.id.upcoming_empty, if (upcoming.isEmpty()) View.VISIBLE else View.GONE)
      views.setTextViewText(R.id.upcoming_empty_title, message)
      views.setTextViewText(R.id.upcoming_empty_hint, if (owned && snapshot != null && !deleted) "未来 ${binding!!.window} 天，暂无已确认的事件" else "轻点卡片，打开应用继续")
      // At the minimum height, prioritize the useful message over the decorative icon.
      views.setViewVisibility(R.id.upcoming_empty_icon, if (height >= 180) View.VISIBLE else View.GONE)
      click(R.id.upcoming_empty, open, 0)
      val status = if (snapshot != null && !deleted) {
        val time = if (snapshot.syncedAt > 0) Instant.ofEpochMilli(snapshot.syncedAt).atZone(ZoneId.of("Asia/Shanghai"))
          .format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) else "尚未获取"
        "${if (unknown > 0) "$unknown 个日期待同步 · " else ""}${WidgetStore(context).syncState(session!!.userId).label} · 上次同步 $time"
      } else ""
      views.setTextViewText(R.id.upcoming_status, status)
      views.setViewVisibility(R.id.upcoming_add, if (height >= 240 && owned && !deleted) View.VISIBLE else View.GONE)
      views.setViewVisibility(R.id.upcoming_refresh, if (height >= 240 && owned && !deleted) View.VISIBLE else View.GONE)
      if (owned && !deleted) {
        click(R.id.upcoming_add, homeIntent(context, binding!!, create = true), 6)
        click(R.id.upcoming_refresh, homeIntent(context, binding, refresh = true), 7)
      }
      views.setContentDescription(R.id.widget_root, (listOf(if (owned && !deleted) title else "近期日程", message) + labels + status).filter { it.isNotBlank() }.joinToString("，"))
      views.applyWidgetAppearance(context, appearance, rows + intArrayOf(R.id.upcoming_title, R.id.upcoming_empty_title),
        dates + intArrayOf(R.id.upcoming_scope, R.id.upcoming_status, R.id.upcoming_empty_hint),
        countdowns + intArrayOf(R.id.upcoming_all, R.id.upcoming_add, R.id.upcoming_refresh))
      val darkIcon = appearance == top.zwtx.daysmatter.data.AppearanceMode.DARK ||
        (appearance == top.zwtx.daysmatter.data.AppearanceMode.SYSTEM && context.resources.configuration.uiMode and
          android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES)
      if (appearance == top.zwtx.daysmatter.data.AppearanceMode.SYSTEM && android.os.Build.VERSION.SDK_INT >= 31) {
        views.setColorInt(R.id.upcoming_empty_icon, "setColorFilter", android.graphics.Color.parseColor("#3478F6"), android.graphics.Color.parseColor("#8AB4FF"))
      } else {
        views.setInt(R.id.upcoming_empty_icon, "setColorFilter", android.graphics.Color.parseColor(if (darkIcon) "#8AB4FF" else "#3478F6"))
      }
      return views
    }
  }
}
