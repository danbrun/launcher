package link.danb.launcher.list

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.model.TileViewData
import link.danb.launcher.ui.RoundedCornerOutlineProvider
import link.danb.launcher.utils.inflate
import link.danb.launcher.utils.applySize

class TransparentTileViewBinder(
    private val onClick: ((View, TileViewData) -> Unit)?,
    private val onLongClick: ((View, TileViewData) -> Unit)?
) : ViewBinder<TransparentTileViewHolder, TransparentTileViewItem> {

    override val viewType: Int = R.id.transparent_tile_view_type_id

    override fun createViewHolder(parent: ViewGroup): TransparentTileViewHolder {
        return TransparentTileViewHolder(parent.inflate(R.layout.transparent_tile_view))
    }

    override fun bindViewHolder(
        holder: TransparentTileViewHolder, viewItem: TransparentTileViewItem
    ) {
        holder.textView.apply {
            text = viewItem.tileViewData.name
            viewItem.tileViewData.icon?.applySize(
                context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
            )
            setCompoundDrawables(viewItem.tileViewData.icon, null, null, null)
            setOnClickListener { onClick?.invoke(it, viewItem.tileViewData) }
            setOnLongClickListener { onLongClick?.invoke(it, viewItem.tileViewData); true }
            clipToOutline = true
            outlineProvider =
                RoundedCornerOutlineProvider(resources.getDimensionPixelSize(R.dimen.app_item_corner_radius))
        }
    }
}

class TransparentTileViewHolder(view: View) : ViewHolder(view) {
    val textView: TextView = view as TextView
}

class TransparentTileViewItem(val tileViewData: TileViewData) : ViewItem {

    override val viewType: Int = R.id.transparent_tile_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean =
        other is TransparentTileViewItem && tileViewData.areItemsTheSame(other.tileViewData)

    override fun areContentsTheSame(other: ViewItem): Boolean =
        other is TransparentTileViewItem && tileViewData.areContentsTheSame(other.tileViewData)
}
