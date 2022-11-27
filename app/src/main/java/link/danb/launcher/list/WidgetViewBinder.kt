package link.danb.launcher.list

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.LauncherFragment.Companion.pixelsToDips
import link.danb.launcher.LauncherFragment.Companion.updateAppWidgetSize
import link.danb.launcher.R
import link.danb.launcher.utils.inflate
import link.danb.launcher.widgets.WidgetViewModel

class WidgetViewBinder(fragment: Fragment) : ViewBinder {
    override val viewType: Int = R.id.widget_view_type_id

    private val widgetViewModel: WidgetViewModel by fragment.activityViewModels()

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return WidgetViewHolder(parent.inflate(R.layout.widget_view))
    }

    override fun bindViewHolder(holder: ViewHolder, viewItem: ViewItem) {
        holder as WidgetViewHolder
        viewItem as WidgetViewItem

        val widgetView = widgetViewModel.getView(viewItem.widgetId).apply {
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                updateAppWidgetSize(
                    holder.widgetFrame.measuredWidth,
                    context.pixelsToDips(
                        resources.getDimensionPixelSize(R.dimen.widget_max_height)
                    )
                )
            }

            setOnLongClickListener {
                widgetViewModel.deleteWidgetId(viewItem.widgetId)
                true
            }
        }

        holder.widgetFrame.removeAllViews()
        holder.widgetFrame.addView(widgetView)
    }

    private class WidgetViewHolder(view: View) : ViewHolder(view) {
        val widgetFrame = view as FrameLayout
    }
}

class WidgetViewItem(val widgetId: Int) : ViewItem {
    override val viewType: Int = R.id.widget_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean {
        return other is WidgetViewItem && widgetId == other.widgetId
    }

    override fun areContentsTheSame(other: ViewItem): Boolean {
        return true
    }
}
