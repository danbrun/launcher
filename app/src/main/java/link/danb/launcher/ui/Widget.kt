package link.danb.launcher.ui

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ListView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.children
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import link.danb.launcher.R
import link.danb.launcher.TabButton
import link.danb.launcher.TabButtonGroup
import link.danb.launcher.database.WidgetData
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.extensions.updateAppWidgetSize
import link.danb.launcher.widgets.AppWidgetViewProvider

@Composable
fun Widget(
  widgetData: WidgetData,
  sizeRange: IntRange,
  modifier: Modifier = Modifier,
  setScrollEnabled: (Boolean) -> Unit,
  isInEditMode: Boolean,
  moveUp: () -> Unit,
  moveDown: () -> Unit,
  remove: () -> Unit,
  setHeight: (Int) -> Unit,
) {
  var height by remember { mutableIntStateOf(widgetData.height) }
  var widgetFrame: WidgetFrame? by remember { mutableStateOf(null) }
  var isScrollEnabled: Boolean by remember { mutableStateOf(true) }
  val draggableState = rememberDraggableState { height = (height + it.toInt()).coerceIn(sizeRange) }

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    AndroidView(
      factory = { WidgetFrame(it).apply { widgetFrame = this } },
      modifier =
        modifier.fillMaxWidth().height(with(LocalDensity.current) { height.toDp() }).pointerInput(
          widgetFrame,
          isScrollEnabled,
        ) {
          awaitPointerEventScope {
            isScrollEnabled =
              if (isScrollEnabled) {
                val touchPos = awaitFirstDown(false).position
                val list =
                  widgetFrame?.let {
                    val framePos = it.boundsOnScreen
                    it.getListViewContaining(
                      touchPos.x.toInt() + framePos.left,
                      touchPos.y.toInt() + framePos.top,
                    )
                  }
                list == null
              } else {
                waitForUpOrCancellation()
                true
              }
            setScrollEnabled(isScrollEnabled)
          }
        },
      onReset = { it.setAppWidget(null) },
      onRelease = { it.setAppWidget(null) },
      update = {
        it.setAppWidget(widgetData.widgetId)
        it.updateSize()
      },
    )

    AnimatedVisibility(visible = isInEditMode) {
      Row {
        TabButtonGroup {
          TabButton(
            painterResource(R.drawable.baseline_arrow_downward_24),
            stringResource(R.string.move_down),
            isChecked = false,
            onClick = moveDown,
          )
          Icon(
            painterResource(R.drawable.baseline_drag_handle_24),
            stringResource(R.string.adjust_height),
            modifier =
              Modifier.draggable(
                  draggableState,
                  Orientation.Vertical,
                  onDragStopped = { setHeight(height) },
                )
                .padding(4.dp),
          )
          TabButton(
            painterResource(R.drawable.baseline_arrow_upward_24),
            stringResource(R.string.move_up),
            isChecked = false,
            onClick = moveUp,
          )
        }

        TabButtonGroup {
          TabButton(
            painterResource(R.drawable.ic_baseline_delete_forever_24),
            stringResource(R.string.remove),
            isChecked = false,
            onClick = remove,
          )
        }
      }
    }
  }
}

@AndroidEntryPoint
class WidgetFrame @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
  FrameLayout(context, attrs) {

  @Inject lateinit var appWidgetViewProvider: AppWidgetViewProvider

  private var appWidgetHostView: AppWidgetHostView? = null

  init {
    requestDisallowInterceptTouchEvent(true)
  }

  fun setAppWidget(widgetId: Int?) {
    if (widgetId != null) {
      if (appWidgetHostView == null) {
        appWidgetHostView = appWidgetViewProvider.getView(widgetId)
        addView(appWidgetHostView)
      } else {
        with(appWidgetViewProvider) { appWidgetHostView!!.setAppWidget(widgetId) }
      }
    } else if (appWidgetHostView != null) {
      removeView(appWidgetHostView)
      appWidgetHostView = null
    }
  }

  fun updateSize() {
    appWidgetHostView?.updateAppWidgetSize(width, height)
  }
}

private fun View.getListViewContaining(x: Int, y: Int): ListView? =
  when (this) {
    is ListView -> this.takeIf { boundsOnScreen.contains(x, y) }
    is ViewGroup -> children.firstNotNullOfOrNull { it.getListViewContaining(x, y) }
    else -> null
  }
