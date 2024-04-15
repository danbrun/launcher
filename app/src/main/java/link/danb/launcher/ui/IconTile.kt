package link.danb.launcher.ui

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import link.danb.launcher.R

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun IconTile(icon: Drawable, name: String, onClick: (View) -> Unit, onLongClick: (View) -> Unit) {
  var imageView: ImageView? by remember { mutableStateOf(null) }
  Card(
    Modifier.padding(4.dp)
      .combinedClickable(
        onClick = { onClick(checkNotNull(imageView)) },
        onLongClick = { onLongClick(checkNotNull(imageView)) },
      )
  ) {
    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
      AndroidView(
        factory = { ImageView(it).apply { imageView = this } },
        update = { it.setImageDrawable(icon) },
        onReset = { it.setImageDrawable(null) },
        modifier = Modifier.size(dimensionResource(R.dimen.launcher_icon_size)),
      )
      Text(
        name,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 8.dp),
      )
    }
  }
}
