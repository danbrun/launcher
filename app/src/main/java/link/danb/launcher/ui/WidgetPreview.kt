package link.danb.launcher.ui

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import link.danb.launcher.widgets.AppWidgetViewProvider

data class WidgetPreviewData(
  val providerInfo: AppWidgetProviderInfo,
  val previewImage: Drawable,
  val label: String,
  val description: String?,
)

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun WidgetPreview(item: WidgetPreviewData, onClick: () -> Unit) {
  Card(Modifier.padding(4.dp).combinedClickable(onClick = onClick)) {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        item.providerInfo.previewLayout != Resources.ID_NULL
    ) {
      Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AndroidView(
          factory = { WidgetPreviewFrame(it) },
          update = { it.setAppWidgetProviderInfo(item.providerInfo.clone().apply { initialLayout = previewLayout }) },
          onReset = {},
        )
      }
    } else {
      Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
        AndroidView(
          factory = { ImageView(it) },
          update = { it.setImageDrawable(item.previewImage) },
          onReset = { it.setImageDrawable(null) },
        )
      }
    }

    Text(item.label, Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center)

    if (item.description != null) {
      Text(
        item.description,
        Modifier.fillMaxWidth().padding(8.dp),
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.S)
class WidgetPreviewFrame @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
  FrameLayout(context, attrs) {

  @Inject lateinit var appWidgetViewProvider: AppWidgetViewProvider

  private var appWidgetHostView: AppWidgetHostView? = null

  fun setAppWidgetProviderInfo(appWidgetProviderInfo: AppWidgetProviderInfo) {
    if (appWidgetHostView == null) {
      appWidgetHostView = appWidgetViewProvider.createPreview(appWidgetProviderInfo)
      addView(appWidgetHostView)
    } else {
      appWidgetHostView?.setAppWidget(Resources.ID_NULL, appWidgetProviderInfo)
    }
  }

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    return true
  }
}
