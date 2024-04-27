package link.danb.launcher.ui

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ListView
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.children
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import link.danb.launcher.database.WidgetData
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.widgets.AppWidgetViewProvider

@Composable
fun Widget(
  widgetData: WidgetData,
  modifier: Modifier = Modifier,
  setScrollEnabled: (Boolean) -> Unit,
) {
  val height = with(LocalDensity.current) { widgetData.height.toDp() }
  var widgetFrame: WidgetFrame? by remember { mutableStateOf(null) }
  var isScrollEnabled: Boolean by remember { mutableStateOf(true) }
  AndroidView(
    factory = { WidgetFrame(it).apply { widgetFrame = this } },
    modifier =
      modifier.fillMaxWidth().height(height).pointerInput(widgetFrame, isScrollEnabled) {
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
    update = { it.setAppWidget(widgetData.widgetId) },
  )
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
}

private fun View.getListViewContaining(x: Int, y: Int): ListView? =
  when (this) {
    is ListView -> this.takeIf { boundsOnScreen.contains(x, y) }
    is ViewGroup -> children.firstNotNullOfOrNull { it.getListViewContaining(x, y) }
    else -> null
  }
