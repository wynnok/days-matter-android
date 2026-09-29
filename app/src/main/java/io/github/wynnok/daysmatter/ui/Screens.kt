package io.github.wynnok.daysmatter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.wynnok.daysmatter.MainViewModel
import io.github.wynnok.daysmatter.data.Event
import io.github.wynnok.daysmatter.data.Snapshot
import org.json.JSONObject

@Composable
fun AuthScreen(vm: MainViewModel, modifier: Modifier, contentPadding: PaddingValues) {
  var register by remember { mutableStateOf(false) }
  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirm by remember { mutableStateOf("") }

  Column(
    modifier.padding(contentPadding).verticalScroll(rememberScrollState()).padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Spacer(Modifier.height(36.dp))
    Text("Days Matter", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
    Text("把重要的日子放在眼前", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(selected = !register, onClick = { register = false }, label = { Text("登录") })
      FilterChip(selected = register, onClick = { register = true }, label = { Text("注册") })
    }
    if (register) {
      OutlinedTextField(name, { name = it }, label = { Text("昵称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
    OutlinedTextField(email, { email = it }, label = { Text("邮箱") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(
      password, { password = it }, label = { Text("密码") }, singleLine = true,
      visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
    )
    if (register) {
      OutlinedTextField(
        confirm, { confirm = it }, label = { Text("确认密码") }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
      )
    }
    Button(onClick = {
      when {
        email.isBlank() || password.isBlank() -> vm.showMessage("请填写邮箱和密码")
        register && name.isBlank() -> vm.showMessage("请填写昵称")
        register && password.length < 6 -> vm.showMessage("密码至少 6 位")
        register && password != confirm -> vm.showMessage("两次密码不一致")
        register -> vm.register(name, email, password)
        else -> vm.login(email, password)
      }
    }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) {
      Text(if (register) "创建账号" else "登录")
    }
  }
}

@Composable
fun HomeScreen(
  snapshot: Snapshot?, grid: Boolean, categoryFilter: Int,
  onFilterChange: (Int) -> Unit, onGridChange: (Boolean) -> Unit,
  onEventClick: (Int) -> Unit, onManageCategories: () -> Unit,
  modifier: Modifier, contentPadding: PaddingValues
) {
  val categories = snapshot?.categories.orEmpty()
  val events = snapshot?.events.orEmpty().filter { categoryFilter == 0 || it.categoryId == categoryFilter }
  Column(modifier.padding(contentPadding)) {
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("记录 ${events.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
      IconButton(onClick = { onGridChange(!grid) }) {
        Icon(if (grid) Icons.Default.ViewAgenda else Icons.Default.ViewModule, "切换布局")
      }
      TextButton(onClick = onManageCategories) { Text("管理分类") }
    }
    Row(
      Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(selected = categoryFilter == 0, onClick = { onFilterChange(0) }, label = { Text("全部") })
      categories.forEach { category ->
        FilterChip(
          selected = categoryFilter == category.id,
          onClick = { onFilterChange(category.id) },
          label = { Text(category.name) }
        )
      }
    }
    if (snapshot == null) {
      EmptyState("正在同步倒数日…")
    } else if (events.isEmpty()) {
      EmptyState(if (categoryFilter == 0) "还没有倒数日，点击右下角添加" else "此分类暂无倒数日")
    } else if (grid) {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        gridItems(events, key = { it.id }) { event ->
          EventCard(event, onClick = { onEventClick(event.id) })
        }
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(events, key = { it.id }) { event ->
          EventCard(event, onClick = { onEventClick(event.id) })
        }
      }
    }
  }
}

@Composable
private fun EventCard(event: Event, onClick: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        CategoryIcon(event.categoryIcon, event.categoryColor)
        Spacer(Modifier.width(8.dp))
        Text(event.categoryName, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        if (event.pinned) Text("置顶", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
      }
      Text(event.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      Text(daysLabel(event.daysDiff), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
      val nextDate = event.nextOccurrence ?: event.targetDate
      Text(
        if (event.dateType == 1) "$nextDate · ${lunarLabel(nextDate) ?: "农历"}" else nextDate,
        style = MaterialTheme.typography.bodySmall
      )
      if (event.repeatType != 0) Text("重复事件", style = MaterialTheme.typography.labelSmall)
    }
  }
}

@Composable
fun EventDetailScreen(
  vm: MainViewModel, eventId: Int, onEdit: () -> Unit, onDeleted: () -> Unit,
  onAddSub: () -> Unit, onEditSub: (Int) -> Unit, contentPadding: PaddingValues
) {
  val event = vm.snapshot?.events?.find { it.id == eventId }
  var confirmDelete by remember(eventId) { mutableStateOf(false) }
  var subToDelete by remember(eventId) { mutableStateOf<Int?>(null) }
  if (event == null) {
    EmptyState("未找到事件，请同步后重试")
    return
  }
  Column(
    Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Card(Modifier.fillMaxWidth()) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CategoryIcon(event.categoryIcon, event.categoryColor)
          Spacer(Modifier.width(8.dp))
          Text(event.categoryName, style = MaterialTheme.typography.labelLarge)
        }
        Text(event.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(daysLabel(event.daysDiff), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text(
          "目标日期：${event.targetDate}" +
            if (event.dateType == 1) " · ${lunarLabel(event.targetDate) ?: "农历"}" else ""
        )
        event.nextOccurrence?.let {
          Text("下一次：$it" + if (event.dateType == 1) " · ${lunarLabel(it) ?: "农历"}" else "")
        }
        if (event.repeatType != 0) {
          val labels = listOf("不重复", "天", "周", "月", "年")
          Text("每 ${event.repeatValue} ${labels.getOrElse(event.repeatType) { "" }}重复")
        }
        if (event.webhookEnabled) {
          val name = vm.snapshot?.channels?.find { it.id == event.channelId }?.name ?: "Webhook"
          Text("Webhook：$name · 提前 ${event.advanceDays} 天 ${event.remindTime.take(5)}")
        }
        val local = vm.localReminder(event.id)
        if (local.enabled) Text("手机提醒：提前 ${local.advanceDays} 天 ${local.time}（北京时间）")
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
        Icon(Icons.Default.Edit, null)
        Spacer(Modifier.width(6.dp))
        Text("编辑")
      }
      OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
        Icon(Icons.Default.Delete, null)
        Spacer(Modifier.width(6.dp))
        Text("删除")
      }
    }
    HorizontalDivider()
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("子事件", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
      TextButton(onClick = onAddSub) { Text("添加") }
    }
    if (event.subEvents.isEmpty()) Text("还没有子事件")
    event.subEvents.forEach { sub ->
      Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text(sub.name, style = MaterialTheme.typography.titleSmall)
            val lunar = if (sub.dateType == 1) " · ${lunarLabel(sub.targetDate) ?: "农历"}" else ""
            Text("${sub.targetDate}$lunar · ${daysLabel(sub.daysDiff)}", style = MaterialTheme.typography.bodySmall)
          }
          IconButton(onClick = { onEditSub(sub.id) }) { Icon(Icons.Default.Edit, "编辑子事件") }
          IconButton(onClick = { subToDelete = sub.id }) { Icon(Icons.Default.Delete, "删除子事件") }
        }
      }
    }
  }
  if (confirmDelete) {
    ConfirmDeleteDialog("删除倒数日", "此事件及其子事件将被删除。", onDismiss = { confirmDelete = false }) {
      confirmDelete = false
      vm.write("DELETE", "/events/$eventId", onDone = onDeleted)
    }
  }
  subToDelete?.let { id ->
    ConfirmDeleteDialog("删除子事件", "确认删除这个子事件？", onDismiss = { subToDelete = null }) {
      subToDelete = null
      vm.write("DELETE", "/sub-events/$id")
    }
  }
}

@Composable
fun ConfirmDeleteDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) }, text = { Text(body) },
    confirmButton = { TextButton(onClick = onConfirm) { Text("删除") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
  )
}

@Composable
fun ProfileScreen(
  vm: MainViewModel, onCategories: () -> Unit, onChannels: () -> Unit,
  onExport: () -> Unit, onImport: () -> Unit, contentPadding: PaddingValues
) {
  val profile = vm.snapshot?.profile
  var editing by remember { mutableStateOf(false) }
  var nickname by remember(profile?.nickname) { mutableStateOf(profile?.nickname.orEmpty()) }
  var email by remember(profile?.email) { mutableStateOf(profile?.email.orEmpty()) }

  Column(
    Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Card(Modifier.fillMaxWidth()) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(profile?.nickname?.ifBlank { vm.session?.name.orEmpty() } ?: vm.session?.name.orEmpty(),
          style = MaterialTheme.typography.titleLarge)
        Text(profile?.email ?: vm.session?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { editing = true }) { Text("编辑资料") }
      }
    }
    SettingsRow("分类管理", "名称、颜色与图标", onCategories)
    SettingsRow("Webhook 渠道", "配置站外提醒", onChannels)
    SettingsRow("导出数据", "保存 JSON 备份", onExport)
    SettingsRow("导入数据", "从 JSON 备份追加数据", onImport)
    Card(Modifier.fillMaxWidth()) {
      Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("深色模式", modifier = Modifier.weight(1f))
        Switch(checked = vm.darkMode, onCheckedChange = vm::setDarkMode)
      }
    }
    OutlinedButton(onClick = vm::logout, modifier = Modifier.fillMaxWidth()) { Text("退出登录") }
  }
  if (editing) {
    AlertDialog(
      onDismissRequest = { editing = false },
      title = { Text("编辑资料") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(nickname, { nickname = it }, label = { Text("昵称") })
          OutlinedTextField(email, { email = it }, label = { Text("邮箱") })
        }
      },
      confirmButton = {
        TextButton(onClick = {
          if (nickname.isBlank() || email.isBlank()) {
            vm.showMessage("昵称和邮箱不能为空")
          } else {
            vm.write("PUT", "/user/info", JSONObject().put("nickname", nickname.trim()).put("email", email.trim()))
            editing = false
          }
        }) { Text("保存") }
      },
      dismissButton = { TextButton(onClick = { editing = false }) { Text("取消") } }
    )
  }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
  Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Column(Modifier.padding(16.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(subtitle, style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
fun EmptyState(text: String) {
  Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
  }
}
