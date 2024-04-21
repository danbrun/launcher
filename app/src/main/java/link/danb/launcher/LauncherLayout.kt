package link.danb.launcher

import android.graphics.Rect
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
fun LauncherList(
  paddingValues: PaddingValues,
  recyclerAdapter: ViewBinderAdapter,
  onRecyclerViewCreated: (RecyclerView) -> Unit,
) {
  val padding =
    with(LocalDensity.current) {
      Rect(
        paddingValues.calculateLeftPadding(LocalLayoutDirection.current).roundToPx(),
        paddingValues.calculateTopPadding().roundToPx(),
        paddingValues.calculateRightPadding(LocalLayoutDirection.current).roundToPx(),
        paddingValues.calculateBottomPadding().roundToPx(),
      )
    }

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
    update = { it.setPadding(padding.left, padding.top, padding.right, padding.bottom) },
    modifier = Modifier.fillMaxSize(),
  )
}
