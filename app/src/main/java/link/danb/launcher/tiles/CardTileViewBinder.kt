package link.danb.launcher.tiles

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.ui.ViewBinder
import link.danb.launcher.utils.inflate
import link.danb.launcher.utils.applySize

class CardTileViewBinder(
    private val onClick: ((View, TileData) -> Unit)? = null,
    private val onLongClick: ((View, TileData) -> Unit)? = null,
) : ViewBinder<CardTileViewHolder, TileViewItem> {

    override val viewType: Int = R.id.card_tile_view_type_id

    override fun createViewHolder(parent: ViewGroup): CardTileViewHolder {
        return CardTileViewHolder(parent.inflate(R.layout.card_tile_view))
    }

    override fun bindViewHolder(holder: CardTileViewHolder, viewItem: TileViewItem) {
        holder.cardView.apply {
            isClickable = onClick !== null || onLongClick != null
            setOnClickListener { onClick?.invoke(it, viewItem.data) }
            setOnLongClickListener { onLongClick?.invoke(it, viewItem.data); true }
        }

        holder.textView.apply {
            text = viewItem.name
            viewItem.icon.value.applySize(
                context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
            )
            setCompoundDrawables(viewItem.icon.value, null, null, null)
        }
    }
}

class CardTileViewHolder(view: View) : ViewHolder(view) {
    val cardView = view as CardView
    val textView: TextView = view.findViewById(R.id.text_view)
}
