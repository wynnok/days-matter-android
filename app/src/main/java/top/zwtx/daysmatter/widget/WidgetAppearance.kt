package top.zwtx.daysmatter.widget

import android.os.Build
import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import top.zwtx.daysmatter.R
import top.zwtx.daysmatter.data.AppearanceMode

/** Resolve each instance independently from the application's device preference. */
fun RemoteViews.applyWidgetAppearance(context: Context, mode: AppearanceMode, textIds: IntArray,
  secondaryIds: IntArray = intArrayOf(), accentIds: IntArray = intArrayOf()) {
  if (mode == AppearanceMode.SYSTEM) {
    setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background)
    if (Build.VERSION.SDK_INT >= 31) {
      textIds.forEach { setColorInt(it, "setTextColor", Color.parseColor("#182234"), Color.parseColor("#F3F5FA")) }
      secondaryIds.forEach { setColorInt(it, "setTextColor", Color.parseColor("#7C8798"), Color.parseColor("#939EB0")) }
      accentIds.forEach { setColorInt(it, "setTextColor", Color.parseColor("#3478F6"), Color.parseColor("#8AB4FF")) }
    } else {
      // Reapply must also clear colors left by a previously explicit appearance.
      textIds.forEach { setTextColor(it, context.getColor(R.color.widget_foreground)) }
      secondaryIds.forEach { setTextColor(it, context.getColor(R.color.widget_secondary)) }
      accentIds.forEach { setTextColor(it, context.getColor(R.color.widget_accent)) }
    }
    // Keep the drawable so the rounded corners survive in launchers and previews.
    // Hosts resolve its day/night resources when applying the view.
    return
  }
  val dark = when (mode) {
    AppearanceMode.DARK -> true
    AppearanceMode.LIGHT -> false
    AppearanceMode.SYSTEM -> false
  }
  setInt(R.id.widget_root, "setBackgroundResource", if (dark) R.drawable.widget_background_dark else R.drawable.widget_background_light)
  val foreground = Color.parseColor(if (dark) "#F3F5FA" else "#182234")
  textIds.forEach { setTextColor(it, foreground) }
  secondaryIds.forEach { setTextColor(it, Color.parseColor(if (dark) "#939EB0" else "#7C8798")) }
  accentIds.forEach { setTextColor(it, Color.parseColor(if (dark) "#8AB4FF" else "#3478F6")) }
}
