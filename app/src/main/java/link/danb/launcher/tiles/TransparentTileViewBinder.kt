package link.danb.launcher.tiles

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.ui.ViewBinder
import link.danb.launcher.ui.RoundedCornerOutlineProvider
import link.danb.launcher.utils.inflate
import link.danb.launcher.utils.applySize

class TransparentTileViewBinder(
    private val onClick: ((View, TileData) -> Unit)? = null,
    private val onLongClick: ((View, TileData) -> Unit)? = null,
) : ViewBinder<TransparentTileViewHolder, TileViewItem> {

    override val viewType: Int = R.id.transparent_tile_view_type_id

    override fun createViewHolder(parent: ViewGroup): TransparentTileViewHolder {
        return TransparentTileViewHolder(parent.inflate(R.layout.transparent_tile_view))
    }

    override fun bindViewHolder(
        holder: TransparentTileViewHolder, viewItem: TileViewItem
    ) {
        holder.textView.apply {
            text = viewItem.name
            viewItem.icon.value.applySize(
                context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
            )
            setCompoundDrawables(viewItem.icon.value, null, null, null)
            setOnClickListener { onClick?.invoke(it, viewItem.data) }
            setOnLongClickListener { onLongClick?.invoke(it, viewItem.data); true }
            clipToOutline = true
            outlineProvider =
                RoundedCornerOutlineProvider(resources.getDimensionPixelSize(R.dimen.app_item_corner_radius))
        }
    }
}

class TransparentTileViewHolder(view: View) : ViewHolder(view) {
    val textView: TextView = view as TextView
}
