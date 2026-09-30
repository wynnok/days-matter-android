package top.zwtx.daysmatter.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.zwtx.daysmatter.MainViewModel
import top.zwtx.daysmatter.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

internal val FloatingTabBottomGap = 10.dp

@Composable
private fun floatingTabHeight() = if (LocalDensity.current.fontScale >= 1.3f) 72.dp else 56.dp

@Composable
internal fun floatingTabContentClearance() = floatingTabHeight() + FloatingTabBottomGap + 18.dp

@Composable
fun HomeTopBar(grid: Boolean, onOpenCategories: () -> Unit, onGridChange: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Row(Modifier.fillMaxWidth().statusBarsPadding().height(64.dp)
    .padding(horizontal = AppDimens.pageGutter),
    verticalAlignment = Alignment.CenterVertically) {
    HomeBarButton(onOpenCategories) { Icon(Icons.Default.Menu, "打开分类抽屉", Modifier.size(20.dp)) }
    Text("我的日子", color = colors.onBackground, style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.weight(1f).padding(start = 12.dp))
    HomeBarButton(onGridChange) {
      Icon(if (grid) Icons.Default.ViewAgenda else Icons.Default.ViewModule, "切换布局", Modifier.size(20.dp))
    }
  }
}

@Composable
private fun HomeBarButton(onClick: () -> Unit, content: @Composable () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = onClick),
    contentAlignment = Alignment.Center) {
    Box(Modifier.size(40.dp).clip(CircleShape)
      .background(colors.surfaceVariant)
      .border(1.dp, colors.outlineVariant, CircleShape),
      contentAlignment = Alignment.Center) { content() }
  }
}

@Composable
fun FloatingTabBar(
  selectedPage: String,
  hazeState: HazeState,
  onHome: () -> Unit,
  onAdd: () -> Unit,
  onProfile: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme
  val height = floatingTabHeight()
  FloatingGlassContainer(hazeState, modifier) {
    Row(
      Modifier.fillMaxWidth().height(height).padding(4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      FloatingTabItem("记录", selectedPage == "home", Icons.AutoMirrored.Filled.EventNote, onHome,
        Modifier.weight(1f))
      Box(
        Modifier.width(54.dp).height(48.dp)
          .shadow(3.dp, RoundedCornerShape(16.dp), ambientColor = colors.primary.copy(alpha = 0.16f))
          .clip(RoundedCornerShape(16.dp))
          .background(colors.primary)
          .clickable(role = Role.Button, onClick = onAdd),
        contentAlignment = Alignment.Center
      ) {
        Icon(painterResource(R.drawable.ic_action_add), "添加倒数日",
          modifier = Modifier.size(25.dp), tint = colors.onPrimary)
      }
      FloatingTabItem("我的", selectedPage == "profile", Icons.Default.PersonOutline, onProfile,
        Modifier.weight(1f))
    }
  }
}

@Composable
fun FloatingDetailActions(
  hazeState: HazeState,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme
  FloatingGlassContainer(hazeState, modifier) {
    Row(Modifier.fillMaxWidth().height(floatingTabHeight()).padding(4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      FloatingDetailAction("编辑", R.drawable.ic_action_edit, colors.primary,
        colors.primary.copy(alpha = 0.12f), onEdit, Modifier.weight(1f))
      FloatingDetailAction("删除", R.drawable.ic_action_delete, colors.error,
        colors.error.copy(alpha = 0.08f), onDelete, Modifier.weight(1f))
    }
  }
}

@Composable
private fun FloatingDetailAction(
  label: String, icon: Int, color: Color, background: Color,
  onClick: () -> Unit, modifier: Modifier = Modifier
) {
  Row(modifier.fillMaxHeight().clip(RoundedCornerShape(24.dp))
    .background(background).clickable(role = Role.Button, onClick = onClick),
    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
    Icon(painterResource(icon), null, modifier = Modifier.size(19.dp), tint = color)
    Spacer(Modifier.width(7.dp))
    Text(label, color = color, style = MaterialTheme.typography.labelLarge)
  }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun FloatingGlassContainer(
  hazeState: HazeState, modifier: Modifier = Modifier,
  horizontalInset: Dp = 36.dp,
  content: @Composable BoxScope.() -> Unit
) {
  val colors = MaterialTheme.colorScheme
  val shape = RoundedCornerShape(28.dp)
  Box(modifier.fillMaxWidth().padding(horizontal = horizontalInset)
    .shadow(8.dp, shape, ambientColor = colors.primary.copy(alpha = 0.12f))
    .clip(shape)
    .hazeEffect(state = hazeState,
      style = HazeMaterials.ultraThin(containerColor = colors.surface.copy(alpha = 0.12f)))
    .background(Brush.verticalGradient(listOf(
      colors.surface.copy(alpha = 0.12f),
      colors.surfaceVariant.copy(alpha = 0.08f)
    )))
    .border(1.dp, colors.outlineVariant.copy(alpha = 0.65f), shape)) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.42f)))
    content()
  }
}

@Composable
private fun FloatingTabItem(
  label: String,
  selected: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme
  val background = animateColorAsState(
    if (selected) colors.primary.copy(alpha = 0.16f) else Color.Transparent,
    animationSpec = tween(240), label = "tabBackground"
  )
  val foreground = animateColorAsState(
    if (selected) colors.primary else colors.onSurface,
    animationSpec = tween(240), label = "tabForeground"
  )
  val scale = animateFloatAsState(if (selected) 1f else 0.94f,
    animationSpec = tween(240), label = "tabScale")
  Column(
    modifier.fillMaxHeight().clip(RoundedCornerShape(24.dp))
      .background(background.value)
      .clickable(role = Role.Tab, onClick = onClick)
      .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(icon, null, tint = foreground.value, modifier = Modifier.size(20.dp))
    Spacer(Modifier.height(1.dp))
    Text(label, color = foreground.value, fontSize = 11.sp, lineHeight = 14.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
  }
}

@Composable
private fun FloatingManageCategoriesButton(
  hazeState: HazeState, onClick: () -> Unit, modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme
  FloatingGlassContainer(hazeState, modifier, horizontalInset = AppDimens.drawerInnerGutter) {
    Row(Modifier.fillMaxWidth().height(floatingTabHeight()).padding(4.dp)
      .clip(RoundedCornerShape(24.dp))
      .background(colors.primary.copy(alpha = 0.12f))
      .clickable(role = Role.Button, onClick = onClick),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.Settings, null, modifier = Modifier.size(19.dp), tint = colors.primary)
      Spacer(Modifier.width(7.dp))
      Text("管理分类", color = colors.primary, style = MaterialTheme.typography.labelLarge)
    }
  }
}

@Composable
fun CategoryDrawer(
  vm: MainViewModel,
  selectedCategoryId: Int,
  onSelect: (Int) -> Unit,
  onClose: () -> Unit,
  isOpen: Boolean,
  progress: Float,
  modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme
  val drawerHazeState = remember { HazeState() }
  var screen by remember { mutableStateOf("filters") }
  var editingCategoryId by remember { mutableIntStateOf(0) }
  LaunchedEffect(isOpen) { if (!isOpen) screen = "filters" }
  BackHandler(isOpen && screen != "filters") {
    screen = if (screen == "editor") "manage" else "filters"
  }
  ModalDrawerSheet(
    modifier = modifier.width(300.dp).graphicsLayer {
      alpha = 0.78f + 0.22f * progress
      scaleX = 0.98f + 0.02f * progress
      transformOrigin = TransformOrigin(0f, 0.5f)
    },
    drawerContainerColor = colors.surface.copy(alpha = 0.97f),
    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
  ) {
    Column(Modifier.fillMaxHeight().padding(start = AppDimens.drawerOuterGutter,
      end = AppDimens.drawerOuterGutter, top = 24.dp, bottom = 20.dp)) {
      Row(
        modifier = Modifier.padding(bottom = if (screen == "filters") AppDimens.sectionGap else 0.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (screen != "filters") {
          IconButton(onClick = { screen = if (screen == "editor") "manage" else "filters" }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
          }
        }
        Column(
          modifier = Modifier.weight(1f)
            .padding(start = if (screen == "filters") AppDimens.drawerCardInset else 0.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          AnimatedContent(targetState = screen,
            transitionSpec = { fadeIn(tween(170)) togetherWith fadeOut(tween(110)) },
            label = "drawerTitle") { destination ->
            Text(when (destination) {
              "manage" -> "分类管理"
              "editor" -> if (editingCategoryId == 0) "添加分类" else "编辑分类"
              else -> "按分类浏览"
            }, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
          }
          if (screen == "filters") Text(
            "每一类，都有值得记住的日子",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
          )
        }
        IconButton(onClick = onClose) {
          Box(
            Modifier.size(32.dp).background(colors.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Close, "关闭分类抽屉", tint = colors.onSurfaceVariant,
              modifier = Modifier.size(18.dp))
          }
        }
      }
      Box(Modifier.weight(1f)) {
        AnimatedContent(
          targetState = screen,
          modifier = Modifier.fillMaxSize().hazeSource(drawerHazeState),
          transitionSpec = {
            val direction = if (listOf("filters", "manage", "editor").indexOf(targetState) >
              listOf("filters", "manage", "editor").indexOf(initialState)) 1 else -1
            (fadeIn(tween(220, delayMillis = 35)) + slideInHorizontally(
              animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
            ) { direction * it / 5 }) togetherWith
              (fadeOut(tween(150)) + slideOutHorizontally(tween(210)) { -direction * it / 7 })
          },
          label = "drawerPages"
        ) { destination ->
          when (destination) {
            "manage" -> CategoryListScreen(
              vm,
              onAdd = { editingCategoryId = 0; screen = "editor" },
              onEdit = { editingCategoryId = it; screen = "editor" },
              contentPadding = PaddingValues(0.dp)
            )
            "editor" -> CategoryEditorScreen(
              vm, editingCategoryId.takeIf { it != 0 },
              onSaved = { screen = "manage" },
              contentPadding = PaddingValues(0.dp)
            )
            else -> Column(
              Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              NavigationDrawerItem(
                label = { Text("全部日子") }, selected = selectedCategoryId == 0,
                onClick = { onSelect(0) },
                icon = { Icon(Icons.Default.Category, null) },
                badge = { Text("${vm.snapshot?.events.orEmpty().size}") },
                shape = RoundedCornerShape(18.dp)
              )
              vm.snapshot?.categories.orEmpty().forEach { category ->
                NavigationDrawerItem(
                  label = { Text(category.name, maxLines = 1) },
                  selected = selectedCategoryId == category.id,
                  onClick = { onSelect(category.id) },
                  icon = { CategoryIcon(category.icon, category.color, size = 26.dp) },
                  badge = { Text("${vm.snapshot?.events.orEmpty().count { it.categoryId == category.id }}") },
                  shape = RoundedCornerShape(18.dp)
                )
              }
              Spacer(Modifier.height(floatingTabContentClearance()))
            }
          }
        }
        AnimatedContent(targetState = screen == "filters",
          modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
          transitionSpec = {
            (fadeIn(tween(210, delayMillis = 50)) + slideInVertically(
              spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)) { it / 4 }) togetherWith
              (fadeOut(tween(120)) + slideOutVertically(tween(170)) { it / 4 })
          },
          label = "manageButton"
        ) { visible ->
          if (visible) FloatingManageCategoriesButton(drawerHazeState, onClick = { screen = "manage" })
        }
      }
    }
  }
}
