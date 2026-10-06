package top.zwtx.daysmatter.data

data class EventOverview(val total: Int, val upcoming: Int, val pending: Int)

/** 汇总已按业务日期派生的主事件，不展开子事件或重复周期。 */
fun List<Event>.overview(): EventOverview = EventOverview(
  total = size,
  upcoming = count { it.isUpcomingWithin(7) },
  pending = count { it.daysDiff == null }
)

fun Event.isUpcomingWithin(dayCount: Int): Boolean = daysDiff?.let { it >= 0 && it < dayCount } == true
