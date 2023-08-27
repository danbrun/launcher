package link.danb.launcher.widgets

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.database.WidgetMetadata
import link.danb.launcher.ui.ViewBinder
import link.danb.launcher.ui.ViewItem
import link.danb.launcher.extensions.inflate
import link.danb.launcher.extensions.removeFromParent
import link.danb.launcher.extensions.setLayoutSize
import link.danb.launcher.extensions.updateAppWidgetSize

class WidgetViewBinder(
    private val appWidgetViewProvider: AppWidgetViewProvider,
    private val onLongClick: ((WidgetMetadata) -> Unit)?,
) : ViewBinder<WidgetViewHolder, WidgetViewItem> {

    override val viewType: Int = R.id.widget_view_type_id

    override fun createViewHolder(parent: ViewGroup): WidgetViewHolder {
        return WidgetViewHolder(parent.inflate(R.layout.widget_view))
    }

    override fun bindViewHolder(holder: WidgetViewHolder, viewItem: WidgetViewItem) {
        appWidgetViewProvider.getView(viewItem.widgetMetadata.widgetId).apply {
            removeFromParent()
            updateAppWidgetSize(
                Resources.getSystem().displayMetrics.widthPixels, viewItem.widgetMetadata.height
            )
            setOnLongClickListener {
                onLongClick?.invoke(viewItem.widgetMetadata)
                true
            }
            holder.widgetFrame.run {
                removeAllViews()
                addView(this@apply)
            }
            setLayoutSize(height = viewItem.widgetMetadata.height)
        }
    }
}

class WidgetViewHolder(view: View) : ViewHolder(view) {
    val widgetFrame = view as FrameLayout
}

class WidgetViewItem(val widgetMetadata: WidgetMetadata) : ViewItem {

    override val viewType: Int = R.id.widget_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean =
        other is WidgetViewItem && widgetMetadata.widgetId == other.widgetMetadata.widgetId

    override fun areContentsTheSame(other: ViewItem): Boolean =
        other is WidgetViewItem && widgetMetadata == other.widgetMetadata
}
