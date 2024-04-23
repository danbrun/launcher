package link.danb.launcher.tiles

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.extensions.inflate
import link.danb.launcher.ui.RoundedCornerOutlineProvider
import link.danb.launcher.ui.ViewBinder

class TileViewBinder(
  private val onClick: ((View, Any) -> Unit)? = null,
  private val onLongClick: ((View, Any) -> Unit)? = null,
) : ViewBinder<TransparentTileViewHolder, TileViewItem> {

  override val viewType: Int = R.id.tile_view_type_id

  override fun createViewHolder(parent: ViewGroup): TransparentTileViewHolder =
    TransparentTileViewHolder(parent.inflate(R.layout.transparent_tile_view))

  override fun bindViewHolder(holder: TransparentTileViewHolder, viewItem: TileViewItem) {
    holder.itemView.apply {
      setOnClickListener { onClick?.invoke(it, viewItem.data) }
      setOnLongClickListener {
        onLongClick?.invoke(it, viewItem.data)
        true
      }
      clipToOutline = true
      outlineProvider =
        RoundedCornerOutlineProvider(
          resources.getDimensionPixelSize(R.dimen.app_item_corner_radius)
        )
    }

    holder.iconView.setImageDrawable(viewItem.icon)
    holder.textView.text = viewItem.name
  }
}

class TransparentTileViewHolder(view: View) : ViewHolder(view) {
  val iconView: ImageView = view.findViewById(R.id.tile_icon)
  val textView: TextView = view.findViewById(R.id.tile_label)
}
