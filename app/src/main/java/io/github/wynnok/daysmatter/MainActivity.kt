package io.github.wynnok.daysmatter

import android.os.Bundle
import android.net.ConnectivityManager
import android.net.Network
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wynnok.daysmatter.ui.AuthScreen
import io.github.wynnok.daysmatter.ui.CategoryEditorScreen
import io.github.wynnok.daysmatter.ui.CategoryListScreen
import io.github.wynnok.daysmatter.ui.ChannelEditorScreen
import io.github.wynnok.daysmatter.ui.ChannelListScreen
import io.github.wynnok.daysmatter.ui.DaysMatterTheme
import io.github.wynnok.daysmatter.ui.EventDetailScreen
import io.github.wynnok.daysmatter.ui.EventEditorScreen
import io.github.wynnok.daysmatter.ui.HomeScreen
import io.github.wynnok.daysmatter.ui.OfflineBanner
import io.github.wynnok.daysmatter.ui.ProfileScreen
import io.github.wynnok.daysmatter.ui.SubEventEditorScreen

class MainActivity : ComponentActivity() {
  private lateinit var appViewModel: MainViewModel
  private val networkCallback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
      runOnUiThread {
        if (::appViewModel.isInitialized && appViewModel.offline) appViewModel.refresh()
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val vm: MainViewModel = viewModel()
      appViewModel = vm
      DaysMatterTheme(vm.darkMode) { DaysMatterApp(vm) }
    }
  }

  override fun onStart() {
    super.onStart()
    val connectivity = getSystemService(ConnectivityManager::class.java)
    connectivity.registerDefaultNetworkCallback(networkCallback)
  }

  override fun onStop() {
    getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback)
    super.onStop()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaysMatterApp(vm: MainViewModel) {
  var page by rememberSaveable { mutableStateOf("home") }
  var eventId by rememberSaveable { mutableIntStateOf(0) }
  var categoryId by rememberSaveable { mutableIntStateOf(0) }
  var channelId by rememberSaveable { mutableIntStateOf(0) }
  var subEventId by rememberSaveable { mutableIntStateOf(0) }
  var categoryFilter by rememberSaveable { mutableIntStateOf(0) }
  var exportText by remember { mutableStateOf<String?>(null) }
  val snackbar = remember { SnackbarHostState() }
  val context = androidx.compose.ui.platform.LocalContext.current

  val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
    val content = exportText
    if (uri != null && content != null) {
      try {
        context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        vm.showMessage("数据已导出")
      } catch (_: Exception) {
        vm.showMessage("保存文件失败")
      }
    }
    exportText = null
  }
  val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      try {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (content == null) vm.showMessage("无法读取文件") else vm.importData(content)
      } catch (_: Exception) {
        vm.showMessage("读取文件失败")
      }
    }
  }

  LaunchedEffect(vm.session?.userId) { page = "home" }
  LaunchedEffect(vm.message) {
    vm.message?.let {
      snackbar.showSnackbar(it)
      vm.clearMessage()
    }
  }

  if (vm.session == null) {
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
      AuthScreen(vm, Modifier.fillMaxSize(), padding)
    }
    return
  }

  val back: () -> Unit = {
    page = when (page) {
      "event_form" -> if (eventId == 0) "home" else "event_detail"
      "sub_form" -> "event_detail"
      "category_form" -> "categories"
      "channel_form" -> "channels"
      "event_detail" -> "home"
      "categories", "channels" -> "profile"
      else -> "home"
    }
  }
  BackHandler(page != "home" && page != "profile") { back() }

  val title = when (page) {
    "home" -> "Days Matter"
    "profile" -> "我的"
    "event_detail" -> "倒数日详情"
    "event_form" -> if (eventId == 0) "添加倒数日" else "编辑倒数日"
    "sub_form" -> if (subEventId == 0) "添加子事件" else "编辑子事件"
    "categories" -> "分类管理"
    "category_form" -> if (categoryId == 0) "添加分类" else "编辑分类"
    "channels" -> "Webhook 渠道"
    "channel_form" -> if (channelId == 0) "添加渠道" else "编辑渠道"
    else -> "Days Matter"
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(title) },
        navigationIcon = {
          if (page !in setOf("home", "profile")) {
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "返回") }
          }
        },
        actions = {
          if (page == "home") {
            IconButton(onClick = vm::refresh) { Icon(Icons.Default.Refresh, "同步") }
          }
        }
      )
    },
    bottomBar = {
      if (page in setOf("home", "profile")) {
        NavigationBar {
          NavigationBarItem(
            selected = page == "home", onClick = { page = "home" },
            icon = { Text("◫") }, label = { Text("记录") }
          )
          NavigationBarItem(
            selected = page == "profile", onClick = { page = "profile" },
            icon = { Text("◉") }, label = { Text("我的") }
          )
        }
      }
    },
    floatingActionButton = {
      if (page == "home") {
        FloatingActionButton(onClick = { eventId = 0; page = "event_form" }) {
          Icon(Icons.Default.Add, "添加倒数日")
        }
      }
    },
    snackbarHost = { SnackbarHost(snackbar) }
  ) { padding ->
    Column(Modifier.fillMaxSize()) {
      if (vm.busy) LinearProgressIndicator()
      if (vm.offline) OfflineBanner(vm.snapshot?.syncedAt)
      when (page) {
        "home" -> HomeScreen(
          vm.snapshot, vm.gridMode, categoryFilter,
          onFilterChange = { categoryFilter = it },
          onGridChange = vm::setGridMode,
          onEventClick = { eventId = it; page = "event_detail" },
          onManageCategories = { page = "categories" },
          modifier = Modifier.fillMaxSize(), contentPadding = padding
        )
        "profile" -> ProfileScreen(
          vm,
          onCategories = { page = "categories" },
          onChannels = { page = "channels" },
          onExport = {
            vm.exportData { text ->
              exportText = text
              exportLauncher.launch("days-matter-backup.json")
            }
          },
          onImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
          contentPadding = padding
        )
        "event_detail" -> EventDetailScreen(
          vm, eventId,
          onEdit = { page = "event_form" },
          onDeleted = { page = "home" },
          onAddSub = { subEventId = 0; page = "sub_form" },
          onEditSub = { subEventId = it; page = "sub_form" },
          contentPadding = padding
        )
        "event_form" -> EventEditorScreen(
          vm, eventId.takeIf { it != 0 },
          onSaved = { id -> eventId = id; page = "event_detail" },
          contentPadding = padding
        )
        "sub_form" -> SubEventEditorScreen(
          vm, eventId, subEventId.takeIf { it != 0 },
          onSaved = { page = "event_detail" }, contentPadding = padding
        )
        "categories" -> CategoryListScreen(
          vm, onAdd = { categoryId = 0; page = "category_form" },
          onEdit = { categoryId = it; page = "category_form" }, contentPadding = padding
        )
        "category_form" -> CategoryEditorScreen(
          vm, categoryId.takeIf { it != 0 }, onSaved = { page = "categories" }, contentPadding = padding
        )
        "channels" -> ChannelListScreen(
          vm, onAdd = { channelId = 0; page = "channel_form" },
          onEdit = { channelId = it; page = "channel_form" }, contentPadding = padding
        )
        "channel_form" -> ChannelEditorScreen(
          vm, channelId.takeIf { it != 0 }, onSaved = { page = "channels" }, contentPadding = padding
        )
      }
    }
  }
}
