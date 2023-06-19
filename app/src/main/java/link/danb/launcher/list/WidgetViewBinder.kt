package link.danb.launcher.list

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.model.WidgetMetadata
import link.danb.launcher.utils.inflate
import link.danb.launcher.widgets.AppWidgetViewProvider

class WidgetViewBinder(
    private val appWidgetViewProvider: AppWidgetViewProvider,
    private val widgetViewListener: WidgetViewListener
) : ViewBinder {
    override val viewType: Int = R.id.widget_view_type_id

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return WidgetViewHolder(parent.inflate(R.layout.widget_view))
    }

    override fun bindViewHolder(holder: ViewHolder, viewItem: ViewItem) {
        holder as WidgetViewHolder
        viewItem as WidgetViewItem

        val widgetView = appWidgetViewProvider.createView(viewItem.widgetMetadata.widgetId).apply {
            setOnLongClickListener {
                widgetViewListener.onLongClick(viewItem.widgetMetadata)
                true
            }
        }

        holder.widgetFrame.apply {
            removeAllViews()
            addView(widgetView)

            layoutParams = layoutParams.apply {
                val heightMultiplier = holder.widgetFrame.context.resources.getDimensionPixelSize(
                    R.dimen.widget_height_multiplier
                )
                height = viewItem.widgetMetadata.height * heightMultiplier
            }
        }
    }

    private class WidgetViewHolder(view: View) : ViewHolder(view) {
        val widgetFrame = view as FrameLayout
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
