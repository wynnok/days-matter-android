package top.zwtx.daysmatter.data

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupDocumentsTest {
  @Test fun savedDocumentContainsTheExactCompatibleBackup() {
    val app = RuntimeEnvironment.getApplication()
    val file = java.io.File(app.cacheDir, "backup.json")
    val text = "{\"version\":\"1.0.0\",\"data\":{\"categories\":[],\"events\":[],\"sub_events\":[],\"remind_channels\":[]}}"
    BackupDocuments(app.contentResolver).save(Uri.fromFile(file), text)
    assertEquals(text, file.readText())
  }
  @Test fun failedDocumentCannotBeReportedAsSaved() {
    val app = RuntimeEnvironment.getApplication()
    val directory = java.io.File(app.cacheDir, "directory").apply { mkdirs() }
    assertThrows(Exception::class.java) { BackupDocuments(app.contentResolver).save(Uri.fromFile(directory), "backup") }
  }
}
