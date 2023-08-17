package link.danb.launcher.list

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.model.WidgetMetadata
import link.danb.launcher.utils.inflate
import link.danb.launcher.utils.removeFromParent
import link.danb.launcher.utils.setLayoutSize
import link.danb.launcher.utils.updateAppWidgetSize
import link.danb.launcher.widgets.AppWidgetViewProvider

class WidgetViewBinder(
    private val appWidgetViewProvider: AppWidgetViewProvider,
    private val widgetViewListener: WidgetViewListener,
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
                widgetViewListener.onLongClick(viewItem.widgetMetadata)
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

fun interface WidgetViewListener {
    fun onLongClick(widgetMetadata: WidgetMetadata)
}
