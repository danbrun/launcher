package link.danb.launcher.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.model.WidgetMetadata
import link.danb.launcher.utils.inflate
import link.danb.launcher.widgets.AppWidgetFrameView

class WidgetViewBinder(private val widgetViewListener: WidgetViewListener) : ViewBinder {
    override val viewType: Int = R.id.widget_view_type_id

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return WidgetViewHolder(parent.inflate(R.layout.widget_view))
    }

    override fun bindViewHolder(holder: ViewHolder, viewItem: ViewItem) {
        holder as WidgetViewHolder
        viewItem as WidgetViewItem

        holder.widgetFrame.apply {
            widgetMetadata = viewItem.widgetMetadata
            setOnLongClickListener {
                widgetViewListener.onLongClick(viewItem.widgetMetadata)
                true
            }
        }
    }

    private class WidgetViewHolder(view: View) : ViewHolder(view) {
        val widgetFrame = view as AppWidgetFrameView
    }
}

class WidgetViewItem(val widgetMetadata: WidgetMetadata) : ViewItem {
    override val viewType: Int = R.id.widget_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean {
        return other is WidgetViewItem && widgetMetadata.widgetId == other.widgetMetadata.widgetId
    }

    override fun areContentsTheSame(other: ViewItem): Boolean {
        return other is WidgetViewItem && widgetMetadata.height == other.widgetMetadata.height
    }
}

fun interface WidgetViewListener {
    fun onLongClick(widgetMetadata: WidgetMetadata)
}
