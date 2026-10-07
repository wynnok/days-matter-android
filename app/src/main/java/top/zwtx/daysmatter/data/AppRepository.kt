package top.zwtx.daysmatter.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.time.Clock

class AppRepository(val store: LocalStore, private val api: ApiClient = ApiClient(), private val clock: Clock = Clock.systemUTC()) {
  suspend fun login(email: String, password: String): Session {
    val data = api.objectData(
      "POST", "/auth/login", body = JSONObject().put("email", email).put("password", password)
    )
    return Session.fromJson(data)
  }

  suspend fun register(name: String, email: String, password: String): Session {
    val data = api.objectData(
      "POST", "/auth/register",
      body = JSONObject().put("name", name).put("email", email).put("password", password)
    )
    return Session.fromJson(data)
  }

  suspend fun refresh(session: Session): Snapshot = coroutineScope {
    val profile = async { api.objectData("GET", "/user/info", session) }
    val categories = async { api.arrayData("/categories", session) }
    val channels = async { api.arrayData("/remind-channels", session) }
    val events = async { api.arrayData("/events", session) }
    val raw = JSONObject()
      .put("profile", profile.await())
      .put("categories", categories.await())
      .put("channels", channels.await())
      .put("events", events.await())
      .put("synced_at", clock.millis())
    Snapshot.fromJson(raw)
  }

  suspend fun write(session: Session, method: String, path: String, body: JSONObject? = null): JSONObject =
    api.objectData(method, path, session, body)

  suspend fun exportData(session: Session): JSONObject = api.objectData("GET", "/data/export", session)

  suspend fun importData(session: Session, data: JSONObject) {
    api.request("POST", "/data/import", session, data)
  }
}
