package link.danb.launcher.ui

import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import link.danb.launcher.widgets.WidgetEditor
import link.danb.launcher.widgets.WidgetFrameView

@Composable
fun Widget(
  widgetData: WidgetData,
  sizeRange: IntRange,
  isConfigurable: Boolean,
  modifier: Modifier = Modifier,
  widgetEditor: WidgetEditor,
  configure: (View) -> Unit,
) {
  var height by remember { mutableIntStateOf(widgetData.height) }
  var isEditing by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val hapticFeedback = LocalHapticFeedback.current

  val widgetFrameView = remember(context) { WidgetFrameView(context) }
  val draggableState = rememberDraggableState { height = (height + it.toInt()).coerceIn(sizeRange) }

  Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    key(widgetFrameView) {
      AndroidView(
        factory = { widgetFrameView },
        modifier =
          Modifier.fillMaxWidth().height(with(LocalDensity.current) { height.toDp() }).pointerInput(
            widgetFrameView
          ) {
            detectLongPress(PointerEventPass.Initial) {
              hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
              isEditing = true
            }
          },
        onReset = {},
        onRelease = { it.clearAppWidget() },
        update = { it.setAppWidget(widgetData.widgetId) },
      )
    }

    AnimatedVisibility(visible = isEditing) {
      Row {
        IconButtonGroup {
          IconButton({ widgetEditor.moveDown(widgetData.widgetId) }) {
            Icon(
              painterResource(R.drawable.baseline_arrow_downward_24),
              stringResource(R.string.move_down),
            )
          }
          Icon(
            painterResource(R.drawable.baseline_drag_handle_24),
            stringResource(R.string.adjust_height),
            modifier =
              Modifier.draggable(
                  draggableState,
                  Orientation.Vertical,
                  onDragStopped = { widgetEditor.setHeight(widgetData.widgetId, height) },
                )
                .padding(4.dp),
          )
          IconButton({ widgetEditor.moveUp(widgetData.widgetId) }) {
            Icon(
              painterResource(R.drawable.baseline_arrow_upward_24),
              stringResource(R.string.move_up),
            )
          }
        }

        IconButtonGroup {
          if (isConfigurable) {
            IconButton({ configure(widgetFrameView) }) {
              Icon(
                painterResource(R.drawable.ic_baseline_settings_24),
                stringResource(R.string.configure_widget),
              )
            }
          }
          IconButton({ widgetEditor.delete(widgetData.widgetId) }) {
            Icon(
              painterResource(R.drawable.ic_baseline_delete_forever_24),
              stringResource(R.string.remove),
            )
          }
        }

        IconButtonGroup {
          IconButton({ isEditing = false }) {
            Icon(painterResource(R.drawable.baseline_check_24), stringResource(R.string.done))
          }
        }
      }
    }
  }
}

private fun View.getListViewContaining(x: Int, y: Int, depth: Int): ListView? =
  when (this) {
    is ListView -> this.takeIf { boundsOnScreen.contains(x, y) }
    is ViewGroup -> children.firstNotNullOfOrNull { it.getListViewContaining(x, y, depth + 1) }
    else -> null
  }
