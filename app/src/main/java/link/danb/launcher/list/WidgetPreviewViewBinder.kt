package link.danb.launcher.list

import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.utils.inflate

class WidgetPreviewViewBinder(private val widgetPreviewListener: WidgetPreviewListener? = null) :
    ViewBinder {
    override val viewType: Int = R.id.widget_preview_view_type_id

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return WidgetPreviewViewHolder(parent.inflate(R.layout.widget_preview_view))
    }

    override fun bindViewHolder(holder: ViewHolder, viewItem: ViewItem) {
        holder as WidgetPreviewViewHolder
        viewItem as WidgetPreviewViewItem

        holder.itemView.apply {
            isClickable = widgetPreviewListener != null
            setOnClickListener { widgetPreviewListener?.onClick(it, viewItem) }
        }

        holder.image.apply {
            setImageDrawable(
                viewItem.appWidgetProviderInfo.loadPreviewImage(context, 0)
                    ?: viewItem.appWidgetProviderInfo.loadIcon(holder.image.context, 0)
            )
        }

        holder.label.apply {
            text = viewItem.appWidgetProviderInfo.loadLabel(context.packageManager)
        }

        holder.description.visibility = View.GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val description =
                viewItem.appWidgetProviderInfo.loadDescription(holder.description.context)

            if (description != null) {
                holder.description.apply {
                    text = description
                    visibility = View.VISIBLE
                }
            }
        }
    }

    private class WidgetPreviewViewHolder(view: View) : ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.widget_preview)
        val label: TextView = view.findViewById(R.id.widget_label)
        val description: TextView = view.findViewById(R.id.widget_description)
    }
}

class WidgetPreviewViewItem(val appWidgetProviderInfo: AppWidgetProviderInfo) : ViewItem {
    override val viewType: Int = R.id.widget_preview_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean {
        return other is WidgetPreviewViewItem
                && appWidgetProviderInfo == other.appWidgetProviderInfo
    }

    override fun areContentsTheSame(other: ViewItem): Boolean {
        return other is WidgetPreviewViewItem
                && appWidgetProviderInfo == other.appWidgetProviderInfo
    }
}

fun interface WidgetPreviewListener {
    fun onClick(view: View, widgetPreviewViewItem: WidgetPreviewViewItem)
}
