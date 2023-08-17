package link.danb.launcher.list

import android.app.Application
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.ui.LauncherIconDrawable
import link.danb.launcher.utils.inflate
import link.danb.launcher.utils.applySize
import javax.inject.Inject

class WidgetHeaderViewBinder(private val onClick: (ApplicationInfo) -> Unit) :
    ViewBinder<WidgetHeaderViewHolder, WidgetHeaderViewItem> {

    override val viewType: Int = R.id.widget_header_view_type_id

    override fun createViewHolder(parent: ViewGroup): WidgetHeaderViewHolder {
        return WidgetHeaderViewHolder(parent.inflate(R.layout.widget_header_view))
    }

    override fun bindViewHolder(holder: WidgetHeaderViewHolder, viewItem: WidgetHeaderViewItem) {
        holder.textView.apply {
            val appIconSize = context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
            val dropdownIconSize = (resources.displayMetrics.density * 24).toInt()
            val dropdownDrawableRes =
                if (viewItem.isExpanded) R.drawable.baseline_expand_less_24 else R.drawable.baseline_expand_more_24

            text = viewItem.name
            setCompoundDrawablesRelative(
                viewItem.icon.applySize(appIconSize),
                null,
                ContextCompat.getDrawable(context, dropdownDrawableRes)
                    ?.applySize(dropdownIconSize),
                null
            )

            setOnClickListener { onClick.invoke(viewItem.applicationInfo) }
        }
    }
}

class WidgetHeaderViewHolder(view: View) : ViewHolder(view) {
    val textView = view as TextView
}

class WidgetHeaderViewItem private constructor(
    val applicationInfo: ApplicationInfo,
    val name: CharSequence,
    val icon: Drawable,
    val isExpanded: Boolean
) : ViewItem {

    override val viewType: Int = R.id.widget_header_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean =
        other is WidgetHeaderViewItem && applicationInfo.packageName == other.applicationInfo.packageName

    override fun areContentsTheSame(other: ViewItem): Boolean =
        other is WidgetHeaderViewItem && isExpanded == other.isExpanded

    class WidgetHeaderViewItemFactory @Inject constructor(private val application: Application) {
        fun create(applicationInfo: ApplicationInfo, isExpanded: Boolean) = WidgetHeaderViewItem(
            applicationInfo,
            applicationInfo.loadLabel(application.packageManager),
            LauncherIconDrawable(applicationInfo.loadIcon(application.packageManager)),
            isExpanded
        )
    }
}
