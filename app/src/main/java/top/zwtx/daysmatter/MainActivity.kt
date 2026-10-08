package top.zwtx.daysmatter

import android.os.Bundle
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import top.zwtx.daysmatter.data.AppearanceMode
import top.zwtx.daysmatter.data.forDisplay
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
import top.zwtx.daysmatter.ui.HelpScreen
import top.zwtx.daysmatter.ui.HomeScreen
import top.zwtx.daysmatter.ui.OfflineBanner
import top.zwtx.daysmatter.ui.ProfileScreen
import top.zwtx.daysmatter.ui.SubEventEditorScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.Instant

class MainActivity : ComponentActivity() {
  private val appViewModel: MainViewModel by viewModels()
  private val networkCallback = object : ConnectivityManager.NetworkCallback() {
    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
      val available = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
      runOnUiThread { appViewModel.networkChanged(available) }
    }
    override fun onLost(network: Network) {
      runOnUiThread {
        val connectivity = getSystemService(ConnectivityManager::class.java)
        val available = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
          ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        appViewModel.networkChanged(available)
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    appViewModel.openEvent(intent)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val vm = appViewModel
    if (savedInstanceState == null) vm.openEvent(intent)
    setContent {
      LaunchedEffect(vm) {
        val connectivity = getSystemService(ConnectivityManager::class.java)
        vm.networkChanged(connectivity.getNetworkCapabilities(connectivity.activeNetwork)
          ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
      }
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
    appViewModel.networkChanged(
      connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    )
  }

  override fun onResume() {
    super.onResume()
    appViewModel.refreshIfNeeded()
  }

  override fun onStop() {
    getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback)
    super.onStop()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DaysMatterApp(vm: MainViewModel) {
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val displayInstant by produceState(Instant.now(), lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      vm.refreshIfNeeded()
      while (true) {
        value = Instant.now()
        delay(30_000)
      }
    }
  }
  val displaySnapshot = vm.snapshot?.let { snapshot ->
    snapshot.copy(events = snapshot.events.map { it.forDisplay(displayInstant) })
  }
  var page by rememberSaveable { mutableStateOf("home") }
  var eventId by rememberSaveable { mutableIntStateOf(0) }
  var channelId by rememberSaveable { mutableIntStateOf(0) }
  var subEventId by rememberSaveable { mutableIntStateOf(0) }
  var confirmDeleteEvent by remember { mutableStateOf(false) }
  var editingCategoryId by rememberSaveable { mutableIntStateOf(0) }
  var categoryFilter by rememberSaveable { mutableIntStateOf(0) }
  val homeStateHolder = key(vm.session?.userId) { rememberSaveableStateHolder() }
  val snackbar = remember { SnackbarHostState() }
  val hazeState = remember { HazeState() }
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val drawerMotion = remember { spring<Float>(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow) }
  val scope = rememberCoroutineScope()
  val context = androidx.compose.ui.platform.LocalContext.current

  val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
    vm.completeExportSelection(uri)
  }
  val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    vm.completeImportSelection(uri)
  }

  top.zwtx.daysmatter.ui.BackupImportDialog(vm)

  LaunchedEffect(vm.session?.userId, vm.eventEntryRequest) {
    page = "home"
    categoryFilter = 0
  }
  LaunchedEffect(vm.homeCategoryNavigation) {
    vm.homeCategoryNavigation?.let {
      categoryFilter = it
      page = "home"
      vm.consumeHomeNavigation()
    }
  }
  LaunchedEffect(vm.session?.userId, vm.eventNavigation) {
    val target = vm.eventNavigation
    if (target != null && target.userId == vm.session?.userId) {
      eventId = target.eventId
      page = "event_detail"
      vm.consumeEventNavigation()
    }
  }
  LaunchedEffect(vm.eventCreationNavigation) {
    if (vm.eventCreationNavigation && vm.session != null) {
      eventId = 0
      page = "event_form"
      vm.consumeEventCreation()
    }
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
      "category_form" -> "categories"
      "channel_form" -> "channels"
      "event_detail" -> "home"
      "channels", "help", "local_reminders", "categories", "widgets" -> "profile"
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
    "widgets" -> "桌面小组件"
    "categories" -> "分类管理"
    "category_form" -> if (editingCategoryId == 0) "添加分类" else "编辑分类"
    "local_reminders" -> "本地提醒"
    "channels" -> "站外提醒"
                    "help" -> "帮助与关于"
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
            "home" -> homeStateHolder.SaveableStateProvider(categoryFilter) {
              HomeScreen(
                displaySnapshot, vm.gridMode, vm.offline, vm.syncFailed, vm.refreshing, categoryFilter,
                onRefresh = vm::refresh,
                onEventClick = { eventId = it; page = "event_detail" },
                modifier = Modifier.fillMaxSize(), contentPadding = screenPadding
              )
            }
            "profile" -> ProfileScreen(
              vm,
              onChannels = { page = "channels" },
              onExport = {
                vm.exportData {
                  exportLauncher.launch("days-matter-backup.json")
                }
              },
              onImport = { if (vm.beginImportSelection()) importLauncher.launch(arrayOf("application/json", "text/plain")) },
              onHelp = { page = "help" },
              onLocalReminders = { page = "local_reminders" },
              onWidgets = { page = "widgets" },
              displaySnapshot = displaySnapshot,
              contentPadding = screenPadding
            )
            "event_detail" -> EventDetailScreen(
              vm, eventId,
              onAddSub = { subEventId = 0; page = "sub_form" },
              onEditSub = { subEventId = it; page = "sub_form" },
              contentPadding = screenPadding, snapshot = displaySnapshot
            )
            "event_form" -> EventEditorScreen(
              vm, eventId.takeIf { it != 0 },
              onSaved = { id ->
                if (eventId != 0) vm.showMessage("已更新")
                eventId = id
                page = "event_detail"
              },
              contentPadding = screenPadding
            )
            "sub_form" -> SubEventEditorScreen(
              vm, eventId, subEventId.takeIf { it != 0 },
              onSaved = { page = "event_detail" }, contentPadding = screenPadding
            )
            "widgets" -> top.zwtx.daysmatter.ui.WidgetManagementScreen(vm,
              onConfigure = { context.startActivity(top.zwtx.daysmatter.widget.ImportantDayWidgetProvider.configurationIntent(context, it)) }, padding = screenPadding)
            "categories" -> top.zwtx.daysmatter.ui.CategoryListScreen(vm,
              onAdd = { editingCategoryId = 0; page = "category_form" },
              onEdit = { editingCategoryId = it; page = "category_form" }, contentPadding = screenPadding)
            "category_form" -> top.zwtx.daysmatter.ui.CategoryEditorScreen(vm, editingCategoryId.takeIf { it != 0 },
              onSaved = { page = "categories" }, contentPadding = screenPadding)
            "local_reminders" -> top.zwtx.daysmatter.ui.LocalReminderScreen(vm,
              onConfigure = { eventId = it; page = "event_form" }, contentPadding = screenPadding, displayInstant = displayInstant)
            "help" -> HelpScreen(screenPadding)
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
      vm.write("DELETE", "/events/$eventId", onWritten = { page = "home" })
    }
  }
}
