package link.danb.launcher.tiles

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import link.danb.launcher.R
import link.danb.launcher.ui.ViewItem

class TileViewItem(
  val data: Any,
  val name: CharSequence,
  val icon: AdaptiveIconDrawable,
  val badge: Drawable,
  val areItemsTheSame: Any.(Any) -> Boolean = { this == it },
) : ViewItem {

  override val viewType: Int = R.id.tile_view_type_id

  override fun areItemsTheSame(other: ViewItem): Boolean =
    other is TileViewItem && areItemsTheSame(data, other.data)

  override fun areContentsTheSame(other: ViewItem): Boolean =
    other is TileViewItem && name == other.name && icon == other.icon
}
