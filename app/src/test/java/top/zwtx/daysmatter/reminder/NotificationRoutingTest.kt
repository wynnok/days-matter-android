package top.zwtx.daysmatter.reminder

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import top.zwtx.daysmatter.TestKeyStore
import top.zwtx.daysmatter.data.LocalStore
import top.zwtx.daysmatter.data.Session

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationRoutingTest {
  @get:Rule val keys = TestKeyStore()
  @Test fun eachActualNotificationOpensItsOwnAccountAndEvent() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    LocalStore(context).saveSession(Session(7, "Test", "test@example.com", "synthetic-token"))
    val notifications = context.getSystemService(NotificationManager::class.java)
    for (id in listOf(11, 12)) ReminderReceiver().onReceive(context,
      Intent().putExtra("event_id", id).putExtra("event_name", "Event $id")
        .putExtra("user_id", 7).putExtra("backend", top.zwtx.daysmatter.BuildConfig.API_BASE_URL))
    val first = shadowOf(notifications).getNotification(11).contentIntent
    val second = shadowOf(notifications).getNotification(12).contentIntent
    assertNotEquals(first, second)
    first.send()
    val opened = shadowOf(context).nextStartedActivity
    assertEquals(11, opened.getIntExtra("event_id", 0))
    assertEquals(7, opened.getIntExtra("user_id", 0))
    assertEquals(top.zwtx.daysmatter.BuildConfig.API_BASE_URL.trimEnd('/'), opened.getStringExtra("backend"))
  }
}
