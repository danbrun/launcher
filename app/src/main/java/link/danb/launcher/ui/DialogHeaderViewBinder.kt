package link.danb.launcher.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.extensions.inflate

class DialogHeaderViewBinder : ViewBinder<DialogHeaderViewHolder, DialogHeaderViewItem> {

  override val viewType: Int = R.id.dialog_header_view_type_id

  override fun createViewHolder(parent: ViewGroup): DialogHeaderViewHolder =
    DialogHeaderViewHolder(parent.inflate(R.layout.dialog_header_view))

  override fun bindViewHolder(holder: DialogHeaderViewHolder, viewItem: DialogHeaderViewItem) {
    holder.textView.apply {
      text = viewItem.label
      setCompoundDrawablesRelativeWithIntrinsicBounds(
        viewItem.icon,
        ResourcesCompat.ID_NULL,
        ResourcesCompat.ID_NULL,
        ResourcesCompat.ID_NULL
      )
    }
  }
}

class DialogHeaderViewHolder(view: View) : ViewHolder(view) {
  val textView = itemView as TextView
}

class DialogHeaderViewItem(
  val label: String,
  @DrawableRes val icon: Int = ResourcesCompat.ID_NULL,
) : ViewItem {

  override val viewType: Int = R.id.dialog_header_view_type_id

  override fun areItemsTheSame(other: ViewItem): Boolean =
    other is DialogHeaderViewItem && label == other.label

  override fun areContentsTheSame(other: ViewItem): Boolean = true
}
