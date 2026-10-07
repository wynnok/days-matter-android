package top.zwtx.daysmatter.widget

import android.content.Context
import java.time.Instant

object WidgetUpdates {
  fun ids(context: Context) = ImportantDayWidgetProvider.instanceIds(context) + UpcomingWidgetProvider.instanceIds(context)
  fun redraw(context: Context, now: Instant = Instant.now()) {
    ImportantDayWidgetProvider.instanceIds(context).forEach { ImportantDayWidgetProvider.update(context, it, now) }
    UpcomingWidgetProvider.instanceIds(context).forEach { UpcomingWidgetProvider.update(context, it, now) }
  }
}
