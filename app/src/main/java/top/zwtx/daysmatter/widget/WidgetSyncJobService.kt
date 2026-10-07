package top.zwtx.daysmatter.widget

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import kotlinx.coroutines.*
import top.zwtx.daysmatter.data.*
import top.zwtx.daysmatter.reminder.ReminderScheduler
import java.io.IOException
import java.time.Clock
import java.time.ZoneId

class WidgetSyncJobService : JobService() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var running: Job? = null
  override fun onStartJob(params: JobParameters): Boolean {
    WidgetUpdates.redraw(this)
    if (params.jobId == WidgetRefreshScheduler.PERIODIC_JOB) {
      WidgetRefreshScheduler.requestNetwork(this)
      return false
    }
    running = scope.launch {
      val success = synchronize(this@WidgetSyncJobService)
      jobFinished(params, !success)
    }
    return true
  }
  override fun onStopJob(params: JobParameters): Boolean { running?.cancel(); return params.jobId == WidgetRefreshScheduler.NETWORK_JOB }
  override fun onDestroy() { scope.cancel(); super.onDestroy() }

  companion object {
    suspend fun synchronize(context: Context, api: ApiClient = ApiClient(), clock: Clock = Clock.systemUTC()): Boolean {
      val store = LocalStore(context)
      val widgets = WidgetStore(context)
      val session = store.loadSession() ?: run { WidgetUpdates.redraw(context, clock.instant()); return true }
      if (WidgetUpdates.ids(context).isEmpty()) return true
      val cached = store.loadSnapshot(session.userId)
      val today = clock.instant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate()
      val fetchedDay = cached?.syncedAt?.let { java.time.Instant.ofEpochMilli(it).atZone(ZoneId.of("Asia/Shanghai")).toLocalDate() }
      if (cached != null && fetchedDay == today && clock.millis() - cached.syncedAt in 0 until 3_600_000 &&
        widgets.syncState(session.userId) == WidgetSyncState.CACHED) {
        WidgetUpdates.redraw(context, clock.instant())
        return true
      }
      try {
        val refreshed = AppRepository(store, api, clock).refresh(session)
        if (store.loadSession() != session) return true
        store.saveSnapshot(session.userId, refreshed)
        widgets.recordSyncState(session.userId, WidgetSyncState.CACHED)
        ReminderScheduler(context, store).reschedule(session.userId, refreshed)
        return true
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        if (store.loadSession() != session) return true
        if (error is ApiException && error.code == 401) {
          ReminderScheduler(context, store).cancelAll(session.userId)
          store.clearAccount(session.userId)
          WidgetRefreshScheduler.ensureScheduled(context)
          return true
        }
        widgets.recordSyncState(session.userId, if (error is IOException) WidgetSyncState.OFFLINE else WidgetSyncState.FAILED)
        return false
      } finally {
        WidgetUpdates.redraw(context, clock.instant())
      }
    }
  }
}
