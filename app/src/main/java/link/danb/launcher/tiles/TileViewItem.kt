package link.danb.launcher.tiles

import android.graphics.drawable.Drawable
import link.danb.launcher.R
import link.danb.launcher.ui.ViewItem

class TileViewItem(
  val style: Style,
  val data: Any,
  val name: CharSequence,
  val icon: Drawable,
  val areItemsTheSame: Any.(Any) -> Boolean = { this == it }
) : ViewItem {

  override val viewType: Int = style.viewTypeId

  override fun areItemsTheSame(other: ViewItem): Boolean =
    other is TileViewItem && areItemsTheSame(data, other.data)

  override fun areContentsTheSame(other: ViewItem): Boolean =
    other is TileViewItem && name == other.name && icon == other.icon

  enum class Style(val viewTypeId: Int) {
    CARD(R.id.card_tile_view_type_id),
    TRANSPARENT(R.id.transparent_tile_view_type_id)
  }
}
