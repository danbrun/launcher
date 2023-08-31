package link.danb.launcher.ui

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.extensions.inflate

class LoadingSpinnerViewBinder : ViewBinder<LoadingSpinnerViewHolder, LoadingSpinnerViewItem> {

  override val viewType: Int = R.id.loading_spinner_view_type_id

  override fun createViewHolder(parent: ViewGroup): LoadingSpinnerViewHolder =
    LoadingSpinnerViewHolder(parent.inflate(R.layout.loading_spinner_view))

  override fun bindViewHolder(holder: LoadingSpinnerViewHolder, viewItem: LoadingSpinnerViewItem) =
    Unit
}

class LoadingSpinnerViewHolder(itemView: View) : ViewHolder(itemView)

object LoadingSpinnerViewItem : ViewItem {

  override val viewType: Int = R.id.loading_spinner_view_type_id

  override fun areItemsTheSame(other: ViewItem): Boolean = other is LoadingSpinnerViewItem

  override fun areContentsTheSame(other: ViewItem): Boolean = true
}
