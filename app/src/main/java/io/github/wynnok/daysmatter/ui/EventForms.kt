package io.github.wynnok.daysmatter.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.wynnok.daysmatter.MainViewModel
import io.github.wynnok.daysmatter.data.LocalReminder
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun EventEditorScreen(
  vm: MainViewModel, eventId: Int?, onSaved: (Int) -> Unit, contentPadding: PaddingValues
) {
  val existing = vm.snapshot?.events?.find { it.id == eventId }
  val initialLocal = eventId?.let(vm::localReminder) ?: LocalReminder()
  val categories = vm.snapshot?.categories.orEmpty()
  val channels = vm.snapshot?.channels.orEmpty().filter { it.active }
  val context = LocalContext.current
  var name by remember(eventId) { mutableStateOf(existing?.name.orEmpty()) }
  var targetDate by remember(eventId) {
    mutableStateOf(existing?.targetDate ?: LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1).toString())
  }
  var dateType by remember(eventId) { mutableIntStateOf(existing?.dateType ?: 0) }
  var categoryId by remember(eventId) { mutableIntStateOf(existing?.categoryId ?: categories.firstOrNull()?.id ?: 0) }
  var repeatType by remember(eventId) { mutableIntStateOf(existing?.repeatType ?: 0) }
  var repeatValue by remember(eventId) { mutableStateOf((existing?.repeatValue ?: 1).toString()) }
  var pinned by remember(eventId) { mutableStateOf(existing?.pinned ?: false) }
  var webhookEnabled by remember(eventId) { mutableStateOf(existing?.webhookEnabled ?: false) }
  var channelId by remember(eventId) { mutableIntStateOf(existing?.channelId ?: 0) }
  var remindTitle by remember(eventId) { mutableStateOf(existing?.remindTitle.orEmpty()) }
  var remindContent by remember(eventId) { mutableStateOf(existing?.remindContent.orEmpty()) }
  var advanceDays by remember(eventId) { mutableIntStateOf(existing?.advanceDays ?: 0) }
  var remindTime by remember(eventId) { mutableStateOf(existing?.remindTime?.take(5) ?: "09:00") }
  var localEnabled by remember(eventId) { mutableStateOf(initialLocal.enabled) }
  var localAdvance by remember(eventId) { mutableIntStateOf(initialLocal.advanceDays) }
  var localTime by remember(eventId) { mutableStateOf(initialLocal.time) }
  var showLunarPicker by remember { mutableStateOf(false) }
  var showQuickCategory by remember { mutableStateOf(false) }
  var quickCategoryName by remember { mutableStateOf("") }
  val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    localEnabled = granted
    if (!granted) vm.showMessage("需要通知权限才能开启手机提醒")
  }

  Column(
    Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(22.dp)
  ) {
    FormSection("基本信息") {
      OutlinedTextField(name, { name = it }, label = { Text("事件名称 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
      SelectionField("日期类型", dateType.toString(), listOf("0" to "公历", "1" to "农历"), { dateType = it.toInt() })
      if (dateType == 0) {
        SolarDateField("目标日期 *", targetDate, { targetDate = it })
      } else {
        Text("${lunarLabel(targetDate) ?: "农历日期"} · 对应公历 $targetDate")
        TextButton(onClick = { showLunarPicker = true }) { Text("选择农历日期") }
      }
      if (categories.isEmpty()) {
        Text("添加倒数日之前，需要先创建分类")
        TextButton(onClick = { showQuickCategory = true }) { Text("添加第一个分类") }
      } else {
        SelectionField(
          "分类 *", categoryId.toString(), categories.map { it.id.toString() to it.name },
          { categoryId = it.toInt() }
        )
      }
    }
    FormSection("重复与展示") {
      SelectionField(
        "重复类型", repeatType.toString(),
        listOf("0" to "不重复", "1" to "按天", "2" to "按周", "3" to "按月", "4" to "按年"),
        { repeatType = it.toInt() }
      )
      if (repeatType != 0) {
        OutlinedTextField(
          repeatValue, { repeatValue = it }, label = { Text("每隔多少${listOf("", "天", "周", "月", "年")[repeatType]}") },
          singleLine = true, modifier = Modifier.fillMaxWidth()
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("置顶", modifier = Modifier.weight(1f))
        Switch(pinned, { pinned = it })
      }
    }
    FormSection("Webhook 提醒") {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("使用站外提醒渠道", modifier = Modifier.weight(1f))
        Switch(webhookEnabled, { webhookEnabled = it })
      }
      if (webhookEnabled) {
        if (channels.isEmpty()) Text("先到“我的 → Webhook 渠道”添加并启用渠道")
        SelectionField("提醒渠道", channelId.toString(), channels.map { it.id.toString() to it.name }, { channelId = it.toInt() })
        OutlinedTextField(remindTitle, { remindTitle = it }, label = { Text("提醒标题（可选）") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(remindContent, { remindContent = it }, label = { Text("提醒内容（可选）") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Text("可使用 {{event_name}}、{{target_date}}、{{days}}", style = MaterialTheme.typography.bodySmall)
        SelectionField("提前提醒", advanceDays.toString(), (0..7).map { it.toString() to "$it 天" }, { advanceDays = it.toInt() })
        TimeField("提醒时间（北京时间）", remindTime, { remindTime = it })
      }
    }
    Card(Modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("手机本地提醒", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("在这台设备上通知", modifier = Modifier.weight(1f))
          Switch(localEnabled, { enabled ->
            if (enabled && Build.VERSION.SDK_INT >= 33 &&
              context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
              permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              localEnabled = enabled
            }
          })
        }
        if (localEnabled) {
          SelectionField("提前提醒", localAdvance.toString(), (0..7).map { it.toString() to "$it 天" }, { localAdvance = it.toInt() })
          TimeField("提醒时间（北京时间）", localTime, { localTime = it })
          Text("系统省电策略可能让通知稍晚送达；断网期间保留已安排的下一次提醒。", style = MaterialTheme.typography.bodySmall)
        }
      }
    }
    Button(onClick = {
      val step = repeatValue.toIntOrNull()
      when {
        name.isBlank() || name.length > 100 -> vm.showMessage("事件名称必填，且不能超过 100 字")
        runCatching { LocalDate.parse(targetDate) }.isFailure -> vm.showMessage("请选择合法的目标日期")
        categoryId == 0 -> vm.showMessage("请选择分类")
        repeatType != 0 && (step == null || step !in 1..365) -> vm.showMessage("重复间隔应为 1–365")
        webhookEnabled && channelId == 0 -> vm.showMessage("请选择启用中的 Webhook 渠道")
        remindTitle.length > 50 || remindContent.length > 200 -> vm.showMessage("提醒标题或内容过长")
        else -> {
          val body = JSONObject()
            .put("event_name", name.trim())
            .put("target_date", targetDate)
            .put("date_type", dateType)
            .put("category_id", categoryId)
            .put("repeat_type", repeatType)
            .put("repeat_value", if (repeatType == 0) 1 else step)
            .put("top_flag", if (pinned) 0 else 1)
            .put("remind_flag", if (webhookEnabled) 0 else 1)
          if (webhookEnabled) {
            body.put("remind_channel_id", channelId)
              .put("remind_title", remindTitle.trim())
              .put("remind_content", remindContent.trim())
              .put("advance_days", advanceDays)
              .put("remind_time", "$remindTime:00")
          }
          vm.saveEvent(eventId, body, LocalReminder(localEnabled, localAdvance, localTime), onSaved)
        }
      }
    }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) { Text("保存倒数日") }
  }

  if (showLunarPicker) {
    LunarPickerDialog(targetDate, onDismiss = { showLunarPicker = false }) {
      targetDate = it
      showLunarPicker = false
    }
  }
  if (showQuickCategory) {
    AlertDialog(
      onDismissRequest = { showQuickCategory = false },
      title = { Text("添加分类") },
      text = {
        OutlinedTextField(
          quickCategoryName, { quickCategoryName = it }, label = { Text("分类名称") }, singleLine = true
        )
      },
      confirmButton = {
        TextButton(onClick = {
          if (quickCategoryName.isBlank()) {
            vm.showMessage("请填写分类名称")
          } else {
            val body = JSONObject()
              .put("category_name", quickCategoryName.trim())
              .put("color", "#6366f1")
              .put("icon", "folder")
            vm.write("POST", "/categories", body) {
              showQuickCategory = false
              quickCategoryName = ""
              vm.snapshot?.categories?.maxByOrNull { it.id }?.let { categoryId = it.id }
            }
          }
        }) { Text("创建") }
      },
      dismissButton = { TextButton(onClick = { showQuickCategory = false }) { Text("取消") } }
    )
  }
}

@Composable
fun SubEventEditorScreen(
  vm: MainViewModel, parentEventId: Int, subEventId: Int?, onSaved: () -> Unit,
  contentPadding: PaddingValues
) {
  val existing = vm.snapshot?.events?.find { it.id == parentEventId }?.subEvents?.find { it.id == subEventId }
  var name by remember(subEventId) { mutableStateOf(existing?.name.orEmpty()) }
  var targetDate by remember(subEventId) {
    mutableStateOf(existing?.targetDate ?: LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1).toString())
  }
  var dateType by remember(subEventId) { mutableIntStateOf(existing?.dateType ?: 0) }
  var showLunarPicker by remember { mutableStateOf(false) }

  Column(
    Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    OutlinedTextField(name, { name = it }, label = { Text("子事件名称") }, modifier = Modifier.fillMaxWidth())
    SelectionField("日期类型", dateType.toString(), listOf("0" to "公历", "1" to "农历"), { dateType = it.toInt() })
    if (dateType == 0) {
      SolarDateField("目标日期", targetDate, { targetDate = it })
    } else {
      Text("${lunarLabel(targetDate) ?: "农历日期"} · 对应公历 $targetDate")
      TextButton(onClick = { showLunarPicker = true }) { Text("选择农历日期") }
    }
    Button(onClick = {
      if (name.isBlank() || name.length > 100) {
        vm.showMessage("子事件名称必填，且不能超过 100 字")
      } else {
        val body = JSONObject()
          .put("event_name", name.trim())
          .put("target_date", targetDate)
          .put("date_type", dateType)
        vm.write(
          if (subEventId == null) "POST" else "PUT",
          if (subEventId == null) "/events/$parentEventId/sub-events" else "/sub-events/$subEventId",
          body, onSaved
        )
      }
    }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) { Text("保存子事件") }
  }
  if (showLunarPicker) {
    LunarPickerDialog(targetDate, onDismiss = { showLunarPicker = false }) {
      targetDate = it
      showLunarPicker = false
    }
  }
}
