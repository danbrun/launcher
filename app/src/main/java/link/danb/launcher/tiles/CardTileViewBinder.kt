package link.danb.launcher.tiles

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.extensions.inflate
import link.danb.launcher.extensions.setSize
import link.danb.launcher.ui.ViewBinder

class CardTileViewBinder(
  private val onClick: ((View, Any) -> Unit)? = null,
  private val onLongClick: ((View, Any) -> Unit)? = null,
) : ViewBinder<CardTileViewHolder, TileViewItem> {

  override val viewType: Int = R.id.card_tile_view_type_id

  override fun createViewHolder(parent: ViewGroup): CardTileViewHolder =
    CardTileViewHolder(parent.inflate(R.layout.card_tile_view))

  override fun bindViewHolder(holder: CardTileViewHolder, viewItem: TileViewItem) {
    holder.cardView.apply {
      isClickable = onClick !== null || onLongClick != null
      setOnClickListener { onClick?.invoke(it, viewItem.data) }
      setOnLongClickListener {
        onLongClick?.invoke(it, viewItem.data)
        true
      }
    }

    holder.textView.apply {
      text = viewItem.name
      viewItem.icon.setSize(context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size))
      setCompoundDrawables(viewItem.icon, null, null, null)
    }
  }
}

class CardTileViewHolder(view: View) : ViewHolder(view) {
  val cardView = view as CardView
  val textView: TextView = view.findViewById(R.id.text_view)
}
