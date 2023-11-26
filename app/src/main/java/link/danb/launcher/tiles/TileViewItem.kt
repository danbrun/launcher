package link.danb.launcher.tiles

import android.graphics.drawable.Drawable
import link.danb.launcher.ui.ViewItem

class TileViewItem(
  override val viewType: Int,
  val data: TileData,
  val name: CharSequence,
  val icon: Drawable,
) : ViewItem {

  override fun areItemsTheSame(other: ViewItem): Boolean =
    other is TileViewItem && data.areItemsTheSame(other.data)

  override fun areContentsTheSame(other: ViewItem): Boolean =
    other is TileViewItem && name == other.name && icon == other.icon
}
