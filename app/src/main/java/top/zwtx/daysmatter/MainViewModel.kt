package top.zwtx.daysmatter

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import top.zwtx.daysmatter.data.ApiException
import top.zwtx.daysmatter.data.AppearanceMode
import top.zwtx.daysmatter.data.AppRepository
import top.zwtx.daysmatter.data.LocalReminder
import top.zwtx.daysmatter.data.LocalStore
import top.zwtx.daysmatter.data.Session
import top.zwtx.daysmatter.data.Snapshot
import top.zwtx.daysmatter.reminder.ReminderScheduler
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

class MainViewModel(application: Application) : AndroidViewModel(application) {
  val store = LocalStore(application)
  private val repository = AppRepository(store)
  private val scheduler = ReminderScheduler(application, store)

  var session by mutableStateOf(store.loadSession())
    private set
  var snapshot by mutableStateOf(session?.let { store.loadSnapshot(it.userId) })
    private set
  var busy by mutableStateOf(false)
    private set
  var testingChannel by mutableStateOf(false)
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
    snapshot = refreshed
    scheduler.reschedule(session.userId, refreshed)
    offline = false
    syncFailed = false
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
          busy = false
        }
      }
    }
  }

  fun login(email: String, password: String) {
    if (busy) return
    viewModelScope.launch {
      var authenticated: Session? = null
      busy = true
      try {
        session = repository.login(email.trim(), password)
        authenticated = session
        snapshot = session?.let { store.loadSnapshot(it.userId) }
        offline = false
        syncFailed = false
        load(session!!)
      } catch (error: Exception) {
        val current = session
        offline = current != null && error is IOException
        syncFailed = current != null && (error !is ApiException || error.code != 401)
        report(error, current)
      } finally {
        if (session === authenticated) busy = false
      }
    }
  }

  fun register(name: String, email: String, password: String) {
    if (busy) return
    viewModelScope.launch {
      var authenticated: Session? = null
      busy = true
      try {
        session = repository.register(name.trim(), email.trim(), password)
        authenticated = session
        snapshot = null
        offline = false
        syncFailed = false
        load(session!!)
      } catch (error: Exception) {
        val current = session
        offline = current != null && error is IOException
        syncFailed = current != null && (error !is ApiException || error.code != 401)
        report(error, current)
      } finally {
        if (session === authenticated) busy = false
      }
    }
  }

  fun logout() {
    session?.let {
      scheduler.cancelAll(it.userId)
      store.clearAccount(it.userId)
    }
    session = null
    snapshot = null
    offline = false
    syncFailed = false
    busy = false
    testingChannel = false
    refreshing = false
  }

  fun localReminder(eventId: Int): LocalReminder =
    session?.let { store.localReminder(it.userId, eventId) } ?: LocalReminder()

  fun saveEvent(eventId: Int?, body: JSONObject, reminder: LocalReminder, onDone: (Int) -> Unit) {
    val current = session ?: return
    viewModelScope.launch {
      busy = true
      try {
        val result = repository.write(
          current, if (eventId == null) "POST" else "PUT",
          if (eventId == null) "/events" else "/events/$eventId", body
        )
        if (session !== current) return@launch
        val id = eventId ?: result.getInt("event_id")
        store.saveLocalReminder(current.userId, id, reminder)
        load(current)
        if (session === current) onDone(id)
      } catch (error: Exception) {
        report(error, current)
      } finally {
        if (session === current) busy = false
      }
    }
  }

  fun write(method: String, path: String, body: JSONObject? = null, onDone: () -> Unit = {}) {
    val current = session ?: return
    viewModelScope.launch {
      busy = true
      try {
        repository.write(current, method, path, body)
        if (session !== current) return@launch
        if (method == "DELETE" && path.startsWith("/events/")) {
          path.substringAfterLast('/').toIntOrNull()?.let { id ->
            scheduler.cancel(current.userId, id)
            store.removeLocalReminder(current.userId, id)
          }
        }
        load(current)
        if (session === current) onDone()
      } catch (error: Exception) {
        report(error, current)
      } finally {
        if (session === current) busy = false
      }
    }
  }

  fun testChannel(body: JSONObject) {
    val current = session ?: return
    if (busy) return
    viewModelScope.launch {
      busy = true
      testingChannel = true
      try {
        repository.write(current, "POST", "/remind-channels/test", body)
        if (session === current) message = "测试消息已发送，请检查接收端"
      } catch (error: Exception) {
        report(error, current)
      } finally {
        if (session === current) {
          testingChannel = false
          busy = false
        }
      }
    }
  }

  fun exportData(onReady: (String) -> Unit) {
    val current = session ?: return
    viewModelScope.launch {
      busy = true
      try {
        val data = repository.exportData(current)
        if (session === current) onReady(data.toString(2))
      } catch (error: Exception) {
        report(error, current)
      } finally {
        if (session === current) busy = false
      }
    }
  }

  fun importData(text: String, onDone: () -> Unit = {}) {
    val current = session ?: return
    viewModelScope.launch {
      busy = true
      try {
        repository.importData(current, JSONObject(text))
        load(current)
        if (session === current) onDone()
      } catch (error: Exception) {
        report(error, current)
      } finally {
        if (session === current) busy = false
      }
    }
  }

  fun showMessage(text: String) { message = text }
}
