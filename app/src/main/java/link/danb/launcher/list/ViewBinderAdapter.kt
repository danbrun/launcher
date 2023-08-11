package link.danb.launcher.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class ViewBinderAdapter(private vararg val viewBinders: ViewBinder) :
    ListAdapter<ViewItem, ViewHolder>(diffUtilItemCallback) {

    var onBindViewHolderListener: ((Int) -> Unit)? = null

    private fun getViewBinder(viewType: Int): ViewBinder {
        return viewBinders.firstOrNull { it.viewType == viewType }
            ?: throw IllegalStateException("View binder not installed for view type.")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return getViewBinder(viewType).createViewHolder(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getViewBinder(getItemViewType(position)).bindViewHolder(holder, getItem(position))
        onBindViewHolderListener?.invoke(position)
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).viewType
    }

    companion object {
        val diffUtilItemCallback = object : DiffUtil.ItemCallback<ViewItem>() {
            override fun areItemsTheSame(oldItem: ViewItem, newItem: ViewItem): Boolean {
                return oldItem.areItemsTheSame(newItem)
            }

            override fun areContentsTheSame(oldItem: ViewItem, newItem: ViewItem): Boolean {
                return oldItem.areContentsTheSame(newItem)
            }
        }
    }
}

interface ViewBinder {
    val viewType: Int

    fun createViewHolder(parent: ViewGroup): ViewHolder
    fun bindViewHolder(holder: ViewHolder, viewItem: ViewItem)
}

interface ViewItem {
    val viewType: Int

    fun areItemsTheSame(other: ViewItem): Boolean
    fun areContentsTheSame(other: ViewItem): Boolean
}
