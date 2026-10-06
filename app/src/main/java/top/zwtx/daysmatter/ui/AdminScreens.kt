package top.zwtx.daysmatter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.zwtx.daysmatter.MainViewModel
import org.json.JSONObject

@Composable
fun CategoryListScreen(
  vm: MainViewModel, onAdd: () -> Unit, onEdit: (Int) -> Unit, contentPadding: PaddingValues
) {
  val categories = vm.snapshot?.categories.orEmpty()
  var deleteId by remember { mutableStateOf<Int?>(null) }
  Column(Modifier.fillMaxSize().padding(contentPadding)) {
    Row(Modifier.fillMaxWidth().padding(horizontal = AppDimens.drawerInnerGutter,
      vertical = AppDimens.drawerInnerGutter),
      verticalAlignment = Alignment.CenterVertically) {
      Text("共 ${categories.size} 个分类", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
      TextButton(onClick = onAdd) { Text("添加分类") }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(AppDimens.drawerInnerGutter),
      contentPadding = PaddingValues(horizontal = AppDimens.drawerInnerGutter, vertical = 4.dp)) {
      items(categories, key = { it.id }) { category ->
        GlassPanel {
          Row(Modifier.padding(AppDimens.drawerCardInset), verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(category.icon, category.color)
            Spacer(Modifier.width(12.dp))
            Text(category.name, modifier = Modifier.weight(1f), maxLines = 1,
              overflow = TextOverflow.Ellipsis)
            IconButton(onClick = { onEdit(category.id) }) { Icon(Icons.Default.Edit, "编辑分类") }
            IconButton(onClick = { deleteId = category.id }) { Icon(Icons.Default.Delete, "删除分类") }
          }
        }
      }
    }
  }
  deleteId?.let { id ->
    ConfirmDeleteDialog("删除分类", "确认删除这个分类？", onDismiss = { deleteId = null }) {
      deleteId = null
      if (vm.snapshot?.events?.any { it.categoryId == id } == true) {
        vm.showMessage("请先移动或删除该分类下的事件")
      } else {
        vm.write("DELETE", "/categories/$id")
      }
    }
  }
}

@Composable
fun CategoryEditorScreen(vm: MainViewModel, categoryId: Int?, onSaved: () -> Unit, contentPadding: PaddingValues) {
  val existing = vm.snapshot?.categories?.find { it.id == categoryId }
  var name by remember(categoryId) { mutableStateOf(existing?.name.orEmpty()) }
  var icon by remember(categoryId) { mutableStateOf(existing?.icon ?: "heart") }
  var color by remember(categoryId) { mutableStateOf(existing?.color ?: "#FF6B35") }
  var search by remember { mutableStateOf("") }
  val colors = listOf("#FF6B35", "#E03131", "#F08C00", "#2B8A3E", "#0C8599", "#6366F1", "#7C5CFF", "#D6336C")
  val icons = CategoryIconResources.ids.filter { id ->
    search.isBlank() || id.contains(search.trim(), ignoreCase = true) ||
      iconKeywords.any { (keyword, ids) -> search.trim().let { keyword.contains(it) || it.contains(keyword) } && id in ids }
  }

  Column(
    Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
      .padding(AppDimens.drawerInnerGutter),
    verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap)
  ) {
    OutlinedTextField(name, { name = it }, label = { FormFieldLabel("分类名称", required = true) },
      singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
    Text("颜色", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      colors.forEach { option ->
        Box(
          Modifier.size(AppDimens.controlHeight).clickable { color = option },
          contentAlignment = Alignment.Center
        ) {
          Box(Modifier.size(36.dp).clip(MaterialTheme.shapes.small)
            .background(categoryColor(option)), contentAlignment = Alignment.Center) {
            if (color == option) Text("✓", color = androidx.compose.ui.graphics.Color.White)
          }
        }
      }
    }
    Text("图标", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(search, { search = it }, label = { Text("搜索图标，例如：生日、旅行、heart") },
      shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
    LazyVerticalGrid(
      columns = GridCells.Adaptive(AppDimens.controlHeight), modifier = Modifier.fillMaxWidth().height(240.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      gridItems(icons) { id ->
        FilterChip(
          selected = icon == id, onClick = { icon = id },
          label = { Icon(painterResource(CategoryIconResources.drawable(id)), id, tint = categoryColor(color)) },
          modifier = Modifier.fillMaxWidth().height(AppDimens.controlHeight)
        )
      }
    }
    Text("已选：$icon", style = MaterialTheme.typography.bodySmall)
    Button(onClick = {
      if (name.isBlank()) {
        vm.showMessage("请填写分类名称")
      } else {
        val body = JSONObject()
          .put("category_name", name.trim())
          .put("color", color)
          .put("icon", icon)
          .put("sort_order", existing?.sortOrder ?: 0)
        vm.write(if (categoryId == null) "POST" else "PUT",
          if (categoryId == null) "/categories" else "/categories/$categoryId", body, { onSaved() })
      }
    }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth().height(AppDimens.controlHeight)) {
      Text("保存分类")
    }
  }
}

private val iconKeywords = mapOf(
  "工作" to listOf("briefcase", "office", "laptop", "clipboard"),
  "纪念" to listOf("heart", "star", "rings", "gift"),
  "生日" to listOf("cake", "party", "balloon"),
  "旅行" to listOf("plane", "map", "compass", "camera"),
  "账单" to listOf("wallet", "creditCard", "receipt"),
  "生活" to listOf("coffee", "music", "home", "flower"),
  "宠物" to listOf("dog", "cat"),
  "时间" to listOf("clock", "hourglass", "alarmClock")
)

@Composable
fun ChannelListScreen(vm: MainViewModel, onAdd: () -> Unit, onEdit: (Int) -> Unit, contentPadding: PaddingValues) {
  val channels = vm.snapshot?.channels.orEmpty()
  var deleteId by remember { mutableStateOf<Int?>(null) }
  Column(Modifier.fillMaxSize().padding(contentPadding)) {
    Row(Modifier.fillMaxWidth().padding(horizontal = AppDimens.pageGutter, vertical = AppDimens.itemGap),
      verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text("提醒渠道", style = MaterialTheme.typography.titleMedium)
        Text("共 ${channels.size} 个 Webhook 渠道", style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      TextButton(onClick = onAdd) { Text("添加渠道") }
    }
    if (channels.isEmpty()) Text("还没有 Webhook 渠道，点击右上角添加",
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = AppDimens.pageGutter, vertical = AppDimens.sectionGap))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(AppDimens.itemGap),
      contentPadding = PaddingValues(horizontal = AppDimens.pageGutter, vertical = 4.dp)) {
      items(channels, key = { it.id }) { channel ->
        GlassPanel {
          Row(Modifier.padding(AppDimens.cardInset), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
              Text(channel.name, style = MaterialTheme.typography.titleSmall)
              Text("${channel.format} · ${if (channel.active) "已启用" else "已停用"}", style = MaterialTheme.typography.bodySmall)
              Text(channel.account, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            IconButton(onClick = { onEdit(channel.id) }) { Icon(Icons.Default.Edit, "编辑渠道") }
            IconButton(onClick = { deleteId = channel.id }) { Icon(Icons.Default.Delete, "删除渠道") }
          }
        }
      }
    }
  }
  deleteId?.let { id ->
    ConfirmDeleteDialog("删除渠道", "确认删除此 Webhook 渠道？", onDismiss = { deleteId = null }) {
      deleteId = null
      if (vm.snapshot?.events?.any { it.webhookEnabled && it.channelId == id } == true) {
        vm.showMessage("请先关闭使用此渠道的事件提醒")
      } else {
        vm.write("DELETE", "/remind-channels/$id")
      }
    }
  }
}

@Composable
fun ChannelEditorScreen(vm: MainViewModel, channelId: Int?, onSaved: () -> Unit, contentPadding: PaddingValues) {
  val existing = vm.snapshot?.channels?.find { it.id == channelId }
  var name by remember(channelId) { mutableStateOf(existing?.name.orEmpty()) }
  var account by remember(channelId) { mutableStateOf(existing?.account.orEmpty()) }
  var format by remember(channelId) { mutableStateOf(existing?.format ?: "generic") }
  var token by remember(channelId) { mutableStateOf("") }
  var template by remember(channelId) { mutableStateOf(existing?.template.orEmpty()) }
  var active by remember(channelId) { mutableStateOf(existing?.active ?: true) }
  val formats = listOf(
    "generic" to "通用 Webhook", "wecom" to "企业微信", "dingtalk" to "钉钉",
    "feishu" to "飞书", "serverchan" to "Server酱", "custom" to "自定义 JSON"
  )

  fun channelPayload(): JSONObject? {
    if (!account.trim().startsWith("https://")) {
      vm.showMessage("请填写 HTTPS Webhook 地址")
      return null
    }
    if (format == "custom" && template.isBlank()) {
      vm.showMessage("请填写自定义 JSON 模板")
      return null
    }
    return JSONObject()
      .put("channel_type", "webhook")
      .put("account", account.trim())
      .put("channel_name", name.trim())
      .put("format_type", format)
      .also { body ->
        if (channelId != null) body.put("is_active", if (active) 1 else 0)
        if (token.isNotBlank()) body.put("auth_token", token.trim())
        if (format == "custom") body.put("body_template", template.trim())
      }
  }

  Column(
    Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
      .padding(AppDimens.pageGutter),
    verticalArrangement = Arrangement.spacedBy(AppDimens.sectionGap)
  ) {
    FormSection("渠道信息") {
      OutlinedTextField(name, { name = it }, label = { FormFieldLabel("渠道名称", required = true) },
        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(account, { account = it }, label = { FormFieldLabel("Webhook HTTPS 地址", required = true) },
        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
      SelectionField("消息格式", format, formats, { format = it })
      OutlinedTextField(
        token, { token = it }, label = { Text(if (existing?.hasAuthToken == true) "Token（留空保持原值）" else "Token（可选）") },
        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()
      )
      if (format == "custom") {
        OutlinedTextField(
          template, { template = it }, label = { Text("自定义 JSON 模板") },
          minLines = 5, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()
        )
        Text("支持 {{title}} 和 {{content}}，字符串占位符需放在引号内", style = MaterialTheme.typography.bodySmall)
      }
      if (channelId != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("启用渠道", modifier = Modifier.weight(1f))
          Switch(active, { active = it })
        }
      } else {
        Text("新渠道保存后默认启用", style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.itemGap)) {
      OutlinedButton(onClick = {
        channelPayload()?.let { body ->
          if (channelId != null) body.put("channel_id", channelId)
          vm.testChannel(body)
        }
      }, enabled = !vm.busy, modifier = Modifier.weight(1f).height(AppDimens.controlHeight)) {
        if (vm.testingChannel) {
          CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
          Spacer(Modifier.width(6.dp))
        }
        Text(if (vm.testingChannel) "测试中…" else "测试渠道")
      }
      Button(onClick = {
        if (name.isBlank()) {
          vm.showMessage("请填写渠道名称")
        } else {
          channelPayload()?.let { body ->
            vm.write(if (channelId == null) "POST" else "PUT",
              if (channelId == null) "/remind-channels" else "/remind-channels/$channelId", body, { onSaved() })
          }
        }
      }, enabled = !vm.busy, modifier = Modifier.weight(1f).height(AppDimens.controlHeight)) {
        Text("保存渠道")
      }
    }
    Text("测试会发送一条消息，不会保存当前修改。", style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
