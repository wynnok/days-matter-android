package io.github.wynnok.daysmatter

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.wynnok.daysmatter.data.ApiException
import io.github.wynnok.daysmatter.data.AppRepository
import io.github.wynnok.daysmatter.data.LocalReminder
import io.github.wynnok.daysmatter.data.LocalStore
import io.github.wynnok.daysmatter.data.Session
import io.github.wynnok.daysmatter.data.Snapshot
import io.github.wynnok.daysmatter.reminder.ReminderScheduler
import kotlinx.coroutines.launch
import org.json.JSONObject

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
  var offline by mutableStateOf(false)
    private set
  var message by mutableStateOf<String?>(null)
    private set
  var darkMode by mutableStateOf(store.darkMode())
    private set
  var gridMode by mutableStateOf(store.gridMode())
    private set

  init {
    if (session != null) refresh()
  }

  fun clearMessage() { message = null }

  fun setDarkMode(enabled: Boolean) {
    darkMode = enabled
    store.setDarkMode(enabled)
  }

  fun setGridMode(enabled: Boolean) {
    gridMode = enabled
    store.setGridMode(enabled)
  }

  private fun report(error: Exception) {
    if (error is ApiException && error.code == 401 && session != null) {
      logout()
      message = "登录已失效，请重新登录"
    } else {
      message = error.message ?: "操作失败，请稍后再试"
    }
  }

  private suspend fun load(session: Session) {
    snapshot = repository.refresh(session)
    snapshot?.let { scheduler.reschedule(session.userId, it) }
    offline = false
  }

  fun refresh() {
    val current = session ?: return
    viewModelScope.launch {
      busy = true
      try {
        load(current)
      } catch (error: Exception) {
        offline = true
        report(error)
      } finally {
        busy = false
      }
    }
  }

  fun login(email: String, password: String) {
    viewModelScope.launch {
      busy = true
      try {
        session = repository.login(email.trim(), password)
        snapshot = null
        load(session!!)
      } catch (error: Exception) {
        report(error)
      } finally {
        busy = false
      }
    }
  }

  fun register(name: String, email: String, password: String) {
    viewModelScope.launch {
      busy = true
      try {
        session = repository.register(name.trim(), email.trim(), password)
        snapshot = null
        load(session!!)
      } catch (error: Exception) {
        report(error)
      } finally {
        busy = false
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
        val id = eventId ?: result.getInt("event_id")
        store.saveLocalReminder(current.userId, id, reminder)
        load(current)
        onDone(id)
      } catch (error: Exception) {
        report(error)
      } finally {
        busy = false
      }
    }
  }

  fun write(method: String, path: String, body: JSONObject? = null, onDone: () -> Unit = {}) {
    val current = session ?: return
    viewModelScope.launch {
      busy = true
      try {
        repository.write(current, method, path, body)
        if (method == "DELETE" && path.startsWith("/events/")) {
          path.substringAfterLast('/').toIntOrNull()?.let { id ->
            scheduler.cancel(current.userId, id)
            store.removeLocalReminder(current.userId, id)
          }
        }
        load(current)
        onDone()
      } catch (error: Exception) {
        report(error)
      } finally {
        busy = false
      }
    }
  }

  fun exportData(onReady: (String) -> Unit) {
    val current = session ?: return
    viewModelScope.launch {
      busy = true
      try {
        onReady(repository.exportData(current).toString(2))
      } catch (error: Exception) {
        report(error)
      } finally {
        busy = false
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
        onDone()
      } catch (error: Exception) {
        report(error)
      } finally {
        busy = false
      }
    }
  }

  fun showMessage(text: String) { message = text }
}
