package io.github.wynnok.daysmatter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.nlf.calendar.Lunar
import com.nlf.calendar.LunarMonth
import com.nlf.calendar.LunarYear
import com.nlf.calendar.Solar
import java.time.LocalDate

fun lunarLabel(solarDate: String): String? = try {
  val date = LocalDate.parse(solarDate)
  val lunar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
  "农历 ${lunar.year} 年${lunar.monthInChinese}月${lunar.dayInChinese}"
} catch (_: Exception) {
  null
}

@Composable
fun LunarPickerDialog(initialSolarDate: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
  val initialLunar = remember(initialSolarDate) {
    runCatching {
      val date = LocalDate.parse(initialSolarDate)
      Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
    }.getOrNull()
  }
  var yearText by remember(initialSolarDate) { mutableStateOf((initialLunar?.year ?: LocalDate.now().year).toString()) }
  val year = yearText.toIntOrNull()?.takeIf { it in 1900..2100 }
  val months = remember(year) {
    year?.let { runCatching { LunarYear.fromYear(it).monthsInYear.map { month -> month.month } }.getOrNull() }
      ?: (1..12).toList()
  }
  var month by remember(initialSolarDate) { mutableIntStateOf(initialLunar?.month ?: 1) }
  val selectedMonth = month.takeIf { it in months } ?: months.first()
  val dayCount = remember(year, selectedMonth) {
    year?.let { runCatching { LunarMonth.fromYm(it, selectedMonth).dayCount }.getOrNull() } ?: 30
  }
  var day by remember(initialSolarDate) { mutableIntStateOf(initialLunar?.day ?: 1) }
  val selectedDay = day.coerceIn(1, dayCount)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("选择农历日期") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(yearText, { yearText = it }, label = { Text("农历年份（1900–2100）") }, singleLine = true)
        SelectionField("月份", selectedMonth.toString(), months.map {
          it.toString() to if (it < 0) "闰${-it}月" else "${it}月"
        }, { month = it.toInt() })
        SelectionField("日期", selectedDay.toString(), (1..dayCount).map { it.toString() to "${it}日" }, { day = it.toInt() })
      }
    },
    confirmButton = {
      TextButton(onClick = {
        if (year != null) {
          runCatching { Lunar.fromYmd(year, selectedMonth, selectedDay).solar.toYmd() }
            .onSuccess(onSelect)
        }
      }, enabled = year != null) { Text("确定") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
  )
}
