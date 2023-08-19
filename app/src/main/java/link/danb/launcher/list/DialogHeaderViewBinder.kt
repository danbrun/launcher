package link.danb.launcher.list

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.utils.inflate

class DialogHeaderViewBinder : ViewBinder<DialogHeaderViewHolder, DialogHeaderViewItem> {

    override val viewType: Int = R.id.dialog_header_view_type_id

    override fun createViewHolder(parent: ViewGroup): DialogHeaderViewHolder {
        return DialogHeaderViewHolder(parent.inflate(R.layout.dialog_header_view))
    }

    override fun bindViewHolder(holder: DialogHeaderViewHolder, viewItem: DialogHeaderViewItem) {
        holder.textView.text = viewItem.label
    }
}

class DialogHeaderViewHolder(view: View) : ViewHolder(view) {
    val textView = itemView as TextView
}

class DialogHeaderViewItem(val label: String) : ViewItem {

    override val viewType: Int = R.id.dialog_header_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean =
        other is GroupHeaderViewItem && label == other.label

    override fun areContentsTheSame(other: ViewItem): Boolean = true
}
