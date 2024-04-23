package link.danb.launcher.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class ViewBinderAdapter(private vararg val viewBinders: ViewBinder<*, *>) :
  ListAdapter<ViewItem, ViewHolder>(diffUtilItemCallback) {

  private fun getViewBinder(viewType: Int): ViewBinder<*, *> =
    viewBinders.firstOrNull { it.viewType == viewType }
      ?: throw IllegalStateException("View binder not installed for view type.")

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    getViewBinder(viewType).createViewHolder(parent)

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    getViewBinder(getItemViewType(position)).bindViewHolder(holder, getItem(position))
  }

  override fun getItemViewType(position: Int): Int = getItem(position).viewType

  companion object {
    val diffUtilItemCallback =
      object : DiffUtil.ItemCallback<ViewItem>() {
        override fun areItemsTheSame(oldItem: ViewItem, newItem: ViewItem): Boolean =
          oldItem.areItemsTheSame(newItem)

        override fun areContentsTheSame(oldItem: ViewItem, newItem: ViewItem): Boolean =
          oldItem.areContentsTheSame(newItem)
      }
  }
}

interface ViewBinder<ViewHolderType : ViewHolder, ViewItemType : ViewItem> {
  val viewType: Int

  fun createViewHolder(parent: ViewGroup): ViewHolderType
  fun bindViewHolder(holder: @UnsafeVariance ViewHolderType, viewItem: @UnsafeVariance ViewItemType)
}

interface ViewItem {
  val viewType: Int

  fun areItemsTheSame(other: ViewItem): Boolean
  fun areContentsTheSame(other: ViewItem): Boolean
}
