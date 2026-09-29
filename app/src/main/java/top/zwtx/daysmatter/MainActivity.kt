package top.zwtx.daysmatter

import android.os.Bundle
import android.net.ConnectivityManager
import android.net.Network
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import top.zwtx.daysmatter.data.AppearanceMode
import top.zwtx.daysmatter.ui.AuthScreen
import top.zwtx.daysmatter.ui.CategoryDrawer
import top.zwtx.daysmatter.ui.ChannelEditorScreen
import top.zwtx.daysmatter.ui.ChannelListScreen
import top.zwtx.daysmatter.ui.ConfirmDeleteDialog
import top.zwtx.daysmatter.ui.DaysMatterTheme
import top.zwtx.daysmatter.ui.EventDetailScreen
import top.zwtx.daysmatter.ui.EventEditorScreen
import top.zwtx.daysmatter.ui.FloatingDetailActions
import top.zwtx.daysmatter.ui.FloatingTabBar
import top.zwtx.daysmatter.ui.FloatingTabBottomGap
import top.zwtx.daysmatter.ui.floatingTabContentClearance
import top.zwtx.daysmatter.ui.HomeTopBar
import top.zwtx.daysmatter.ui.HomeScreen
import top.zwtx.daysmatter.ui.OfflineBanner
import top.zwtx.daysmatter.ui.ProfileScreen
import top.zwtx.daysmatter.ui.SubEventEditorScreen
import kotlinx.coroutines.launch

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
      val systemDark = isSystemInDarkTheme()
      val dark = when (vm.appearanceMode) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> systemDark
      }
      DaysMatterTheme(dark) {
        val systemBarColor = MaterialTheme.colorScheme.background.toArgb()
        SideEffect {
          window.statusBarColor = systemBarColor
          window.navigationBarColor = systemBarColor
          WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !dark
          WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !dark
        }
        DaysMatterApp(vm)
      }
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
  var channelId by rememberSaveable { mutableIntStateOf(0) }
  var subEventId by rememberSaveable { mutableIntStateOf(0) }
  var confirmDeleteEvent by remember { mutableStateOf(false) }
  var categoryFilter by rememberSaveable { mutableIntStateOf(0) }
  var exportText by remember { mutableStateOf<String?>(null) }
  val snackbar = remember { SnackbarHostState() }
  val hazeState = remember { HazeState() }
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val drawerMotion = remember { spring<Float>(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow) }
  val scope = rememberCoroutineScope()
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

  LaunchedEffect(vm.session?.userId) {
    page = "home"
    categoryFilter = 0
  }
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

  LaunchedEffect(page, vm.session?.userId) {
    if (page != "home") drawerState.animateTo(DrawerValue.Closed, drawerMotion)
  }
  LaunchedEffect(vm.snapshot?.categories, categoryFilter) {
    if (categoryFilter != 0 && vm.snapshot != null &&
      vm.snapshot?.categories?.none { it.id == categoryFilter } == true) categoryFilter = 0
  }
  val drawerWidthPx = with(LocalDensity.current) { 300.dp.toPx() }
  val drawerOffset = drawerState.currentOffset
  val drawerProgress = if (drawerOffset.isNaN()) {
    if (drawerState.targetValue == DrawerValue.Open) 1f else 0f
  } else {
    (1f + drawerOffset / drawerWidthPx).coerceIn(0f, 1f)
  }

  val back: () -> Unit = {
    page = when (page) {
      "event_form" -> if (eventId == 0) "home" else "event_detail"
      "sub_form" -> "event_detail"
      "channel_form" -> "channels"
      "event_detail" -> "home"
      "channels" -> "profile"
      else -> "home"
    }
  }
  BackHandler(drawerProgress > 0.01f) {
    scope.launch { drawerState.animateTo(DrawerValue.Closed, drawerMotion) }
  }
  BackHandler(page != "home" && page != "profile") { back() }

  val title = when (page) {
    "home" -> "Days Matter"
    "profile" -> "我的"
    "event_detail" -> "倒数日详情"
    "event_form" -> if (eventId == 0) "添加倒数日" else "编辑倒数日"
    "sub_form" -> if (subEventId == 0) "添加子事件" else "编辑子事件"
    "channels" -> "Webhook 渠道"
    "channel_form" -> if (channelId == 0) "添加渠道" else "编辑渠道"
    else -> "Days Matter"
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    gesturesEnabled = page == "home",
    scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.28f),
    drawerContent = {
      CategoryDrawer(
        vm = vm,
        selectedCategoryId = categoryFilter,
        onSelect = { id ->
          categoryFilter = id
          scope.launch { drawerState.animateTo(DrawerValue.Closed, drawerMotion) }
        },
        onClose = { scope.launch { drawerState.animateTo(DrawerValue.Closed, drawerMotion) } },
        isOpen = drawerState.isOpen,
        progress = drawerProgress
      )
    }
  ) {
    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        when (page) {
          "home" -> HomeTopBar(
            grid = vm.gridMode,
            onOpenCategories = { scope.launch { drawerState.animateTo(DrawerValue.Open, drawerMotion) } },
            onGridChange = { vm.updateGridMode(!vm.gridMode) }
          )
          "profile" -> Unit
          else -> TopAppBar(
            title = { Text(title) },
            navigationIcon = {
              IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            },
            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.background
            )
          )
        }
      },
      snackbarHost = {
        SnackbarHost(snackbar, modifier = Modifier.padding(bottom = if (page in setOf("home", "profile", "event_detail")) floatingTabContentClearance() else 0.dp))
      }
    ) { padding ->
      val screenPadding = PaddingValues(0.dp)
      Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().hazeSource(hazeState)) {
          if (vm.busy && !vm.refreshing) LinearProgressIndicator()
          if (vm.offline) OfflineBanner(vm.snapshot?.syncedAt)
          when (page) {
            "home" -> HomeScreen(
              vm.snapshot, vm.gridMode, vm.offline, vm.syncFailed, vm.refreshing, categoryFilter,
              onRefresh = vm::refresh,
              onEventClick = { eventId = it; page = "event_detail" },
              modifier = Modifier.fillMaxSize(), contentPadding = screenPadding
            )
            "profile" -> ProfileScreen(
              vm,
              onChannels = { page = "channels" },
              onExport = {
                vm.exportData { text ->
                  exportText = text
                  exportLauncher.launch("days-matter-backup.json")
                }
              },
              onImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
              contentPadding = screenPadding
            )
            "event_detail" -> EventDetailScreen(
              vm, eventId,
              onAddSub = { subEventId = 0; page = "sub_form" },
              onEditSub = { subEventId = it; page = "sub_form" },
              contentPadding = screenPadding
            )
            "event_form" -> EventEditorScreen(
              vm, eventId.takeIf { it != 0 },
              onSaved = { id -> eventId = id; page = "event_detail" },
              contentPadding = screenPadding
            )
            "sub_form" -> SubEventEditorScreen(
              vm, eventId, subEventId.takeIf { it != 0 },
              onSaved = { page = "event_detail" }, contentPadding = screenPadding
            )
            "channels" -> ChannelListScreen(
              vm, onAdd = { channelId = 0; page = "channel_form" },
              onEdit = { channelId = it; page = "channel_form" }, contentPadding = screenPadding
            )
            "channel_form" -> ChannelEditorScreen(
              vm, channelId.takeIf { it != 0 }, onSaved = { page = "channels" }, contentPadding = screenPadding
            )
          }
        }
        if (page in setOf("home", "profile")) {
          FloatingTabBar(
            selectedPage = page,
            hazeState = hazeState,
            onHome = { page = "home" },
            onAdd = { eventId = 0; page = "event_form" },
            onProfile = { page = "profile" },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = FloatingTabBottomGap)
          )
        }
        if (page == "event_detail" && vm.snapshot?.events?.any { it.id == eventId } == true) {
          FloatingDetailActions(
            hazeState = hazeState,
            onEdit = { page = "event_form" },
            onDelete = { confirmDeleteEvent = true },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = FloatingTabBottomGap)
          )
        }
      }
    }
  }
  if (confirmDeleteEvent && page == "event_detail") {
    ConfirmDeleteDialog("删除倒数日", "此事件及其子事件将被删除。",
      onDismiss = { confirmDeleteEvent = false }) {
      confirmDeleteEvent = false
      vm.write("DELETE", "/events/$eventId", onDone = { page = "home" })
    }
  }
}
