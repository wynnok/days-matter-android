package top.zwtx.daysmatter.data

import top.zwtx.daysmatter.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiException(val code: Int, message: String, val outcomeUnknown: Boolean = false) : Exception(message)

class ApiClient(baseUrl: String = BuildConfig.API_BASE_URL) {
  private val baseUrl = baseUrl.trimEnd('/')

  suspend fun request(
    method: String,
    path: String,
    session: Session? = null,
    body: JSONObject? = null
  ): JSONObject = withContext(Dispatchers.IO) {
    val bodyBytes = body?.toString()?.toByteArray(Charsets.UTF_8)
    val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 15_000
      readTimeout = 20_000
      setRequestProperty("Accept", "application/json")
      session?.let {
        setRequestProperty("X-User-Id", it.userId.toString())
        setRequestProperty("X-Auth-Token", it.token)
      }
      if (bodyBytes != null) {
        setFixedLengthStreamingMode(bodyBytes.size)
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
      }
    }

    try {
      if (bodyBytes != null) {
        connection.outputStream.use { it.write(bodyBytes) }
      }
      val status = connection.responseCode
      val stream = if (status in 200..299) connection.inputStream else connection.errorStream
      val content = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
      val response = try {
        JSONObject(content)
      } catch (_: Exception) {
        throw ApiException(status, "服务器返回了无法解析的响应", outcomeUnknown = true)
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
    request(method, path, session, body).optJSONObject("data")
      ?: if (method == "GET") throw ApiException(502, "服务器返回了缺失的数据") else JSONObject()

  suspend fun arrayData(path: String, session: Session): JSONArray =
    request("GET", path, session).optJSONArray("data") ?: throw ApiException(502, "服务器返回了缺失的列表数据")
}
