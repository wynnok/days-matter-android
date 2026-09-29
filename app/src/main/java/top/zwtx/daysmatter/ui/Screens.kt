package top.zwtx.daysmatter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.zwtx.daysmatter.MainViewModel
import top.zwtx.daysmatter.data.Event
import top.zwtx.daysmatter.data.Snapshot
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AuthScreen(vm: MainViewModel, modifier: Modifier, contentPadding: PaddingValues) {
  var register by remember { mutableStateOf(false) }
  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirm by remember { mutableStateOf("") }

  Column(
    modifier.padding(contentPadding).background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = AppDimens.pageGutter, vertical = AppDimens.sectionGap),
    verticalArrangement = Arrangement.spacedBy(AppDimens.sectionGap)
  ) {
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text("D / M", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
      Text("记住每一个\n值得期待的日子。", style = MaterialTheme.typography.headlineMedium)
      Text("DAYS MATTER  ·  你的时间手记", color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge)
    }
    GlassPanel {
      Column(Modifier.padding(AppDimens.cardInset),
        verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
        Text(if (register) "创建你的空间" else "欢迎回来", style = MaterialTheme.typography.titleLarge)
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
        }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(AppDimens.controlHeight)) {
          if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
          else Text(if (register) "创建账号" else "进入我的日子")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  snapshot: Snapshot?, grid: Boolean, offline: Boolean, syncFailed: Boolean, refreshing: Boolean,
  categoryFilter: Int, onRefresh: () -> Unit, onEventClick: (Int) -> Unit,
  modifier: Modifier, contentPadding: PaddingValues
) {
  val categories = snapshot?.categories.orEmpty()
  val events = snapshot?.events.orEmpty().filter { categoryFilter == 0 || it.categoryId == categoryFilter }
  val categoryName = categories.find { it.id == categoryFilter }?.name ?: "全部记录"
  val emptyMessage = when {
    snapshot == null && offline -> "当前离线，暂无已同步的记录"
    snapshot == null && syncFailed -> "暂时无法同步，请下拉重试"
    snapshot == null -> "正在同步倒数日…"
    events.isEmpty() && categoryFilter == 0 -> "还没有倒数日，点击下方 + 添加"
    events.isEmpty() -> "此分类暂无倒数日"
    else -> null
  }
  val header: @Composable () -> Unit = {
    HomeHeader(snapshot, categoryName, events.size, onEventClick)
  }
  PullToRefreshBox(
    isRefreshing = refreshing,
    onRefresh = onRefresh,
    modifier = modifier.padding(contentPadding)
  ) {
    if (grid) {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = AppDimens.pageGutter, end = AppDimens.pageGutter,
          bottom = floatingTabContentClearance()),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.itemGap),
        verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)
      ) {
        item(span = { GridItemSpan(maxLineSpan) }) { header() }
        if (emptyMessage != null) {
          item(span = { GridItemSpan(maxLineSpan) }) { HomeEmptyState(emptyMessage) }
        } else {
          gridItems(events, key = { it.id }) { event ->
            EventCard(event, grid = true, onClick = { onEventClick(event.id) })
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = AppDimens.pageGutter, end = AppDimens.pageGutter,
          bottom = floatingTabContentClearance()),
        verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)
      ) {
        item { header() }
        if (emptyMessage != null) {
          item { HomeEmptyState(emptyMessage) }
        } else {
          items(events, key = { it.id }) { event ->
            EventCard(event, grid = false, onClick = { onEventClick(event.id) })
          }
        }
      }
    }
  }
}

@Composable
private fun HomeHeader(
  snapshot: Snapshot?, categoryName: String, eventCount: Int, onEventClick: (Int) -> Unit
) {
  Column {
    Spacer(Modifier.height(4.dp))
    HomeOverview(snapshot, onEventClick)
    Spacer(Modifier.height(AppDimens.sectionGap))
    Row(Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically) {
      Text(categoryName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
      Text("$eventCount 个日子", color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
private fun HomeEmptyState(message: String) {
  Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun HomeOverview(snapshot: Snapshot?, onEventClick: (Int) -> Unit, modifier: Modifier = Modifier) {
  val events = snapshot?.events.orEmpty()
  val next = events.filter { (it.daysDiff ?: -1) >= 0 }.minByOrNull { it.daysDiff ?: Int.MAX_VALUE }
  val today = events.count { it.daysDiff == 0 }
  val upcoming = events.count { (it.daysDiff ?: -1) > 0 }
  val date = LocalDate.now(ZoneId.of("Asia/Shanghai"))
    .format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))
  val shape = MaterialTheme.shapes.large
  Box(modifier.fillMaxWidth().clip(shape)
    .background(Brush.linearGradient(listOf(Color(0xFF2B394E), Color(0xFF425774))))
    .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
    .then(if (next == null) Modifier else Modifier.clickable { onEventClick(next.id) })) {
    Column(Modifier.fillMaxWidth().padding(AppDimens.pageGutter)) {
      Text(date, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium)
      Spacer(Modifier.height(14.dp))
      if (next != null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("下一个日子", color = Color.White.copy(alpha = 0.72f),
              style = MaterialTheme.typography.labelSmall)
            Text(next.name, color = Color.White, style = MaterialTheme.typography.titleLarge,
              maxLines = 2, overflow = TextOverflow.Ellipsis)
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(if (next.daysDiff == 0) "今天" else "${next.daysDiff}", color = Color.White,
              fontSize = if (next.daysDiff == 0) 28.sp else 42.sp,
              lineHeight = 46.sp, fontWeight = FontWeight.SemiBold)
            if (next.daysDiff != 0) Text("天后", color = Color.White.copy(alpha = 0.82f),
              style = MaterialTheme.typography.labelMedium)
          }
        }
      } else {
        Text("把重要的日子放在眼前", color = Color.White,
          style = MaterialTheme.typography.titleLarge)
      }
      Spacer(Modifier.height(AppDimens.sectionGap))
      HorizontalDivider(color = Color.White.copy(alpha = 0.22f))
      Spacer(Modifier.height(12.dp))
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OverviewStat("$today", "今天到期", Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(22.dp).background(Color.White.copy(alpha = 0.2f)))
        OverviewStat("$upcoming", "即将到来", Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun OverviewStat(value: String, label: String, modifier: Modifier = Modifier) {
  Row(modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center) {
    Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.width(7.dp))
    Text(label, color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun EventCard(event: Event, grid: Boolean, onClick: () -> Unit) {
  val nextDate = event.nextOccurrence ?: event.targetDate
  val dateLabel = if (event.dateType == 1) "$nextDate · ${lunarLabel(nextDate) ?: "农历"}" else nextDate
  val count = event.daysDiff?.let { if (it < 0) "${-it}" else "$it" } ?: "—"
  val suffix = when {
    event.daysDiff == null -> "待同步"
    event.daysDiff == 0 -> "今天"
    event.daysDiff > 0 -> "天后"
    else -> "天前"
  }
  GlassPanel(onClick = onClick) {
    if (grid) {
      Column(Modifier.padding(AppDimens.cardInset), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          EventIcon(event)
          Spacer(Modifier.weight(1f))
          if (event.pinned) Text("置顶", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary)
        }
        Text(event.name, style = MaterialTheme.typography.titleMedium, maxLines = 2,
          overflow = TextOverflow.Ellipsis, minLines = 2)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(count, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
          Text(suffix, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text(dateLabel, style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    } else {
      Row(Modifier.padding(AppDimens.cardInset), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
        EventIcon(event)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(event.name, style = MaterialTheme.typography.titleMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text("${event.categoryName} · $dateLabel", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(count, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
          Text(suffix, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@Composable
private fun EventIcon(event: Event) {
  Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
    .background(categoryColor(event.categoryColor).copy(alpha = 0.14f)),
    contentAlignment = Alignment.Center) {
    CategoryIcon(event.categoryIcon, event.categoryColor, size = 26.dp)
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
    Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
      .padding(AppDimens.pageGutter),
    verticalArrangement = Arrangement.spacedBy(AppDimens.sectionGap)
  ) {
    GlassPanel {
      Column(Modifier.padding(AppDimens.cardInset),
        verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          EventIcon(event)
          Spacer(Modifier.width(10.dp))
          Text(event.categoryName, style = MaterialTheme.typography.labelLarge)
        }
        Text(event.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(daysLabel(event.daysDiff), style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
          "目标日期：${event.targetDate}" +
            if (event.dateType == 1) " · ${lunarLabel(event.targetDate) ?: "农历"}" else "",
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        event.nextOccurrence?.let {
          Text("下一次：$it" + if (event.dateType == 1) " · ${lunarLabel(it) ?: "农历"}" else "",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
      OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).height(AppDimens.controlHeight)) {
        Icon(Icons.Default.Edit, null)
        Spacer(Modifier.width(6.dp))
        Text("编辑")
      }
      OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f).height(AppDimens.controlHeight)) {
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
      GlassPanel {
        Row(Modifier.padding(AppDimens.cardInset), verticalAlignment = Alignment.CenterVertically) {
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
  vm: MainViewModel, onChannels: () -> Unit,
  onExport: () -> Unit, onImport: () -> Unit, contentPadding: PaddingValues
) {
  val profile = vm.snapshot?.profile
  var editing by remember { mutableStateOf(false) }
  var nickname by remember(profile?.nickname) { mutableStateOf(profile?.nickname.orEmpty()) }
  var email by remember(profile?.email) { mutableStateOf(profile?.email.orEmpty()) }
  val displayName = profile?.nickname?.ifBlank { vm.session?.name.orEmpty() } ?: vm.session?.name.orEmpty()
  val displayEmail = profile?.email ?: vm.session?.email.orEmpty()
  val syncStatus = when {
    vm.offline -> "离线阅读中 · 等待同步"
    vm.busy -> "正在同步"
    vm.syncFailed -> "已登录 · 同步失败"
    vm.snapshot == null -> "已登录 · 暂未同步"
    else -> "已登录 · 数据已同步"
  }

  Column(
    Modifier.fillMaxSize().padding(contentPadding).background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = AppDimens.pageGutter, vertical = AppDimens.pageGutter),
    verticalArrangement = Arrangement.spacedBy(AppDimens.sectionGap)
  ) {
    Text("我的", style = MaterialTheme.typography.headlineMedium)
    GlassPanel {
      Column(Modifier.padding(AppDimens.cardInset)) {
        Row(verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
          Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(displayName.take(1).ifBlank { "日" }, style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer)
          }
          Column(Modifier.weight(1f)) {
            Text(displayName, style = MaterialTheme.typography.titleMedium)
            Text(displayEmail, style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
              overflow = TextOverflow.Ellipsis)
          }
          TextButton(onClick = { editing = true }) { Text("编辑") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant,
          modifier = Modifier.padding(vertical = AppDimens.itemGap))
        Text(syncStatus, style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
      Text("设置", style = MaterialTheme.typography.titleMedium)
      GlassPanel {
        Column {
          SettingsRow("Webhook 渠道", "配置站外提醒", onChannels)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRow("导出数据", "保存 JSON 备份", onExport)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRow("导入数据", "从 JSON 备份追加数据", onImport)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Column(Modifier.padding(horizontal = AppDimens.cardInset, vertical = 14.dp)) {
            Text("外观", style = MaterialTheme.typography.titleMedium)
            Text("自动跟随系统深色模式", style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
    OutlinedButton(onClick = vm::logout, modifier = Modifier.fillMaxWidth().height(AppDimens.controlHeight)) {
      Text("退出登录")
    }
    Spacer(Modifier.height(floatingTabContentClearance()))
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
  Row(Modifier.fillMaxWidth().clickable(onClick = onClick)
    .padding(horizontal = AppDimens.cardInset, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(subtitle, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(18.dp))
  }
}

@Composable
fun EmptyState(text: String) {
  Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
  }
}
