package top.zwtx.daysmatter.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import top.zwtx.daysmatter.data.Event

class EventOrderingTest {
  @Test
  fun pastAndFutureEventsAreOrderedByAbsoluteDays() {
    val events = listOf(event(1, -876), event(2, 65), event(3, -2), event(4, 0), event(5, 7))

    assertEquals(listOf(4, 3, 5, 2, 1), events.sortedByNearestDay().map { it.id })
  }

  @Test
  fun equalDistancesPreserveTheirOriginalOrder() {
    val events = listOf(event(1, 7), event(2, -7), event(3, 7))

    assertEquals(listOf(1, 2, 3), events.sortedByNearestDay().map { it.id })
  }

  @Test
  fun pinnedEventsComeFirstAndEachGroupIsOrderedByAbsoluteDays() {
    val events = listOf(
      event(1, 65, pinned = true), event(2, -2), event(3, -7, pinned = true),
      event(4, 0), event(5, 9, pinned = true), event(6, 30)
    )

    assertEquals(listOf(3, 5, 1, 4, 2, 6), events.sortedByNearestDay().map { it.id })
  }

  @Test
  fun pinnedEventsWithUnknownDaysStayAheadOfOrdinaryEvents() {
    val events = listOf(event(1, 0), event(2, null, pinned = true), event(3, 2, pinned = true))

    assertEquals(listOf(3, 2, 1), events.sortedByNearestDay().map { it.id })
  }

  @Test
  fun unknownDaysGoLastAndLargeNegativeValuesDoNotOverflow() {
    val events = listOf(event(1, null), event(2, Int.MIN_VALUE), event(3, Int.MAX_VALUE), event(4, 0))

    assertEquals(listOf(4, 3, 2, 1), events.sortedByNearestDay().map { it.id })
  }

  private fun event(id: Int, days: Int?, pinned: Boolean = false) = Event(
    id = id, name = "事件 $id", targetDate = "2026-09-30", dateType = 0,
    categoryId = 1, categoryName = "纪念日", categoryColor = "#6366f1", categoryIcon = "folder",
    repeatType = 0, repeatValue = 1, webhookEnabled = false, channelId = null,
    remindTitle = null, remindContent = null, advanceDays = 0, remindTime = "09:00:00",
    pinned = pinned, daysDiff = days, nextOccurrence = null, subEvents = emptyList()
  )
}
