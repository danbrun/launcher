package link.danb.launcher.ui

import android.view.View
import android.view.ViewGroup
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.children
import link.danb.launcher.R
import link.danb.launcher.database.WidgetData
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.extensions.detectLongPress
import link.danb.launcher.widgets.WidgetFrameView

@Composable
fun Widget(
  widgetData: WidgetData,
  sizeRange: IntRange,
  modifier: Modifier = Modifier,
  setScrollEnabled: (Boolean) -> Unit,
  moveUp: () -> Unit,
  moveDown: () -> Unit,
  remove: () -> Unit,
  setHeight: (Int) -> Unit,
) {
  var height by remember { mutableIntStateOf(widgetData.height) }
  var isEditing by remember { mutableStateOf(false) }
  var widgetFrame: WidgetFrameView? by remember { mutableStateOf(null) }
  var isScrollEnabled: Boolean by remember { mutableStateOf(true) }
  val draggableState = rememberDraggableState { height = (height + it.toInt()).coerceIn(sizeRange) }
  val hapticFeedback = LocalHapticFeedback.current

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    AndroidView(
      factory = { WidgetFrameView(it).apply { widgetFrame = this } },
      modifier =
        modifier
          .fillMaxWidth()
          .height(with(LocalDensity.current) { height.toDp() })
          .pointerInput(widgetFrame) {
            detectLongPress(PointerEventPass.Initial) {
              hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
              isEditing = true
            }
          }
          .pointerInput(widgetFrame, isScrollEnabled) {
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
      onReset = {},
      onRelease = { it.clearAppWidget() },
      update = { it.setAppWidget(widgetData.widgetId) },
    )

    AnimatedVisibility(visible = isEditing) {
      Row {
        TabButtonGroup {
          TabButton(
            painterResource(R.drawable.baseline_check_24),
            stringResource(R.string.done),
            isChecked = false,
            onClick = { isEditing = false },
          )
        }

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
            tint = MaterialTheme.colorScheme.primary,
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

private fun View.getListViewContaining(x: Int, y: Int): ListView? =
  when (this) {
    is ListView -> this.takeIf { boundsOnScreen.contains(x, y) }
    is ViewGroup -> children.firstNotNullOfOrNull { it.getListViewContaining(x, y) }
    else -> null
  }
