package link.danb.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import link.danb.launcher.extensions.setSpanSizeProvider
import link.danb.launcher.ui.DynamicGridLayoutManager
import link.danb.launcher.ui.GroupHeaderViewItem
import link.danb.launcher.ui.NestedScrollingRecyclerView
import link.danb.launcher.ui.ViewBinderAdapter
import link.danb.launcher.widgets.WidgetEditorViewItem
import link.danb.launcher.widgets.WidgetViewItem

@Composable
fun LauncherLayout(
  launcherList: @Composable (inset: WindowInsets) -> Unit,
  bottomBar: @Composable () -> Unit,
) {
  SubcomposeLayout { constraints ->
    val bottomBarHeight =
      subcompose(0) { bottomBar() }
        .first()
        .measure(Constraints.fixedWidth(constraints.maxWidth))
        .height

    layout(constraints.maxWidth, constraints.maxHeight) {
      subcompose(1) {
          Box(modifier = Modifier.fillMaxSize()) {
            Column {
              launcherList(
                WindowInsets.safeDrawing.add(WindowInsets(0.dp, 0.dp, 0.dp, bottomBarHeight.toDp()))
              )
            }

            Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
              Column(modifier = Modifier.align(Alignment.BottomCenter)) { bottomBar() }
            }
          }
        }
        .map { it.measure(constraints) }
        .forEach { it.placeRelative(0, 0) }
    }
  }
}

@Composable
fun LauncherList(
  windowInsets: WindowInsets,
  recyclerAdapter: ViewBinderAdapter,
  onRecyclerViewCreated: (RecyclerView) -> Unit,
) {
  val density = LocalDensity.current
  val direction = LocalLayoutDirection.current

  val left = windowInsets.getLeft(density, direction)
  val top = windowInsets.getTop(density)
  val right = windowInsets.getRight(density, direction)
  val bottom = windowInsets.getBottom(density)

  AndroidView(
    factory = { context ->
      NestedScrollingRecyclerView(context)
        .apply {
          clipToPadding = false
          adapter = recyclerAdapter
          layoutManager =
            DynamicGridLayoutManager(context, R.dimen.min_column_width).apply {
              setSpanSizeProvider { position, spanCount ->
                when (recyclerAdapter.currentList[position]) {
                  is WidgetViewItem,
                  is WidgetEditorViewItem,
                  is GroupHeaderViewItem -> spanCount
                  else -> 1
                }
              }
            }
        }
        .also { onRecyclerViewCreated(it) }
    },
    update = { it.setPadding(left, top, right, bottom) },
    modifier = Modifier.fillMaxSize(),
  )
}

@Composable
fun BottomBar(
  topContent: @Composable ColumnScope.() -> Unit,
  tabButtonGroups: @Composable RowScope.() -> Unit,
  floatingActionButton: @Composable () -> Unit,
) {
  Column(modifier = Modifier.padding(8.dp)) {
    topContent()

    Spacer(modifier = Modifier.height(8.dp))

    Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
      tabButtonGroups()

      Spacer(modifier = Modifier.width(8.dp))

      floatingActionButton()
    }
  }
}

@Composable
fun TabButtonGroup(iconButtons: @Composable () -> Unit) {
  Card(shape = RoundedCornerShape(28.dp)) {
    Row(modifier = Modifier.padding(4.dp)) { iconButtons() }
  }
}

@Composable
fun TabButton(icon: Painter, name: String, isChecked: Boolean, onClick: () -> Unit) {
  FilledIconToggleButton(checked = isChecked, onCheckedChange = { _ -> onClick() }) {
    Icon(painter = icon, contentDescription = name)
  }
}
