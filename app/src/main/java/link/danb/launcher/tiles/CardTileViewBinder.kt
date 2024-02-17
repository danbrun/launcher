package link.danb.launcher.tiles

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.extensions.inflate
import link.danb.launcher.ui.ViewBinder

class CardTileViewBinder(
  private val onClick: ((CardTileViewHolder, Any) -> Unit)? = null,
  private val onLongClick: ((CardTileViewHolder, Any) -> Unit)? = null,
) : ViewBinder<CardTileViewHolder, TileViewItem> {

  override val viewType: Int = R.id.card_tile_view_type_id

  override fun createViewHolder(parent: ViewGroup): CardTileViewHolder =
    CardTileViewHolder(parent.inflate(R.layout.card_tile_view))

  override fun bindViewHolder(holder: CardTileViewHolder, viewItem: TileViewItem) {
    holder.cardView.apply {
      isClickable = onClick !== null || onLongClick != null
      setOnClickListener { onClick?.invoke(holder, viewItem.data) }
      setOnLongClickListener {
        onLongClick?.invoke(holder, viewItem.data)
        true
      }
    }

    holder.iconView.setImageDrawable(viewItem.icon)
    holder.textView.text = viewItem.name
  }
}

class CardTileViewHolder(view: View) : ViewHolder(view) {
  val cardView = view as CardView
  val iconView: ImageView = view.findViewById(R.id.tile_icon)
  val textView: TextView = view.findViewById(R.id.tile_label)
}
