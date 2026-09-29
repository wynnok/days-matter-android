package io.github.wynnok.daysmatter.data

import io.github.wynnok.daysmatter.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiException(val code: Int, message: String) : Exception(message)

class ApiClient {
  private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

  suspend fun request(
    method: String,
    path: String,
    session: Session? = null,
    body: JSONObject? = null
  ): JSONObject = withContext(Dispatchers.IO) {
    val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 15_000
      readTimeout = 20_000
      setRequestProperty("Accept", "application/json")
      session?.let {
        setRequestProperty("X-User-Id", it.userId.toString())
        setRequestProperty("X-Auth-Token", it.token)
      }
      if (body != null) {
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
      }
    }

    try {
      if (body != null) {
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
      }
      val status = connection.responseCode
      val stream = if (status in 200..299) connection.inputStream else connection.errorStream
      val content = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
      val response = try {
        JSONObject(content)
      } catch (_: Exception) {
        throw ApiException(status, "服务器返回了无法解析的响应")
      }
      val code = response.optInt("code", status)
      if (status !in 200..299 || code != 200) {
        throw ApiException(code, response.optString("message", "请求失败"))
      }
      response
    } finally {
      connection.disconnect()
    }
  }

  suspend fun objectData(method: String, path: String, session: Session? = null, body: JSONObject? = null): JSONObject =
    request(method, path, session, body).optJSONObject("data") ?: JSONObject()

  suspend fun arrayData(path: String, session: Session): JSONArray =
    request("GET", path, session).optJSONArray("data") ?: JSONArray()
}
