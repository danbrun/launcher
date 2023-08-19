package link.danb.launcher.list

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.model.TileViewData
import link.danb.launcher.utils.inflate
import link.danb.launcher.utils.applySize

class CardTileViewBinder(
    private val onClick: ((View, TileViewData) -> Unit)? = null,
    private val onLongClick: ((View, TileViewData) -> Unit)? = null,
) : ViewBinder<CardTileViewHolder, CardTileViewItem> {

    override val viewType: Int = R.id.card_tile_view_type_id

    override fun createViewHolder(parent: ViewGroup): CardTileViewHolder {
        return CardTileViewHolder(parent.inflate(R.layout.card_tile_view))
    }

    override fun bindViewHolder(holder: CardTileViewHolder, viewItem: CardTileViewItem) {
        holder.cardView.apply {
            isClickable = onClick !== null || onLongClick != null
            setOnClickListener { onClick?.invoke(it, viewItem.tileViewData) }
            setOnLongClickListener { onLongClick?.invoke(it, viewItem.tileViewData); true }
        }

        holder.textView.apply {
            text = viewItem.tileViewData.name
            viewItem.tileViewData.icon?.applySize(
                context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
            )
            setCompoundDrawables(viewItem.tileViewData.icon, null, null, null)
        }
    }
}

class CardTileViewHolder(view: View) : ViewHolder(view) {
    val cardView = view as CardView
    val textView: TextView = view.findViewById(R.id.text_view)
}

class CardTileViewItem(val tileViewData: TileViewData) : ViewItem {

    override val viewType: Int = R.id.card_tile_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean =
        other is CardTileViewItem && tileViewData.areItemsTheSame(other.tileViewData)

    override fun areContentsTheSame(other: ViewItem): Boolean =
        other is CardTileViewItem && tileViewData.areContentsTheSame(other.tileViewData)
}
