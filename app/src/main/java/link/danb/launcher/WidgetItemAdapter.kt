package link.danb.launcher

import android.annotation.SuppressLint
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class WidgetItemAdapter(val onClick: (AppWidgetProviderInfo) -> Unit) :
    ListAdapter<AppWidgetProviderInfo, WidgetItemAdapter.ViewHolder>(WidgetItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.widget_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.widgetItem.context
        val widgetProviderInfo: AppWidgetProviderInfo = getItem(position)

        holder.launcherIcon.apply {
            text = widgetProviderInfo.loadLabel(context.packageManager)
            setCompoundDrawables(
                LauncherIconDrawable(
                    widgetProviderInfo.loadIcon(context, 0)
                ).apply {
                    val size =
                        context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
                    setBounds(0, 0, size, size)
                }, null, null, null
            )
            background = null
            isClickable = false
            isFocusable = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            holder.description.apply {
                text = widgetProviderInfo.loadDescription(context)
            }
        }

        holder.preview.apply {
            maxWidth = 200
            maxHeight = 200
            setImageDrawable(widgetProviderInfo.loadPreviewImage(context, 0))
        }

        holder.widgetItem.setOnClickListener { onClick(widgetProviderInfo) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val widgetItem: View = itemView
        val launcherIcon: TextView = itemView.findViewById(R.id.launcher_icon)
        val description: TextView = itemView.findViewById(R.id.widget_description)
        val preview: ImageView = itemView.findViewById(R.id.widget_preview)
    }

    class WidgetItemDiffCallback : DiffUtil.ItemCallback<AppWidgetProviderInfo>() {
        override fun areItemsTheSame(
            oldItem: AppWidgetProviderInfo,
            newItem: AppWidgetProviderInfo
        ): Boolean {
            return oldItem == newItem
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(
            oldItem: AppWidgetProviderInfo,
            newItem: AppWidgetProviderInfo
        ): Boolean {
            return oldItem == newItem
        }
    }
}