package io.github.wynnok.daysmatter.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun categoryColor(value: String): Color = try {
  Color(android.graphics.Color.parseColor(value))
} catch (_: Exception) {
  Color(0xFF6366F1)
}

@Composable
fun CategoryIcon(identifier: String, color: String, size: Dp = 24.dp) {
  Icon(
    painter = painterResource(CategoryIconResources.drawable(identifier)),
    contentDescription = null,
    tint = categoryColor(color),
    modifier = Modifier.size(size).padding(2.dp)
  )
}

@Composable
fun OfflineBanner(syncedAt: Long?) {
  val time = syncedAt?.takeIf { it > 0 }?.let {
    SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).apply {
      timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }.format(Date(it))
  }
  Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
    Text(
      "当前离线，显示${time?.let { " $it 同步的" } ?: ""}缓存数据。编辑需要联网。",
      modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall
    )
  }
}

fun daysLabel(days: Int?): String = when {
  days == null -> "日期待同步"
  days == 0 -> "就是今天"
  days > 0 -> "还有 $days 天"
  else -> "已过 ${-days} 天"
}

@Composable
fun SelectionField(
  label: String,
  selected: String,
  choices: List<Pair<String, String>>,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  Column(modifier) {
    Text(label, style = MaterialTheme.typography.labelMedium)
    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
      Text(choices.firstOrNull { it.first == selected }?.second ?: "请选择")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      choices.forEach { (value, title) ->
        DropdownMenuItem(text = { Text(title) }, onClick = {
          onSelect(value)
          expanded = false
        })
      }
    }
  }
}

@Composable
fun SolarDateField(label: String, value: String, onChange: (String) -> Unit) {
  val context = LocalContext.current
  val initial = runCatching { LocalDate.parse(value) }.getOrElse { LocalDate.now() }
  Column {
    Text(label, style = MaterialTheme.typography.labelMedium)
    OutlinedButton(onClick = {
      DatePickerDialog(
        context,
        { _, year, month, day -> onChange("%04d-%02d-%02d".format(year, month + 1, day)) },
        initial.year, initial.monthValue - 1, initial.dayOfMonth
      ).show()
    }, modifier = Modifier.fillMaxWidth()) {
      Text(value.ifBlank { "选择日期" })
    }
  }
}

@Composable
fun TimeField(label: String, value: String, onChange: (String) -> Unit) {
  val context = LocalContext.current
  val parts = value.split(':')
  val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
  val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
  Column {
    Text(label, style = MaterialTheme.typography.labelMedium)
    OutlinedButton(onClick = {
      TimePickerDialog(context, { _, selectedHour, selectedMinute ->
        onChange("%02d:%02d".format(selectedHour, selectedMinute))
      }, hour, minute, true).show()
    }, modifier = Modifier.fillMaxWidth()) { Text(value.take(5)) }
  }
}

@Composable
fun FormSection(title: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    content()
  }
}
