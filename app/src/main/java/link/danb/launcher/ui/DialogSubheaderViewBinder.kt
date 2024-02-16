package link.danb.launcher.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import link.danb.launcher.R
import link.danb.launcher.extensions.inflate

class DialogSubtitleViewBinder : ViewBinder<DialogSubtitleViewHolder, DialogSubtitleViewItem> {

  override val viewType: Int = R.id.dialog_subtitle_view_type_id

  override fun createViewHolder(parent: ViewGroup): DialogSubtitleViewHolder =
    DialogSubtitleViewHolder(parent.inflate(R.layout.dialog_subtitle_view))

  override fun bindViewHolder(holder: DialogSubtitleViewHolder, viewItem: DialogSubtitleViewItem) {
    holder.textView.text = viewItem.label
  }
}

class DialogSubtitleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
  val textView = itemView as TextView
}

class DialogSubtitleViewItem(val label: String) : ViewItem {

  override val viewType: Int = R.id.dialog_subtitle_view_type_id

  override fun areItemsTheSame(other: ViewItem): Boolean =
    other is DialogSubtitleViewItem && label == other.label

  override fun areContentsTheSame(other: ViewItem): Boolean = true
}
