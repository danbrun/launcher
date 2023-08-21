package link.danb.launcher.widgets

import android.app.Application
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.icons.LauncherIconCache
import link.danb.launcher.ui.ViewBinder
import link.danb.launcher.ui.ViewItem
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
            text = viewItem.name
            updateDrawables(viewItem)

            setOnClickListener {
                onClick.invoke(viewItem.applicationInfo)
                viewItem.isExpanded = !viewItem.isExpanded
                updateDrawables(viewItem)
            }
        }
    }

    private fun TextView.updateDrawables(viewItem: WidgetHeaderViewItem) {
        val appIconSize = context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
        val dropdownIconSize = (resources.displayMetrics.density * 24).toInt()
        val dropdownDrawableRes =
            if (viewItem.isExpanded) R.drawable.baseline_expand_less_24 else R.drawable.baseline_expand_more_24

        setCompoundDrawablesRelative(
            viewItem.icon.value.applySize(appIconSize),
            null,
            ContextCompat.getDrawable(context, dropdownDrawableRes)?.applySize(dropdownIconSize),
            null
        )
    }
}

class WidgetHeaderViewHolder(view: View) : ViewHolder(view) {
    val textView = view as TextView
}

class WidgetHeaderViewItem private constructor(
    val applicationInfo: ApplicationInfo,
    val name: CharSequence,
    val icon: Lazy<Drawable>,
    var isExpanded: Boolean
) : ViewItem {

    override val viewType: Int = R.id.widget_header_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean =
        other is WidgetHeaderViewItem && applicationInfo.packageName == other.applicationInfo.packageName

    override fun areContentsTheSame(other: ViewItem): Boolean =
        other is WidgetHeaderViewItem && name == other.name && icon == other.icon

    class WidgetHeaderViewItemFactory @Inject constructor(
        private val application: Application, private val launcherIconCache: LauncherIconCache
    ) {
        fun create(info: ApplicationInfo, user: UserHandle, isExpanded: Boolean) =
            WidgetHeaderViewItem(
                info,
                info.loadLabel(application.packageManager),
                launcherIconCache.get(info, user),
                isExpanded
            )
    }
}