package top.zwtx.daysmatter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class OccurrenceDisplayTest {
  @Test
  fun confirmedDateMovesForwardDespiteStaleServerCount() {
    val original = event("2026-10-08", serverDays = 77)
    val displayed = original.forDisplay(Instant.parse("2026-10-05T16:00:00Z"))

    assertEquals(2, displayed.daysDiff)
    assertEquals(77, original.daysDiff)
  }

  @Test
  fun missingOrInvalidConfirmedDatesNeverUseTheOriginalDateOrServerCount() {
    val now = Instant.parse("2026-10-05T16:00:00Z")
    listOf(null, "", "2026-02-30", "+10000-10-08").forEach { occurrence ->
      assertNull(event(occurrence).forDisplay(now).daysDiff)
    }
  }

  @Test
  fun todayAndPastOccurrencesUseCalendarDays() {
    val now = Instant.parse("2026-10-05T16:00:00Z")
    assertEquals(0, event("2026-10-06").forDisplay(now).daysDiff)
    assertEquals(-2, event("2026-10-04").forDisplay(now).daysDiff)
  }

  @Test
  fun expiredRepeatingOccurrenceWaitsForTheServer() {
    assertNull(event("2026-10-05", repeatType = 4)
      .forDisplay(Instant.parse("2026-10-05T16:00:00Z")).daysDiff)
  }

  @Test
  fun sameEventChangesOnShanghaiMidnight() {
    val original = event("2026-10-08")
    assertEquals(3, original.forDisplay(Instant.parse("2026-10-05T15:59:59Z")).daysDiff)
    assertEquals(2, original.forDisplay(Instant.parse("2026-10-05T16:00:00Z")).daysDiff)
  }

  @Test
  fun lunarEventUsesConfirmedSolarOccurrence() {
    val lunar = event("2026-06-19").copy(dateType = 1, targetDate = "2020-06-25")
    assertEquals(0, lunar.forDisplay(Instant.parse("2026-06-18T16:00:00Z")).daysDiff)
  }

  @Test
  fun subEventsKeepTheirOwnConfirmedOccurrenceWhenParentRepeats() {
    val child = SubEvent(2, 1, "节点", "2020-10-08", 0, 77, "2026-10-08")
    val original = event("2026-10-05", repeatType = 4).copy(subEvents = listOf(child))
    val displayed = original.forDisplay(Instant.parse("2026-10-05T16:00:00Z"))
    assertNull(displayed.daysDiff)
    assertEquals(2, displayed.subEvents.single().daysDiff)
    assertEquals(77, child.daysDiff)
  }

  private fun event(occurrence: String?, serverDays: Int? = 77, repeatType: Int = 0) = Event(
    id = 1, name = "生日", targetDate = "2020-10-08", dateType = 0,
    categoryId = 1, categoryName = "纪念日", categoryColor = "#6366f1", categoryIcon = "heart",
    repeatType = repeatType, repeatValue = 1, webhookEnabled = false, channelId = null,
    remindTitle = null, remindContent = null, advanceDays = 0, remindTime = "09:00:00",
    pinned = false, daysDiff = serverDays, nextOccurrence = occurrence, subEvents = emptyList()
  )
}
