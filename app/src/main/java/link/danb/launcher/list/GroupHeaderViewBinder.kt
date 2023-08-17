package link.danb.launcher.list

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.utils.inflate

class GroupHeaderViewBinder : ViewBinder<GroupHeaderViewHolder, GroupHeaderViewItem> {

    override val viewType: Int = R.id.group_header_view_type_id

    override fun createViewHolder(parent: ViewGroup): GroupHeaderViewHolder {
        return GroupHeaderViewHolder(parent.inflate(R.layout.group_header_view))
    }

    override fun bindViewHolder(holder: GroupHeaderViewHolder, viewItem: GroupHeaderViewItem) {
        holder.textView.text = viewItem.label
    }
}

class GroupHeaderViewHolder(view: View) : ViewHolder(view) {
    val textView = itemView as TextView
}

class GroupHeaderViewItem(val label: String) : ViewItem {

    override val viewType: Int = R.id.group_header_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean =
        other is GroupHeaderViewItem && label == other.label

    override fun areContentsTheSame(other: ViewItem): Boolean = true
}
