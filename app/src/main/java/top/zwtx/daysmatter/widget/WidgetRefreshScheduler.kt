package top.zwtx.daysmatter.widget

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import top.zwtx.daysmatter.data.LocalStore

object WidgetRefreshScheduler {
  const val PERIODIC_JOB = 2101
  const val NETWORK_JOB = 2102
  private const val HOUR = 3_600_000L
  fun ensureScheduled(context: Context) {
    val jobs = context.getSystemService(JobScheduler::class.java)
    if (WidgetUpdates.ids(context).isEmpty()) {
      jobs.cancel(PERIODIC_JOB); jobs.cancel(NETWORK_JOB)
      return
    }
    if (jobs.getPendingJob(PERIODIC_JOB) == null) jobs.schedule(
      JobInfo.Builder(PERIODIC_JOB, ComponentName(context, WidgetSyncJobService::class.java))
        .setPeriodic(HOUR).setPersisted(true).build())
    if (LocalStore(context).loadSession() == null) jobs.cancel(NETWORK_JOB)
  }
  fun requestNetwork(context: Context) {
    ensureScheduled(context)
    if (WidgetUpdates.ids(context).isEmpty() || LocalStore(context).loadSession() == null) return
    val jobs = context.getSystemService(JobScheduler::class.java)
    if (jobs.getPendingJob(NETWORK_JOB) == null) jobs.schedule(
      JobInfo.Builder(NETWORK_JOB, ComponentName(context, WidgetSyncJobService::class.java))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true)
        .setBackoffCriteria(HOUR, JobInfo.BACKOFF_POLICY_LINEAR).build())
  }
}
