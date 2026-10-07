package top.zwtx.daysmatter.widget

import android.os.Build
import android.graphics.Color
import android.widget.RemoteViews
import top.zwtx.daysmatter.R
import top.zwtx.daysmatter.data.AppearanceMode

/** Resolve each instance independently from the application's device preference. */
fun RemoteViews.applyWidgetAppearance( mode: AppearanceMode, textIds: IntArray) {
  if (mode == AppearanceMode.SYSTEM) {
    if (Build.VERSION.SDK_INT >= 31) {
      setColorInt(R.id.widget_root, "setBackgroundColor", Color.parseColor("#FFF8F4"), Color.parseColor("#24211F"))
      textIds.forEach { setColorInt(it, "setTextColor", Color.parseColor("#292421"), Color.parseColor("#FFF1E9")) }
    }
    // Earlier hosts resolve the XML day/night resources when applying the view.
    return
  }
  val dark = when (mode) {
    AppearanceMode.DARK -> true
    AppearanceMode.LIGHT -> false
    AppearanceMode.SYSTEM -> false
  }
  setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor(if (dark) "#24211F" else "#FFF8F4"))
  val foreground = Color.parseColor(if (dark) "#FFF1E9" else "#292421")
  textIds.forEach { setTextColor(it, foreground) }
}
