package top.zwtx.daysmatter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EventOverviewTest {
  @Test
  fun accountOverviewCountsOnlyMainEventsInTheSevenDayWindow() {
    val now = Instant.parse("2026-10-05T16:00:00Z")
    val dates = listOf("2026-10-06", "2026-10-12", "2026-10-13", "2026-11-04",
      "2026-11-05", "2026-10-05", null, "2026-10-05")
    val events = dates.mapIndexed { index, date ->
      event(index + 1, date, if (index == 7) 4 else 0).forDisplay(now)
    }
    assertEquals(EventOverview(8, 2, 2), events.overview())
  }

  @Test
  fun thirtyDayWindowIncludesTodayThroughDayTwentyNine() {
    val now = Instant.parse("2026-10-05T16:00:00Z")
    assertTrue(event(1, "2026-10-06", 0).forDisplay(now).isUpcomingWithin(30))
    assertTrue(event(2, "2026-11-04", 0).forDisplay(now).isUpcomingWithin(30))
    assertFalse(event(3, "2026-11-05", 0).forDisplay(now).isUpcomingWithin(30))
    assertFalse(event(4, "2026-10-05", 0).forDisplay(now).isUpcomingWithin(30))
    assertFalse(event(5, null, 0).forDisplay(now).isUpcomingWithin(30))
  }

  @Test
  fun confirmedEmptyAccountHasZeroRatherThanUnknownCounts() {
    assertEquals(EventOverview(0, 0, 0), emptyList<Event>().overview())
  }

  private fun event(id: Int, date: String?, repeatType: Int) = Event(
    id = id, name = "事件 $id", targetDate = "2020-10-06", dateType = 0,
    categoryId = if (id % 2 == 0) 2 else 1, categoryName = "分类", categoryColor = "#6366f1",
    categoryIcon = "heart", repeatType = repeatType, repeatValue = 1, webhookEnabled = false,
    channelId = null, remindTitle = null, remindContent = null, advanceDays = 0, remindTime = "09:00:00",
    pinned = false, daysDiff = 77, nextOccurrence = date,
    subEvents = listOf(SubEvent(100 + id, id, "子事件", "2026-10-06", 0, 0, "2026-10-06"))
  )
}
