package link.danb.launcher.list

import android.annotation.SuppressLint
import android.appwidget.AppWidgetProviderInfo
import android.content.res.Resources
import android.os.Build
import android.os.UserHandle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.utils.inflate
import link.danb.launcher.widgets.AppWidgetViewProvider

class WidgetPreviewViewBinder(
    private val appWidgetViewProvider: AppWidgetViewProvider,
    private val widgetPreviewListener: WidgetPreviewListener? = null
) : ViewBinder {
    override val viewType: Int = R.id.widget_preview_view_type_id

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return WidgetPreviewViewHolder(parent.inflate(R.layout.widget_preview_view))
    }

    @SuppressLint("ResourceType")
    override fun bindViewHolder(holder: ViewHolder, viewItem: ViewItem) {
        holder as WidgetPreviewViewHolder
        viewItem as WidgetPreviewViewItem

        holder.itemView.apply {
            isClickable = widgetPreviewListener != null
            setOnClickListener { widgetPreviewListener?.onClick(it, viewItem) }
        }

        holder.frame.removeAllViews()
        holder.frame.setOnClickListener { widgetPreviewListener?.onClick(it, viewItem) }
        holder.image.setImageDrawable(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && viewItem.providerInfo.previewLayout != Resources.ID_NULL) {
            holder.frame.addView(appWidgetViewProvider.createPreview(viewItem.providerInfo))
        } else {
            holder.image.setImageDrawable(
                viewItem.providerInfo.loadPreviewImage(holder.image.context, 0)
                    ?: viewItem.providerInfo.loadIcon(holder.image.context, 0)
            )
        }

        holder.label.apply {
            text = viewItem.providerInfo.loadLabel(context.packageManager)
        }

        holder.description.visibility = View.GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val description = viewItem.providerInfo.loadDescription(holder.description.context)

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
        val frame: FrameLayout = view.findViewById(R.id.widget_preview_frame)
        val label: TextView = view.findViewById(R.id.widget_label)
        val description: TextView = view.findViewById(R.id.widget_description)
    }
}

class WidgetPreviewViewItem(val providerInfo: AppWidgetProviderInfo, val userHandle: UserHandle) :
    ViewItem {
    override val viewType: Int = R.id.widget_preview_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean {
        return other is WidgetPreviewViewItem && providerInfo == other.providerInfo && userHandle == other.userHandle
    }

    override fun areContentsTheSame(other: ViewItem): Boolean {
        return true
    }
}

fun interface WidgetPreviewListener {
    fun onClick(view: View, widgetPreviewViewItem: WidgetPreviewViewItem)
}
