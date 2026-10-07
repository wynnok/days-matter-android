package top.zwtx.daysmatter

import android.app.Application
import android.net.Uri
import top.zwtx.daysmatter.data.BackupDocuments
import top.zwtx.daysmatter.data.BackupPreview
import top.zwtx.daysmatter.data.ImportOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import top.zwtx.daysmatter.data.ApiException
import top.zwtx.daysmatter.data.ApiClient
import top.zwtx.daysmatter.data.AppearanceMode
import top.zwtx.daysmatter.data.AppRepository
import top.zwtx.daysmatter.data.LocalReminder
import top.zwtx.daysmatter.data.LocalStore
import top.zwtx.daysmatter.data.Profile
import top.zwtx.daysmatter.data.Session
import top.zwtx.daysmatter.data.Snapshot
import top.zwtx.daysmatter.data.withSavedEvent
import top.zwtx.daysmatter.data.withDeletedEvent
import top.zwtx.daysmatter.reminder.ReminderScheduler
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class MainViewModel(application: Application, api: ApiClient, private val clock: Clock = Clock.systemUTC()) : AndroidViewModel(application) {
  constructor(application: Application) : this(application, ApiClient())
  val store = LocalStore(application)
  private val repository = AppRepository(store, api, clock)
  private val scheduler = ReminderScheduler(application, store)

  var session by mutableStateOf(store.loadSession())
    private set
  var snapshot by mutableStateOf(session?.let { store.loadSnapshot(it.userId) })
    private set
  var busy by mutableStateOf(false)
    private set
  var testingChannel by mutableStateOf(false)
    private set
  var savingProfile by mutableStateOf(false)
    private set
  var refreshing by mutableStateOf(false)
    private set
  var offline by mutableStateOf(false)
    private set
  var syncFailed by mutableStateOf(false)
    private set
  var message by mutableStateOf<String?>(null)
    private set
  var gridMode by mutableStateOf(store.gridMode())
    private set
  var appearanceMode by mutableStateOf(store.appearanceMode())
    private set

  private var authenticationGeneration = 0
  private val backend = api.baseUrl
  private var pendingTarget = store.pendingEventTarget()
  var eventNavigation by mutableStateOf<EventTarget?>(null)
    private set

  var eventEntryRequest by mutableStateOf(0)
    private set

  var homeCategoryNavigation by mutableStateOf<Int?>(null)
    private set
  fun consumeHomeNavigation() { homeCategoryNavigation = null }

  var eventCreationNavigation by mutableStateOf(false)
    private set
  fun consumeEventCreation() { eventCreationNavigation = false }

  fun openEvent(intent: android.content.Intent) {
    if (intent.getBooleanExtra("widget_home", false)) {
      clearEventTarget()
      eventNavigation = null
      eventEntryRequest++
      if (session?.userId == intent.getIntExtra("user_id", 0) && intent.getStringExtra("backend") == backend) {
        val category = intent.getIntExtra("category_id", 0)
        if (category != 0 && snapshot?.categories?.none { it.id == category } == true) message = "分类已删除，请重新配置小组件"
        else homeCategoryNavigation = category
        if (intent.getBooleanExtra("create_event", false)) eventCreationNavigation = true
        if (intent.getBooleanExtra("widget_refresh", false)) refresh()
      } else message = "请登录小组件所属账号后查看日程"
      return
    }
    if (intent.getBooleanExtra("create_event", false)) {
      clearEventTarget()
      eventNavigation = null
      if (session?.userId == intent.getIntExtra("user_id", 0) && intent.getStringExtra("backend") == backend) {
        eventEntryRequest++
        eventCreationNavigation = true
      } else message = "请登录事件所属账号后创建事件"
      return
    }

    val target = EventTarget.fromIntent(intent) ?: return
    eventNavigation = null
    eventEntryRequest++
    pendingTarget = target
    store.savePendingEventTarget(target)
    resolveEventTarget()
  }

  fun consumeEventNavigation() { eventNavigation = null }

  private fun clearEventTarget() {
    pendingTarget = null
    store.savePendingEventTarget(null)
  }

  private fun resolveEventTarget(confirmed: Boolean = false, failed: Boolean = false) {
    val target = pendingTarget ?: return
    if (target.backend.trimEnd('/') != backend) {
      clearEventTarget()
      message = "事件属于其他服务，请在对应服务中打开"
      return
    }
    val current = session ?: run { message = "请登录事件所属账号以打开此事件"; return }
    if (target.userId != current.userId) {
      clearEventTarget()
      message = "事件属于其他账号，已返回首页"
      return
    }
    if (snapshot?.events?.any { it.id == target.eventId } == true) {
      eventNavigation = target
      clearEventTarget()
    } else if (confirmed || failed) {
      clearEventTarget()
      message = if (confirmed) "事件已删除或不可用，已返回首页" else "暂时无法获取事件，已返回首页，请联网后重试"
    } else if (!busy) refresh()
  }

  init {
    if (session != null) refresh()
  }

  fun clearMessage() { message = null }

  fun updateGridMode(enabled: Boolean) {
    gridMode = enabled
    store.setGridMode(enabled)
  }

  fun updateAppearanceMode(mode: AppearanceMode) {
    appearanceMode = mode
    store.setAppearanceMode(mode)
  }

  private var networkAvailable: Boolean? = null
  private var pendingNetworkRecovery = false

  fun networkChanged(available: Boolean) {
    val recovering = networkAvailable == false || offline || syncFailed
    networkAvailable = available
    if (!available) { offline = true; updateWidgets() }
    else if (busy && recovering) pendingNetworkRecovery = true
    else refreshIfNeeded()
  }

  fun refreshIfNeeded() {
    val current = session
    if (current != null && store.loadSession() == null) { logout(); return }
    val disk = current?.let { store.loadSnapshot(it.userId) }
    if (disk != null && disk.syncedAt > (snapshot?.syncedAt ?: 0)) {
      snapshot = disk
      offline = networkAvailable == false
      syncFailed = false
    }
    updateWidgets()
    if (session == null || busy || networkAvailable == false) return
    val cached = snapshot
    val today = clock.instant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate()
    val fetchedDate = cached?.syncedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("Asia/Shanghai")).toLocalDate() }
    if (cached == null || offline || syncFailed || fetchedDate != today ||
      clock.millis() - cached.syncedAt >= 60_000 || cached.syncedAt > clock.millis()) refresh()
  }

  private fun updateWidgets() {
    val context = getApplication<Application>()
    session?.let {
      top.zwtx.daysmatter.widget.WidgetStore(context).recordSyncState(it.userId,
        when { offline -> top.zwtx.daysmatter.widget.WidgetSyncState.OFFLINE; syncFailed -> top.zwtx.daysmatter.widget.WidgetSyncState.FAILED; else -> top.zwtx.daysmatter.widget.WidgetSyncState.CACHED })
    }
    top.zwtx.daysmatter.widget.WidgetUpdates.redraw(context, clock.instant())
    top.zwtx.daysmatter.widget.WidgetRefreshScheduler.ensureScheduled(context)
  }

  private fun completeOperation(owner: Session?) {
    if (session !== owner) return
    busy = false
    updateWidgets()
    resolveEventTarget(failed = offline || syncFailed)
    if (pendingNetworkRecovery) {
      pendingNetworkRecovery = false
      refreshIfNeeded()
    }
  }

  private fun requireNetwork(): Boolean {
    if (networkAvailable != false) return true
    message = "当前离线，此操作需要联网；输入已保留，请联网后再提交"
    return false
  }

  private fun report(error: Exception, requestedSession: Session? = null) {
    if (requestedSession != null && session !== requestedSession) return
    if (error is ApiException && error.code == 401 && requestedSession != null) {
      logout()
      message = "登录已失效，请重新登录"
    } else {
      message = error.message ?: "操作失败，请稍后再试"
    }
  }

  private suspend fun load(session: Session) {
    if (this.session !== session) return
    val refreshed = repository.refresh(session)
    if (this.session !== session) return
    store.saveSnapshot(session.userId, refreshed)
    snapshot = refreshed
    scheduler.reschedule(session.userId, refreshed)
    offline = networkAvailable == false
    syncFailed = false
    updateWidgets()
    resolveEventTarget(confirmed = true)
  }

  private fun reportAfterWrite(error: Exception, current: Session, successMessage: String) {
    if (session !== current) return
    if (error is ApiException && error.code == 401) {
      logout()
      message = "$successMessage，登录已失效，请重新登录"
    } else {
      offline = error is IOException
      syncFailed = true
      message = "$successMessage，但同步失败，请稍后刷新"
    }
  }

  fun refresh() {
    val current = session ?: return
    if (busy) return
    viewModelScope.launch {
      busy = true
      refreshing = true
      try {
        load(current)
      } catch (error: Exception) {
        if (session === current) {
          offline = error is IOException
          syncFailed = error !is ApiException || error.code != 401
          report(error, current)
        }
      } finally {
        if (session === current) {
          refreshing = false
          completeOperation(current)
        }
      }
    }
  }

  fun login(email: String, password: String) {
    if (busy) return
    val generation = ++authenticationGeneration
    viewModelScope.launch {
      var authenticated: Session? = null
      busy = true
      try {
        val result = repository.login(email.trim(), password)
        if (generation != authenticationGeneration) return@launch
        store.saveSession(result)
        session = result
        authenticated = session
        lastExport = session?.let { store.lastExport(it.userId) } ?: 0L
        snapshot = session?.let { store.loadSnapshot(it.userId) }
        updateWidgets()
        offline = false
        syncFailed = false
        load(session!!)
      } catch (error: Exception) {
        if (generation != authenticationGeneration) return@launch
        val current = session
        offline = current != null && error is IOException
        syncFailed = current != null && (error !is ApiException || error.code != 401)
        report(error, current)
      } finally {
        if (generation == authenticationGeneration && session === authenticated) completeOperation(authenticated)
      }
    }
  }

  fun register(name: String, email: String, password: String) {
    if (busy) return
    val generation = ++authenticationGeneration
    viewModelScope.launch {
      var authenticated: Session? = null
      busy = true
      try {
        val result = repository.register(name.trim(), email.trim(), password)
        if (generation != authenticationGeneration) return@launch
        store.saveSession(result)
        session = result
        authenticated = session
        lastExport = session?.let { store.lastExport(it.userId) } ?: 0L
        snapshot = null
        updateWidgets()
        offline = false
        syncFailed = false
        load(session!!)
      } catch (error: Exception) {
        if (generation != authenticationGeneration) return@launch
        val current = session
        offline = current != null && error is IOException
        syncFailed = current != null && (error !is ApiException || error.code != 401)
        report(error, current)
      } finally {
        if (generation == authenticationGeneration && session === authenticated) completeOperation(authenticated)
      }
    }
  }

  fun logout() {
    authenticationGeneration++
    session?.let {
      scheduler.cancelAll(it.userId)
      store.clearAccount(it.userId)
    }
    eventNavigation = null
    eventCreationNavigation = false
    importPreview = null
    importOwner = null
    importOutcome = null
    showingImportOutcome = false
    session = null
    lastExport = 0L
    snapshot = null
    offline = false
    syncFailed = false
    busy = false
    testingChannel = false
    savingProfile = false
    refreshing = false
    pendingNetworkRecovery = false
    updateWidgets()
  }

  fun localReminder(eventId: Int): LocalReminder =
    session?.let { store.localReminder(it.userId, eventId) } ?: LocalReminder()

  fun saveProfile(nickname: String, email: String, onSaved: () -> Unit) {
    val current = session ?: return
    if (busy) return
    if (!requireNetwork()) return
    viewModelScope.launch {
      busy = true
      savingProfile = true
      var saved = false
      try {
        val data = repository.write(current, "PUT", "/user/info",
          JSONObject().put("nickname", nickname.trim()).put("email", email.trim()))
        if (session !== current) return@launch
        saved = true
        snapshot = snapshot?.let {
          it.copy(profile = Profile.fromJson(data), raw = JSONObject(it.raw.toString()).put("profile", data))
        }
        message = "资料保存成功"
        onSaved()
        snapshot?.let { store.saveSnapshot(current.userId, it) }
        load(current)
      } catch (error: Exception) {
        if (saved) reportAfterWrite(error, current, "资料已保存") else report(error, current)
      } finally {
        if (session === current) {
          savingProfile = false
          completeOperation(current)
        }
      }
    }
  }

  fun saveEvent(eventId: Int?, body: JSONObject, reminder: LocalReminder, onDone: (Int) -> Unit) {
    val current = session ?: return
    if (busy) return
    if (!requireNetwork()) return
    viewModelScope.launch {
      busy = true
      var saved = false
      try {
        val result = repository.write(
          current, if (eventId == null) "POST" else "PUT",
          if (eventId == null) "/events" else "/events/$eventId", body
        )
        if (session !== current) return@launch
        saved = true
        val id = eventId ?: result.getInt("event_id")
        message = "保存成功"
        onDone(id)
        store.saveLocalReminder(current.userId, id, reminder)
        snapshot = snapshot?.withSavedEvent(id, body, result)
        snapshot?.let { store.saveSnapshot(current.userId, it); scheduler.reschedule(current.userId, it) }
        updateWidgets()
        load(current)
      } catch (error: Exception) {
        if (saved) reportAfterWrite(error, current, "保存成功") else {
          report(error, current)
          if (session === current && error is IOException) {
            offline = true
            message = "网络连接失败，保存需要联网；输入已保留，请联网后核实数据"
          }
        }
      } finally {
        if (session === current) completeOperation(current)
      }
    }
  }

  fun setChannelActive(channelId: Int, active: Boolean) {
    val current = session ?: return
    if (snapshot?.channels?.none { it.id == channelId } != false) return
    writeAndRefresh("PUT", "/remind-channels/$channelId", JSONObject().put("is_active", if (active) 1 else 0),
      if (active) "渠道已启用" else "渠道已停用") {
      snapshot = snapshot?.let { existing ->
        val raw = JSONObject(existing.raw.toString())
        raw.optJSONArray("channels")?.let { channels ->
          for (index in 0 until channels.length()) {
            val channel = channels.getJSONObject(index)
            if (channel.getInt("channel_id") == channelId) channel.put("is_active", if (active) 1 else 0)
          }
        }
        existing.copy(channels = existing.channels.map {
          if (it.id == channelId) it.copy(active = active) else it
        }, raw = raw)
      }
      snapshot?.let { store.saveSnapshot(current.userId, it) }
    }
  }

  fun write(method: String, path: String, body: JSONObject? = null, onWritten: (JSONObject) -> Unit = {}) {
    writeAndRefresh(method, path, body, if (method == "DELETE") "删除成功" else "保存成功", onWritten)
  }

  private fun writeAndRefresh(
    method: String, path: String, body: JSONObject?, successMessage: String, onWritten: (JSONObject) -> Unit
  ) {
    val current = session ?: return
    if (busy) return
    if (!requireNetwork()) return
    viewModelScope.launch {
      busy = true
      var written = false
      try {
        val result = repository.write(current, method, path, body)
        if (session !== current) return@launch
        written = true
        message = successMessage
        onWritten(result)
        if (method == "DELETE" && path.startsWith("/events/")) {
          path.substringAfterLast('/').toIntOrNull()?.let { id ->
            scheduler.cancel(current.userId, id)
            store.removeLocalReminder(current.userId, id)
            snapshot = snapshot?.withDeletedEvent(id)
            snapshot?.let { store.saveSnapshot(current.userId, it) }
            updateWidgets()
          }
        }
        load(current)
      } catch (error: Exception) {
        if (written) reportAfterWrite(error, current, successMessage) else {
          report(error, current)
          if (session === current && error is IOException) {
            offline = true
            message = "网络连接失败，操作需要联网；输入已保留，请联网后核实数据"
          }
        }
      } finally {
        if (session === current) completeOperation(current)
      }
    }
  }

  fun testChannel(body: JSONObject) {
    val current = session ?: return
    if (busy) return
    if (!requireNetwork()) return
    viewModelScope.launch {
      busy = true
      testingChannel = true
      try {
        repository.write(current, "POST", "/remind-channels/test", body)
        if (session === current) message = "测试请求已完成，请检查接收端"
      } catch (error: Exception) {
        report(error, current)
      } finally {
        if (session === current) {
          testingChannel = false
          completeOperation(current)
        }
      }
    }
  }

  var lastExport by mutableStateOf(session?.let { store.lastExport(it.userId) } ?: 0L)
    private set

  fun saveExport(uri: Uri, text: String, owner: Session) {
    if (session !== owner) return
    viewModelScope.launch {
      try {
        withContext(Dispatchers.IO) { BackupDocuments(getApplication<Application>().contentResolver).save(uri, text) }
        if (session === owner) {
          lastExport = System.currentTimeMillis()
          store.recordExport(owner.userId, lastExport)
          message = "备份已保存"
        }
      } catch (_: Exception) {
        if (session === owner) message = "保存文件失败，请重新选择位置"
      }
    }
  }

  fun exportData(onReady: (String) -> Unit) {
    val current = session ?: return
    if (busy) return
    if (!requireNetwork()) return
    viewModelScope.launch {
      busy = true
      try {
        val data = repository.exportData(current)
        if (session === current) onReady(data.toString(2))
      } catch (error: Exception) {
        report(error, current)
      } finally {
        if (session === current) completeOperation(current)
      }
    }
  }

  var importPreview by mutableStateOf<BackupPreview?>(null)
    private set
  private var importOwner: Session? = null
  var importOutcome by mutableStateOf<ImportOutcome?>(null)
    private set
  var showingImportOutcome by mutableStateOf(false)
    private set

  fun viewImportedData() { if (!busy) showingImportOutcome = false }

  fun prepareImport(text: String, owner: Session? = session) {
    if (owner == null || session !== owner || busy) return
    if (importOutcome == ImportOutcome.UNKNOWN) {
      showingImportOutcome = true
      message = "导入结果未知，可能已追加，请先刷新并核实数据，避免重复导入"
      return
    }
    importOutcome = null
    try {
      importPreview = BackupPreview.parse(text)
      importOwner = owner
    } catch (_: Exception) {
      message = "备份格式或版本不兼容，请选择有效的账号备份"
    }
  }

  fun cancelImport() {
    if (busy) return
    importPreview = null
    importOwner = null
  }

  fun acknowledgeImportOutcome() {
    if (!busy) { importOutcome = null; showingImportOutcome = false }
  }

  fun refreshImportData() {
    val current = session ?: return
    if (busy) return
    val unknown = importOutcome == ImportOutcome.UNKNOWN
    viewModelScope.launch {
      busy = true
      refreshing = true
      try {
        load(current)
        if (session === current) {
          importOutcome = if (unknown) ImportOutcome.UNKNOWN else ImportOutcome.COMPLETE
          message = if (unknown) "账号数据已刷新，请核对是否已追加，避免重复导入" else "导入请求已完成，账号数据已刷新"
        }
      } catch (error: Exception) {
        if (session === current) {
          offline = error is IOException
          syncFailed = true
          report(error, current)
          if (session === current) message = if (unknown) "导入结果未知，刷新失败，请稍后核实账号数据" else "导入请求已完成，但刷新失败，请仅刷新账号数据"
        }
      } finally {
        if (session === current) { refreshing = false; completeOperation(current) }
      }
    }
  }

  fun confirmImport() {
    val current = importOwner ?: return
    val preview = importPreview ?: return
    if (session !== current || busy) return
    if (!requireNetwork()) return
    viewModelScope.launch {
      busy = true
      var written = false
      try {
        repository.importData(current, preview.data)
        written = true
        if (session !== current) return@launch
        importPreview = null
        importOwner = null
        load(current)
        if (session === current) {
          importOutcome = ImportOutcome.COMPLETE
          message = "导入请求已完成，账号数据已刷新"
        }
      } catch (error: Exception) {
        if (session !== current) return@launch
        if (written) {
          reportAfterWrite(error, current, "导入请求已完成")
          if (session === current) {
            showingImportOutcome = true
            importOutcome = ImportOutcome.REFRESH_FAILED
            message = "导入请求已完成，但刷新失败，请仅刷新账号数据"
          }
        } else if (error is ApiException && error.code in 400..499 && !error.outcomeUnknown) {
          report(error, current)
          if (session === current) message = "导入请求被拒绝，请检查备份或重新登录"
        } else {
          importPreview = null
          importOwner = null
          showingImportOutcome = true
          importOutcome = ImportOutcome.UNKNOWN
          message = "导入结果未知，可能已追加，请先刷新并核实数据，避免重复导入"
        }
      } finally {
        if (session === current) completeOperation(current)
      }
    }
  }

  fun showMessage(text: String) { message = text }
}
