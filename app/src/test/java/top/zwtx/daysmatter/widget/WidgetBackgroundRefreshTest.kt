package top.zwtx.daysmatter.widget

import android.app.job.JobScheduler
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.widget.TextView
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import top.zwtx.daysmatter.*
import top.zwtx.daysmatter.data.*
import top.zwtx.daysmatter.ui.AccountTestServer
import java.time.Instant
import java.time.Clock
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetBackgroundRefreshTest {
  @get:Rule val keys = TestKeyStore()
  private val app get() = RuntimeEnvironment.getApplication()
  private val manager get() = AppWidgetManager.getInstance(app)
  private lateinit var server: AccountTestServer
  private val now = Instant.parse("2026-10-07T00:00:00Z")
  private lateinit var store: LocalStore
  private var fixed = 0
  private var recent = 0
  @Before fun start() {
    server = AccountTestServer()
    store = LocalStore(app)
    store.saveSession(Session(7, "User", "test@example.com", "token"))
    store.saveSnapshot(7, Snapshot.fromJson(JSONObject().put("synced_at", now.minusSeconds(7200).toEpochMilli())
      .put("events", JSONArray().put(JSONObject().put("event_id", 11).put("event_name", "已确认缓存")
        .put("server_next_occurrence", "2026-10-08")))))
    fixed = shadowOf(manager).createWidget(ImportantDayWidgetProvider::class.java, R.layout.important_day_widget)
    recent = shadowOf(manager).createWidget(UpcomingWidgetProvider::class.java, R.layout.upcoming_widget)
    shadowOf(manager).putWidgetInfo(fixed, AppWidgetProviderInfo().apply { provider = ComponentName(app, ImportantDayWidgetProvider::class.java) })
    shadowOf(manager).putWidgetInfo(recent, AppWidgetProviderInfo().apply { provider = ComponentName(app, UpcomingWidgetProvider::class.java) })
    WidgetStore(app).bind(fixed, EventTarget(BuildConfig.API_BASE_URL, 7, 11))
    WidgetStore(app).saveUpcoming(recent, UpcomingBinding(BuildConfig.API_BASE_URL, 7))
  }
  @After fun stop() { server.close() }
  private fun text(widget: Int, view: Int) = shadowOf(manager).getViewFor(widget).findViewById<TextView>(view).text.toString()
  @Test(timeout = 20_000) fun systemOpportunitiesRedrawKnownDatesAndScheduleSharedHourlyRecovery() {
    listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED).forEach {
      WidgetClockReceiver().onReceive(app, Intent(it))
    }
    WidgetUpdates.redraw(app, Instant.parse("2026-10-10T00:00:00Z"))
    assertEquals("已过 2 天", text(fixed, R.id.widget_days))
    assertEquals("近期无已确认日程", text(recent, R.id.upcoming_row_1))
    val jobs = app.getSystemService(JobScheduler::class.java).allPendingJobs
    assertTrue(jobs.any { it.isPeriodic && it.intervalMillis >= 3_600_000 && it.isPersisted })
    assertTrue(jobs.any { !it.isPeriodic && it.networkType == android.app.job.JobInfo.NETWORK_TYPE_ANY })
  }
  @Test(timeout = 20_000) fun backgroundFailurePreservesCacheAndTimeThenRecoveryUpdatesBothTypes() = runBlocking {
    server.failEvents = true
    val clock = Clock.fixed(now, ZoneOffset.UTC)
    assertFalse(WidgetSyncJobService.synchronize(app, ApiClient(server.baseUrl), clock))
    assertEquals("明确失败应交还给系统退避，而不是持续发起读取", 1, server.eventRequests.get())
    assertEquals("已确认缓存", text(fixed, R.id.widget_name))
    assertTrue(text(recent, R.id.upcoming_status).contains("同步失败"))
    assertEquals(now.minusSeconds(7200).toEpochMilli(), store.loadSnapshot(7)!!.syncedAt)
    server.failEvents = false
    server.events.getJSONObject(0).put("event_name", "后台获取的新名称").put("server_next_occurrence", "2026-10-08")
    assertTrue(WidgetSyncJobService.synchronize(app, ApiClient(server.baseUrl), clock))
    assertEquals("后台获取的新名称", text(fixed, R.id.widget_name))
    assertTrue(text(recent, R.id.upcoming_row_1).contains("后台获取的新名称"))
    assertEquals(now.toEpochMilli(), store.loadSnapshot(7)!!.syncedAt)
    server.unauthorizedEvents = true
    assertTrue(WidgetSyncJobService.synchronize(app, ApiClient(server.baseUrl), Clock.fixed(now.plusSeconds(7200), ZoneOffset.UTC)))
    assertNull(store.loadSession())
    assertEquals("请登录以查看重要日子", text(fixed, R.id.widget_days))
    assertEquals("请登录以查看近期日程", text(recent, R.id.upcoming_row_1))
  }
  @Test(timeout = 20_000) fun overlappingInstancesShareOneAccountFetchAndLateOldOwnerCannotOverwriteNewAccount() = runBlocking {
    server.eventsPaused = java.util.concurrent.CountDownLatch(1)
    val api = ApiClient(server.baseUrl)
    val clock = Clock.fixed(now, ZoneOffset.UTC)
    val first = async { WidgetSyncJobService.synchronize(app, api, clock) }
    withContext(Dispatchers.IO) { assertTrue(server.eventsStarted.await(5, java.util.concurrent.TimeUnit.SECONDS)) }
    val second = async { WidgetSyncJobService.synchronize(app, api, clock) }
    yield()
    store.saveSession(Session(8, "Other", "other@example.com", "other"))
    store.saveSnapshot(8, Snapshot.fromJson(JSONObject().put("synced_at", now.toEpochMilli())
      .put("events", JSONArray().put(JSONObject().put("event_id", 11).put("event_name", "另一个账号的事件")))))
    server.eventsPaused.countDown()
    assertTrue(first.await())
    assertTrue(second.await())
    assertEquals(1, server.eventRequests.get())
    assertEquals(8, store.loadSession()!!.userId)
    assertEquals("已确认缓存", store.loadSnapshot(7)!!.events.single().name)
    assertEquals("请登录所属账号或重新配置", text(fixed, R.id.widget_days))
    assertEquals("请登录所属账号或重新配置", text(recent, R.id.upcoming_row_1))
  }
  @Test(timeout = 20_000) fun stoppedBackgroundSyncDoesNotCancelForegroundAccountRefresh() = runBlocking {
    server.eventsPaused = java.util.concurrent.CountDownLatch(1)
    val api = ApiClient(server.baseUrl)
    val clock = Clock.fixed(now, ZoneOffset.UTC)
    val background = async { WidgetSyncJobService.synchronize(app, api, clock) }
    withContext(Dispatchers.IO) { assertTrue(server.eventsStarted.await(5, java.util.concurrent.TimeUnit.SECONDS)) }
    val foreground = async { AppRepository(store, api, clock).refresh(store.loadSession()!!) }
    yield()
    background.cancel()
    server.eventsPaused.countDown()
    background.join()
    val confirmed = foreground.await()
    store.saveSnapshot(7, confirmed)
    WidgetUpdates.redraw(app, now)
    assertEquals("目标事件", text(fixed, R.id.widget_name))
    assertTrue(text(recent, R.id.upcoming_row_1).contains("目标事件"))
    assertEquals(now.toEpochMilli(), confirmed.syncedAt)
  }

  @Test(timeout = 20_000) fun offlineRedrawLeavesExpiredRepeatPendingAndNeverCreatesAnotherOccurrence() {
    val raw = store.loadSnapshot(7)!!.raw
    raw.getJSONArray("events").getJSONObject(0).put("repeat_type", 4)
    store.saveSnapshot(7, Snapshot.fromJson(raw))
    WidgetStore(app).recordSyncState(7, WidgetSyncState.OFFLINE)
    WidgetUpdates.redraw(app, Instant.parse("2026-10-10T00:00:00Z"))
    assertEquals("待同步下一次日期", text(fixed, R.id.widget_days))
    assertEquals("本次日期：2026-10-08", text(fixed, R.id.widget_date))
    assertTrue(text(recent, R.id.upcoming_status).contains("1 个日期待同步"))
    assertEquals("近期无已确认日程", text(recent, R.id.upcoming_row_1))
    assertTrue(text(recent, R.id.upcoming_status).contains("离线缓存"))
  }

  @Test(timeout = 20_000) fun confirmedWriteNeverAcceptsAnOlderBackgroundReadAsItsRefresh() = runBlocking {
    server.events.getJSONObject(0).put("event_name", "写入前的名称")
    server.captureEventsBeforePause = true
    server.applyEventWrites = true
    server.eventsPaused = java.util.concurrent.CountDownLatch(1)
    val api = ApiClient(server.baseUrl)
    val clock = Clock.fixed(now, ZoneOffset.UTC)
    val owner = store.loadSession()!!
    val background = async { WidgetSyncJobService.synchronize(app, api, clock) }
    withContext(Dispatchers.IO) { assertTrue(server.eventsStarted.await(5, java.util.concurrent.TimeUnit.SECONDS)) }
    val repository = AppRepository(store, api, clock)
    repository.write(owner, "PUT", "/events/11", JSONObject().put("event_name", "写入后的名称"))
    val foreground = async {
      val data = repository.refresh(owner)
      store.saveSnapshot(owner.userId, data)
      WidgetUpdates.redraw(app, now)
    }
    yield()
    server.eventsPaused.countDown()
    background.await()
    foreground.await()
    assertEquals("写入后的名称", text(fixed, R.id.widget_name))
    assertTrue(text(recent, R.id.upcoming_row_1).contains("写入后的名称"))
    assertEquals("写入后的名称", store.loadSnapshot(7)!!.events.single().name)
  }

}
