package top.zwtx.daysmatter.data

import android.content.ContentResolver
import android.net.Uri

/** 保存系统文件选择器返回的文档，空输出流也视为失败。 */
class BackupDocuments(private val resolver: ContentResolver) {
  fun save(uri: Uri, text: String) {
    val stream = resolver.openOutputStream(uri, "wt") ?: error("无法写入文件")
    stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
  }
}
