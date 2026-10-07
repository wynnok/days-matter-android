package top.zwtx.daysmatter.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImportantDayWidgetTest {
  @Test fun systemCanDiscoverImportantDayWidgetAndItsRequiredConfiguration() {
    val app = RuntimeEnvironment.getApplication()
    val receivers = app.packageManager.queryBroadcastReceivers(
      Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setPackage(app.packageName), PackageManager.GET_META_DATA)
    val provider = receivers.singleOrNull { it.activityInfo.name.endsWith("ImportantDayWidgetProvider") }
    assertNotNull("系统小组件选择器需要可发现的 provider", provider)
    val resource = provider!!.activityInfo.metaData.getInt(AppWidgetManager.META_DATA_APPWIDGET_PROVIDER)
    val xml = app.resources.getXml(resource)
    while (xml.eventType != org.xmlpull.v1.XmlPullParser.START_TAG) xml.next()
    assertEquals("appwidget-provider", xml.name)
    assertEquals("top.zwtx.daysmatter.widget.WidgetConfigurationActivity",
      xml.getAttributeValue("http://schemas.android.com/apk/res/android", "configure"))
    assertEquals(android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE,
      xml.getAttributeIntValue("http://schemas.android.com/apk/res/android", "widgetFeatures", 0))
    xml.close()
  }
}
