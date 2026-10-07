package top.zwtx.daysmatter

import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActivityEntryLifecycleTest {
  @get:Rule val keys = TestKeyStore()
  @Test fun configurationRecreationDoesNotRedeliverConsumedEntryButWarmIntentDoes() {
    val app = RuntimeEnvironment.getApplication()
    val entry = EventTarget(BuildConfig.API_BASE_URL, 7, 11).intent(app)
    val controller = Robolectric.buildActivity(MainActivity::class.java, entry).setup()
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
    val vm = ViewModelProvider(controller.get())[MainViewModel::class.java]
    assertEquals(1, vm.eventEntryRequest)
    val rotated = android.content.res.Configuration(controller.get().resources.configuration)
    rotated.orientation = android.content.res.Configuration.ORIENTATION_LANDSCAPE
    controller.configurationChange(rotated)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
    val recreated = ViewModelProvider(controller.get())[MainViewModel::class.java]
    assertEquals(if (recreated === vm) 1 else 0, recreated.eventEntryRequest)
    val beforeWarmEntry = recreated.eventEntryRequest
    controller.newIntent(EventTarget(BuildConfig.API_BASE_URL, 7, 12).intent(app))
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
    assertEquals(beforeWarmEntry + 1, recreated.eventEntryRequest)
    controller.pause().stop().destroy()
  }
}
