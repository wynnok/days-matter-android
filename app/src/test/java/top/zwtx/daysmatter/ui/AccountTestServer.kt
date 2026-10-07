package top.zwtx.daysmatter.ui

import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** A controlled external HTTP service shared by navigation and desktop integration tests. */
class AccountTestServer : AutoCloseable {
  var userId = 7
  var captureEventsBeforePause = false
  var applyEventWrites = false
  var events = JSONArray().put(JSONObject().put("event_id", 11).put("event_name", "目标事件")
    .put("server_next_occurrence", "2026-10-20"))
  var emptyEvents = false
  var failEvents = false
  var unauthorizedEvents = false
  var failRefreshAfterWrite = false
  var eventsPaused = CountDownLatch(0)
  var exportPaused = CountDownLatch(0)
  val eventsStarted = CountDownLatch(1)
  val eventRequests = AtomicInteger()
  val exportStarted = CountDownLatch(1)
  var loginPaused = CountDownLatch(0)
  val loginStarted = CountDownLatch(1)
  val loginResponses = AtomicInteger()
  private val executor = Executors.newCachedThreadPool()
  private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
    this.executor = this@AccountTestServer.executor
    createContext("/") { exchange ->
      val path = exchange.requestURI.path
      val requestedUser = userId
      val capturedEvents = if (path == "/events" && captureEventsBeforePause) JSONArray(events.toString()) else null
      if (path.startsWith("/events/") && exchange.requestMethod == "PUT" && applyEventWrites) {
        val body = JSONObject(exchange.requestBody.bufferedReader().use { it.readText() })
        events.getJSONObject(0).put("event_name", body.getString("event_name"))
      }
      when (path) {
        "/events" -> { eventRequests.incrementAndGet(); eventsStarted.countDown(); eventsPaused.await(5, TimeUnit.SECONDS) }
        "/data/export" -> { exportStarted.countDown(); exportPaused.await(5, TimeUnit.SECONDS) }
        "/auth/login" -> { loginStarted.countDown(); loginPaused.await(5, TimeUnit.SECONDS) }
      }
      if (path.startsWith("/events/") && exchange.requestMethod != "GET" && failRefreshAfterWrite) failEvents = true
      val code = when {
        path == "/events" && unauthorizedEvents -> 401
        path == "/events" && failEvents -> 503
        else -> 200
      }
      val data: Any = when (path) {
        "/auth/login" -> JSONObject().put("user_id", requestedUser).put("name", "账号$requestedUser")
          .put("email", "test@example.com").put("token", "synthetic-token-$requestedUser")
        "/user/info" -> JSONObject().put("nickname", "账号$requestedUser").put("email", "test@example.com")
        "/events" -> if (emptyEvents) JSONArray() else capturedEvents ?: events
        "/data/export" -> JSONObject().put("version", "1.0.0")
        else -> if (exchange.requestMethod == "GET") JSONArray() else JSONObject().put("event_id", 11)
      }
      val bytes = JSONObject().put("code", code).put("message", "测试同步失败").put("data", data).toString().toByteArray()
      exchange.sendResponseHeaders(code, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
      if (path == "/auth/login") loginResponses.incrementAndGet()
    }
    start()
  }
  val baseUrl = "http://127.0.0.1:${server.address.port}"
  override fun close() {
    eventsPaused.countDown(); exportPaused.countDown(); loginPaused.countDown()
    server.stop(0); executor.shutdownNow()
  }
}
