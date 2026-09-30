package top.zwtx.daysmatter.ui

import top.zwtx.daysmatter.data.Event
import kotlin.math.abs

internal fun List<Event>.sortedByNearestDay(): List<Event> =
  sortedWith(compareBy<Event> { !it.pinned }
    .thenBy { event -> event.daysDiff?.let { abs(it.toLong()) } ?: Long.MAX_VALUE })
