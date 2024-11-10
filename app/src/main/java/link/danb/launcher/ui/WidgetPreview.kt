package link.danb.launcher.ui

import android.appwidget.AppWidgetProviderInfo
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import link.danb.launcher.widgets.WidgetFrameView

data class WidgetPreviewData(
  val providerInfo: AppWidgetProviderInfo,
  val previewImage: Drawable,
  val label: String,
  val description: String?,
)

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun WidgetPreview(item: WidgetPreviewData, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Card(modifier.padding(4.dp).clip(CardDefaults.shape).combinedClickable(onClick = onClick)) {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        item.providerInfo.previewLayout != Resources.ID_NULL
    ) {
      Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AndroidView(
          factory = { WidgetFrameView(it) },
          update = { it.setAppWidgetPreview(item.providerInfo) },
          onReset = { it.clearAppWidget() },
          onRelease = { it.clearAppWidget() },
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
