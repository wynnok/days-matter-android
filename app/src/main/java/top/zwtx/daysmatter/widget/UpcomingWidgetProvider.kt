package top.zwtx.daysmatter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
      appearance: top.zwtx.daysmatter.data.AppearanceMode = WidgetStore(context).appearance(id)): RemoteViews {
      val views = RemoteViews(context.packageName, R.layout.upcoming_widget)
      val store = LocalStore(context)
      val session = store.loadSession()
      val owned = binding != null && binding.userId == session?.userId && binding.backend.trimEnd('/') == BuildConfig.API_BASE_URL.trimEnd('/')
      val snapshot = if (owned) store.loadSnapshot(binding!!.userId) else null
      val category = snapshot?.categories?.find { it.id == binding?.categoryId }
      val deleted = snapshot != null && binding!!.categoryId != 0 && category == null
      val height = AppWidgetManager.getInstance(context).getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
      val limit = if (height >= 300) 5 else 3
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
      views.setTextViewText(R.id.upcoming_title, if (owned && !deleted) title else "近期日程")
      val open = if (owned && !deleted) homeIntent(context, binding!!) else if (session == null || (!owned && binding != null))
        Intent(context, MainActivity::class.java) else ImportantDayWidgetProvider.configurationIntent(context, id)
      fun click(viewId: Int, intent: Intent, key: Int) {
        views.setOnClickPendingIntent(viewId, PendingIntent.getActivity(context, id * 10 + key, intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
      }
      click(R.id.upcoming_title, open, 0)
      click(R.id.upcoming_all, open, 0)
      val labels = mutableListOf<String>()
      rows.forEachIndexed { index, row ->
        val event = upcoming.getOrNull(index).takeIf { index < limit }
        val text = event?.let { "${it.nextOccurrence?.substring(5)?.replace('-', '/')} · ${it.name} · ${if (it.daysDiff == 0) "今天" else "还有 ${it.daysDiff} 天"}" }
          ?: if (index == 0) message else ""
        labels.add(text)
        views.setTextViewText(row, text)
        views.setViewVisibility(row, if (text.isNotBlank()) View.VISIBLE else View.GONE)
        click(row, event?.let { EventTarget(binding!!.backend, binding.userId, it.id).intent(context) } ?: open, index + 1)
      }
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
      views.setContentDescription(R.id.widget_root, (listOf(if (owned && !deleted) title else "近期日程") + labels + status).filter { it.isNotBlank() }.joinToString("，"))
      views.applyWidgetAppearance(appearance, rows + intArrayOf(R.id.upcoming_title, R.id.upcoming_status, R.id.upcoming_all, R.id.upcoming_add, R.id.upcoming_refresh))
      return views
    }
  }
}
