package top.zwtx.daysmatter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupPreviewTest {
  @Test fun previewCountsFileObjectsWithoutClaimingImportedCounts() {
    val preview = BackupPreview.parse("""{"version":"1.0.0","data":{"categories":[{"category_id":1}],"events":[{"event_id":2}],"sub_events":[{"sub_event_id":3},{"sub_event_id":4}],"remind_channels":[{"auth_token":"secret"}]}}""")
    assertEquals("分类 1 · 渠道 1 · 主事件 1 · 子事件 2", preview.summary)
    assertEquals(1, preview.data.getJSONObject("data").getJSONArray("remind_channels").length())
  }
  @Test fun legacyWithoutChannelsIsAcceptedAndBadVersionOrShapeIsRejected() {
    assertEquals("分类 0 · 渠道 0 · 主事件 0 · 子事件 0",
      BackupPreview.parse("""{"data":{"categories":[],"events":[],"sub_events":[]}}""").summary)
    for (text in listOf("[]", "{}", """{"version":"2.0.0","data":{"categories":[],"events":[]}}""",
      """{"data":{"categories":{},"events":[]}}""", """{"data":{"categories":[null],"events":[]}}""")) {
      assertThrows(Exception::class.java) { BackupPreview.parse(text) }
    }
  }
}
