package link.danb.launcher.widgets

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
import link.danb.launcher.extensions.inflate
import link.danb.launcher.ui.ViewBinder
import link.danb.launcher.ui.ViewItem

class WidgetPreviewViewBinder(
  private val appWidgetViewProvider: AppWidgetViewProvider,
  private val onClick: ((View, WidgetPreviewViewItem) -> Unit)? = null,
) : ViewBinder<WidgetPreviewViewHolder, WidgetPreviewViewItem> {

  override val viewType: Int = R.id.widget_preview_view_type_id

  override fun createViewHolder(parent: ViewGroup): WidgetPreviewViewHolder =
    WidgetPreviewViewHolder(parent.inflate(R.layout.widget_preview_view))

  override fun bindViewHolder(holder: WidgetPreviewViewHolder, viewItem: WidgetPreviewViewItem) {
    holder.itemView.apply {
      isClickable = onClick != null
      setOnClickListener { onClick?.invoke(it, viewItem) }
    }

    holder.frame.removeAllViews()
    holder.frame.setOnClickListener { onClick?.invoke(it, viewItem) }
    holder.image.setImageDrawable(null)
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        viewItem.providerInfo.previewLayout != Resources.ID_NULL
    ) {
      holder.frame.visibility = View.VISIBLE
      holder.image.visibility = View.GONE
      holder.frame.addView(appWidgetViewProvider.createPreview(viewItem.providerInfo))
    } else {
      holder.frame.visibility = View.GONE
      holder.image.visibility = View.VISIBLE
      holder.image.setImageDrawable(
        viewItem.providerInfo.loadPreviewImage(holder.image.context, 0)
          ?: viewItem.providerInfo.loadIcon(holder.image.context, 0)
      )
    }

    holder.label.apply { text = viewItem.providerInfo.loadLabel(context.packageManager) }

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
}

class WidgetPreviewViewHolder(view: View) : ViewHolder(view) {
  val image: ImageView = view.findViewById(R.id.widget_preview)
  val frame: FrameLayout = view.findViewById(R.id.widget_preview_frame)
  val label: TextView = view.findViewById(R.id.widget_label)
  val description: TextView = view.findViewById(R.id.widget_description)
}

class WidgetPreviewViewItem(val providerInfo: AppWidgetProviderInfo, val userHandle: UserHandle) :
  ViewItem {

  override val viewType: Int = R.id.widget_preview_view_type_id

  override fun areItemsTheSame(other: ViewItem): Boolean =
    other is WidgetPreviewViewItem &&
      providerInfo.provider == other.providerInfo.provider &&
      userHandle == other.userHandle

  override fun areContentsTheSame(other: ViewItem): Boolean = true
}
