package link.danb.launcher.list

import android.graphics.drawable.Drawable
import link.danb.launcher.R
import link.danb.launcher.model.TileData

class TileViewItem private constructor(
    override val viewType: Int,
    val data: TileData,
    val name: CharSequence,
    val icon: Lazy<Drawable>,
) : ViewItem {

    override fun areItemsTheSame(other: ViewItem): Boolean =
        other is TileViewItem && data.areItemsTheSame(other.data)

    override fun areContentsTheSame(other: ViewItem): Boolean =
        other is TileViewItem && name == other.name && icon == other.icon

    companion object {
        fun cardTileViewItem(
            data: TileData, name: CharSequence, icon: Lazy<Drawable>
        ) = TileViewItem(R.id.card_tile_view_type_id, data, name, icon)

        fun transparentTileViewItem(
            data: TileData, name: CharSequence, icon: Lazy<Drawable>
        ) = TileViewItem(R.id.transparent_tile_view_type_id, data, name, icon)
    }
}
