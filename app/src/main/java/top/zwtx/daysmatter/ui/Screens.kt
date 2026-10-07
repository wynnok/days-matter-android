package top.zwtx.daysmatter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.zwtx.daysmatter.MainViewModel
import top.zwtx.daysmatter.R
import top.zwtx.daysmatter.data.Event
import top.zwtx.daysmatter.data.AppearanceMode
import top.zwtx.daysmatter.data.Snapshot
import top.zwtx.daysmatter.data.overview
import top.zwtx.daysmatter.data.forDisplay
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
          OutlinedTextField(name, { name = it }, label = { FormFieldLabel("昵称", required = true) },
            singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
        }
        OutlinedTextField(email, { email = it }, label = { FormFieldLabel("邮箱", required = true) },
          singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
          password, { password = sanitizePasswordInput(it) },
          label = { FormFieldLabel("密码", required = true) }, singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password, autoCorrectEnabled = false,
            hintLocales = LocaleList("en")
          ),
          supportingText = { Text("仅支持英文字母、数字和半角符号，不含空格") },
          shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()
        )
        if (register) {
          OutlinedTextField(
            confirm, { confirm = sanitizePasswordInput(it) },
            label = { FormFieldLabel("确认密码", required = true) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Password, autoCorrectEnabled = false,
              hintLocales = LocaleList("en")
            ),
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()
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
  val events = snapshot?.events.orEmpty()
    .filter { categoryFilter == 0 || it.categoryId == categoryFilter }
    .sortedByNearestDay()
  val categoryName = categories.find { it.id == categoryFilter }?.name ?: "全部记录"
  val emptyMessage = when {
    snapshot == null && offline -> "当前离线，暂无已同步的记录"
    snapshot == null && syncFailed -> "暂时无法同步，请下拉重试"
    snapshot == null -> "正在同步倒数日…"
    events.isEmpty() && syncFailed -> "上次成功获取时暂无记录，当前数据尚未更新"
    events.isEmpty() && categoryFilter == 0 -> "还没有倒数日，点击下方 + 添加"
    events.isEmpty() -> "此分类暂无倒数日"
    else -> null
  }
  val header: @Composable () -> Unit = {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
      if (snapshot != null && syncFailed && !offline) {
        GlassPanel {
          Column(Modifier.padding(AppDimens.cardInset)) {
            Text("同步失败，显示上次成功获取的数据")
            TextButton(onClick = onRefresh, enabled = !refreshing) { Text("重试同步") }
          }
        }
      }
      HomeHeader(snapshot, categoryName, events.size, onEventClick)
    }
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
      Text(if (snapshot == null) "— 个日子" else "$eventCount 个日子", color = MaterialTheme.colorScheme.onSurfaceVariant,
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
  val featured = next ?: events.filter { (it.daysDiff ?: 0) < 0 }.maxByOrNull { it.daysDiff ?: Int.MIN_VALUE }
    ?: events.firstOrNull { it.daysDiff == null }
  val overview = snapshot?.events?.overview()
  val date = featured?.let {
    if (it.daysDiff == null) "日期待同步"
    else "${if (it.daysDiff < 0) "最近经过" else "下一次"} · ${readableDate(it.nextOccurrence ?: it.targetDate)}"
  }
    ?: LocalDate.now(ZoneId.of("Asia/Shanghai"))
      .format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))
  val shape = MaterialTheme.shapes.large
  Box(modifier.fillMaxWidth().clip(shape)
    .background(Brush.linearGradient(listOf(Color(0xFF245FC0), Color(0xFF08797D))))
    .border(1.dp, Color.White.copy(alpha = 0.24f), shape)
    .then(if (featured == null) Modifier else Modifier.clickable { onEventClick(featured.id) })) {
    Column(Modifier.fillMaxWidth().padding(AppDimens.pageGutter)) {
      Text(date, color = Color.White.copy(alpha = 0.94f), style = MaterialTheme.typography.labelMedium)
      Spacer(Modifier.height(14.dp))
      if (featured != null) {
        val days = featured.daysDiff
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Column(Modifier.weight(1f)) {
            Text(featured.name, color = Color.White, style = MaterialTheme.typography.titleLarge,
              maxLines = 2, overflow = TextOverflow.Ellipsis)
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(when {
              days == null -> "—"
              days == 0 -> "今天"
              days < 0 -> "${-days.toLong()}"
              else -> "$days"
            }, color = Color.White,
              fontSize = if (days == 0) 28.sp else 42.sp,
              lineHeight = 46.sp, fontWeight = FontWeight.SemiBold)
            if (days != 0) Text(when {
              days == null -> pendingDateLabel(featured.repeatType)
              days < 0 -> "天前"
              else -> "天后"
            },
              color = Color.White.copy(alpha = 0.94f),
              style = MaterialTheme.typography.labelMedium)
          }
        }
      } else {
        Text(if (snapshot == null) "尚无已同步数据" else "把重要的日子放在眼前", color = Color.White,
          style = MaterialTheme.typography.titleLarge)
      }
      Spacer(Modifier.height(AppDimens.sectionGap))
      HorizontalDivider(color = Color.White.copy(alpha = 0.34f))
      Spacer(Modifier.height(12.dp))
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OverviewStat(overview?.total?.toString() ?: "—", "事件总数", Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(22.dp).background(Color.White.copy(alpha = 0.34f)))
        OverviewStat(overview?.upcoming?.toString() ?: "—", "近期 7 天", Modifier.weight(1f))
      }
      if (overview != null && overview.pending > 0) {
        Spacer(Modifier.height(8.dp))
        Text("其中 ${overview.pending} 个事件日期待同步", color = Color.White.copy(alpha = 0.94f),
          style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}

@Composable
private fun OverviewStat(value: String, label: String, modifier: Modifier = Modifier) {
  Row(modifier.semantics(mergeDescendants = true) {},
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center) {
    Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.width(7.dp))
    Text(label, color = Color.White.copy(alpha = 0.94f), style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun EventCard(event: Event, grid: Boolean, onClick: () -> Unit) {
  val nextDate = event.nextOccurrence ?: event.targetDate
  val dateLabel = if (event.daysDiff == null) "日期待同步" else compactDate(nextDate)
  val count = when {
    event.daysDiff == null -> "—"
    event.daysDiff == 0 -> "今天"
    event.daysDiff < 0 -> "${-event.daysDiff.toLong()}"
    else -> "${event.daysDiff}"
  }
  val suffix = when {
    event.daysDiff == null -> pendingDateLabel(event.repeatType)
    event.daysDiff == 0 -> ""
    event.daysDiff > 0 -> "天后"
    else -> "天前"
  }
  val countStyle = if (event.daysDiff == 0) MaterialTheme.typography.titleLarge
    else MaterialTheme.typography.headlineMedium
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
          Text(count, style = countStyle, color = MaterialTheme.colorScheme.primary)
          if (suffix.isNotEmpty()) Text(suffix, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text("${if (event.repeatType == 0) "目标" else "下次"} $dateLabel", style = MaterialTheme.typography.bodySmall,
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
          Text(count, style = countStyle, color = MaterialTheme.colorScheme.primary)
          if (suffix.isNotEmpty()) Text(suffix, style = MaterialTheme.typography.labelSmall,
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
  vm: MainViewModel, eventId: Int,
  onAddSub: () -> Unit, onEditSub: (Int) -> Unit, contentPadding: PaddingValues,
  snapshot: Snapshot?
) {
  val event = snapshot?.events?.find { it.id == eventId }
  var subToDelete by remember(eventId) { mutableStateOf<Int?>(null) }
  if (event == null) {
    EmptyState("未找到事件，请同步后重试")
    return
  }
  val nextDate = event.nextOccurrence ?: event.targetDate
  val localReminder = vm.localReminder(event.id)
  val countdown = when {
    event.daysDiff == null -> "—"
    event.daysDiff == 0 -> "今天"
    event.daysDiff < 0 -> (-event.daysDiff.toLong()).toString()
    else -> event.daysDiff.toString()
  }
  val countdownUnit = when {
    event.daysDiff == null -> pendingDateLabel(event.repeatType)
    event.daysDiff == 0 -> ""
    event.daysDiff > 0 -> "天后"
    else -> "天前"
  }
  val countdownColor = if ((event.daysDiff ?: 0) < 0) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.primary
  Column(
    Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
      .padding(AppDimens.pageGutter),
    verticalArrangement = Arrangement.spacedBy(AppDimens.sectionGap)
  ) {
    Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
      .background(MaterialTheme.colorScheme.primaryContainer)) {
      Column(Modifier.padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          EventIcon(event)
          Text(event.categoryName, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(18.dp))
        Text(event.name, style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(countdown, fontSize = if (event.daysDiff == 0) 42.sp else 64.sp,
            lineHeight = 68.sp, fontWeight = FontWeight.Bold,
            color = countdownColor)
          if (countdownUnit.isNotEmpty()) Text(countdownUnit, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(bottom = 9.dp))
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f))
        Spacer(Modifier.height(14.dp))
        Text(if (event.repeatType == 0) "目标日期" else "下一次日期",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
        Text(if (event.daysDiff == null) "等待同步"
          else readableDate(nextDate), style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer)
        if (event.dateType == 1 && event.daysDiff != null) lunarLabel(nextDate)?.let {
          Text(it, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
        }
      }
    }

    if (event.repeatType != 0) {
      DetailSection("日期与规则") {
        DetailInfo("原始日期", readableDate(event.targetDate),
          if (event.dateType == 1) lunarLabel(event.targetDate) else null)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val units = listOf("", "天", "周", "月", "年")
        DetailInfo("重复规则", "每 ${event.repeatValue} ${units.getOrElse(event.repeatType) { "" }}")
      }
    }

    if (event.webhookEnabled || localReminder.enabled) {
      DetailSection("提醒") {
        if (event.webhookEnabled) {
          val channelName = vm.snapshot?.channels?.find { it.id == event.channelId }?.name ?: "Webhook 渠道"
          DetailInfo("站外提醒 · $channelName", "${reminderOffset(event.advanceDays)} · ${event.remindTime.take(5)}")
        }
        if (event.webhookEnabled && localReminder.enabled) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        if (localReminder.enabled) {
          DetailInfo("本机通知", "${reminderOffset(localReminder.advanceDays)} · ${localReminder.time}")
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("子事件", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onAddSub, modifier = Modifier.height(40.dp)) {
          Icon(painterResource(R.drawable.ic_action_add), null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("添加子事件", style = MaterialTheme.typography.labelLarge)
        }
      }
      if (event.subEvents.isEmpty()) {
        GlassPanel {
          Row(Modifier.fillMaxWidth().padding(AppDimens.cardInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center) {
              Icon(Icons.AutoMirrored.Filled.EventNote, null, modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text("还没有子事件", style = MaterialTheme.typography.titleSmall)
              Text("添加一个小节点，让这个日子更完整",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
      event.subEvents.forEach { sub ->
        GlassPanel {
          Row(Modifier.padding(AppDimens.cardInset), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(sub.name, style = MaterialTheme.typography.titleMedium)
              val subDate = sub.nextOccurrence ?: sub.targetDate
              val lunar = if (sub.dateType == 1 && sub.daysDiff != null) lunarLabel(subDate) else null
              Text(if (sub.daysDiff == null) "日期待同步" else "${readableDate(subDate)} · ${daysLabel(sub.daysDiff)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
              if (lunar != null) Text(lunar, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onEditSub(sub.id) }, modifier = Modifier.size(AppDimens.controlHeight)) {
              Icon(painterResource(R.drawable.ic_action_edit), "编辑子事件", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { subToDelete = sub.id }, modifier = Modifier.size(AppDimens.controlHeight)) {
              Icon(painterResource(R.drawable.ic_action_delete), "删除子事件", modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error)
            }
          }
        }
      }
    }
    Spacer(Modifier.height(floatingTabContentClearance()))
  }
  subToDelete?.let { id ->
    ConfirmDeleteDialog("删除子事件", "确认删除这个子事件？", onDismiss = { subToDelete = null }) {
      subToDelete = null
      vm.write("DELETE", "/sub-events/$id")
    }
  }
}

private fun readableDate(value: String): String = runCatching {
  LocalDate.parse(value).format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA))
}.getOrDefault(value)

private fun compactDate(value: String): String = runCatching {
  val date = LocalDate.parse(value)
  val pattern = if (date.year == LocalDate.now(ZoneId.of("Asia/Shanghai")).year) "M月d日" else "yyyy年M月d日"
  date.format(DateTimeFormatter.ofPattern(pattern, Locale.CHINA))
}.getOrDefault(value)

private fun reminderOffset(days: Int): String = if (days == 0) "当天" else "提前 $days 天"

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    GlassPanel {
      Column(Modifier.padding(AppDimens.cardInset),
        verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) { content() }
    }
  }
}

@Composable
private fun DetailInfo(label: String, value: String, note: String? = null) {
  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
    Text(label, style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.titleMedium)
    if (note != null) Text(note, style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
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
  onExport: () -> Unit, onImport: () -> Unit, contentPadding: PaddingValues, onHelp: () -> Unit = {}, onLocalReminders: () -> Unit = {}, onCategories: () -> Unit = {}, onWidgets: () -> Unit = {},
  displaySnapshot: Snapshot? = vm.snapshot?.let { it.copy(events = it.events.map { event -> event.forDisplay() }) }
) {
  val profile = displaySnapshot?.profile
  val overview = displaySnapshot?.events?.overview()
  var editing by remember(vm.session?.userId) { mutableStateOf(false) }
  var confirmingExport by remember { mutableStateOf(false) }
  var choosingAppearance by remember { mutableStateOf(false) }
  var nickname by remember(profile?.nickname) { mutableStateOf(profile?.nickname.orEmpty()) }
  var email by remember(profile?.email) { mutableStateOf(profile?.email.orEmpty()) }
  val displayName = profile?.nickname?.ifBlank { vm.session?.name.orEmpty() } ?: vm.session?.name.orEmpty()
  val displayEmail = profile?.email ?: vm.session?.email.orEmpty()
  val appearanceLabel = when (vm.appearanceMode) {
    AppearanceMode.LIGHT -> "浅色模式"
    AppearanceMode.DARK -> "深色模式"
    AppearanceMode.SYSTEM -> "跟随系统"
  }
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
          Box(Modifier.size(54.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, "默认头像", modifier = Modifier.size(28.dp),
              tint = MaterialTheme.colorScheme.primary)
          }
          Column(Modifier.weight(1f)) {
            Text(displayName, style = MaterialTheme.typography.titleMedium)
            Text(displayEmail, style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
              overflow = TextOverflow.Ellipsis)
          }
          TextButton(onClick = { editing = true }, enabled = profile != null && !vm.busy) { Text("编辑") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant,
          modifier = Modifier.padding(vertical = AppDimens.itemGap))
        Text("事件总数 ${overview?.total ?: "—"} · 近期 7 天 ${overview?.upcoming ?: "—"}",
          style = MaterialTheme.typography.bodyMedium)
        if (overview != null && overview.pending > 0) Text("其中 ${overview.pending} 个事件日期待同步",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(syncStatus, style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("上次成功获取：${formatSyncTime(vm.snapshot?.syncedAt) ?: "尚未获取"}",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    SettingsGroup("提醒与权限") {
      SettingsRow("本地提醒", "当前设备的通知、权限与测试", onLocalReminders)
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      SettingsRow("站外提醒", "配置 Webhook 提醒渠道", onChannels)
    }
    SettingsGroup("数据与同步") {
      SettingsRow("同步状态与重试", syncStatus, vm::refresh)
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRow("导出数据", "保存账号 JSON 备份") { confirmingExport = true }
          if (vm.lastExport > 0) Text("最近导出：已保存 · ${formatSyncTime(vm.lastExport)}", modifier = Modifier.padding(16.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRow("导入数据", "从 JSON 备份追加数据", onImport)
          if (vm.importOutcome == top.zwtx.daysmatter.data.ImportOutcome.UNKNOWN || vm.importOutcome == top.zwtx.daysmatter.data.ImportOutcome.REFRESH_FAILED) {
            Column(Modifier.padding(16.dp)) {
              Text(if (vm.importOutcome == top.zwtx.daysmatter.data.ImportOutcome.UNKNOWN) "导入结果未知，可能已追加。先查看并核实账号数据，避免重复导入。" else "导入请求已完成，账号数据尚未刷新。")
              TextButton(onClick = vm::refreshImportData, enabled = !vm.busy) { Text("仅刷新账号数据") }
              if (vm.importOutcome == top.zwtx.daysmatter.data.ImportOutcome.UNKNOWN) {
                TextButton(onClick = vm::acknowledgeImportOutcome, enabled = !vm.busy) { Text("已核实，重新选择备份") }
              }
            }
          }
    }
    SettingsGroup("分类与偏好") {
      SettingsRow("分类管理", "管理分类名称、颜色与图标", onCategories)
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      SettingsRow("外观", appearanceLabel) { choosingAppearance = true }
    }
    SettingsGroup("桌面小组件") {
      SettingsRow("管理桌面小组件", "添加说明与已有实例配置", onWidgets)
    }
    SettingsGroup("关于与帮助") {
      SettingsRow("帮助与关于", "日期、备份、提醒及版本声明", onHelp)
    }
    OutlinedButton(onClick = vm::logout, modifier = Modifier.fillMaxWidth().height(AppDimens.controlHeight)) {
      Text("退出登录")
    }
    Spacer(Modifier.height(floatingTabContentClearance()))
  }
  if (confirmingExport) {
    AlertDialog(onDismissRequest = { confirmingExport = false }, title = { Text("导出账号备份") },
      text = { Text("包含分类、渠道信息、主事件和子事件，可能含 Webhook 地址与凭据，请妥善保管。不包含账号资料、本地提醒、外观或小组件实例。") },
      confirmButton = { TextButton(onClick = { confirmingExport = false; onExport() }, enabled = !vm.busy) { Text("选择保存位置") } },
      dismissButton = { TextButton(onClick = { confirmingExport = false }) { Text("取消") } })
  }
  if (editing) {
    AlertDialog(
      onDismissRequest = { if (!vm.savingProfile) editing = false },
      title = { Text("编辑资料") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(nickname, { nickname = it }, label = { FormFieldLabel("昵称", required = true) },
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(), enabled = !vm.savingProfile)
          OutlinedTextField(email, { email = it }, label = { FormFieldLabel("邮箱", required = true) },
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(), enabled = !vm.savingProfile)
        }
      },
      confirmButton = {
        TextButton(onClick = {
          if (nickname.isBlank() || email.isBlank()) {
            vm.showMessage("昵称和邮箱不能为空")
          } else {
            vm.saveProfile(nickname, email) { editing = false }
          }
        }, enabled = !vm.busy) { Text(if (vm.savingProfile) "保存中…" else "保存") }
      },
      dismissButton = { TextButton(onClick = { editing = false }, enabled = !vm.savingProfile) { Text("取消") } }
    )
  }
  if (choosingAppearance) {
    AlertDialog(
      onDismissRequest = { choosingAppearance = false },
      title = { Text("外观") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          listOf(
            Triple(AppearanceMode.LIGHT, "浅色模式", "始终使用浅色外观"),
            Triple(AppearanceMode.DARK, "深色模式", "始终使用深色外观"),
            Triple(AppearanceMode.SYSTEM, "跟随系统", "随设备设置自动切换")
          ).forEach { (mode, title, description) ->
            Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
              .selectable(selected = vm.appearanceMode == mode, role = Role.RadioButton) {
                vm.updateAppearanceMode(mode)
                choosingAppearance = false
              }.padding(horizontal = 8.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically) {
              RadioButton(selected = vm.appearanceMode == mode, onClick = null)
              Column(Modifier.padding(start = 8.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(description, style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = { choosingAppearance = false }) { Text("取消") } }
    )
  }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    GlassPanel { Column { content() } }
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
