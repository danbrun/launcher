package link.danb.launcher.list

import android.app.Application
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.utils.inflate
import link.danb.launcher.utils.setSize

class ApplicationHeaderViewBinder : ViewBinder {
    override val viewType: Int = R.id.application_header_view_type_id

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return ApplicationHeaderViewHolder(parent.inflate(R.layout.application_header_view))
    }

    override fun bindViewHolder(holder: ViewHolder, viewItem: ViewItem) {
        holder as ApplicationHeaderViewHolder
        viewItem as ApplicationHeaderViewItem

        holder.textView.apply {
            text = viewItem.name
            viewItem.icon.setSize(
                context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
            )
            setCompoundDrawables(viewItem.icon, null, null, null)
        }
    }

    private class ApplicationHeaderViewHolder(view: View) : ViewHolder(view) {
        val textView = view as TextView
    }
}

class ApplicationHeaderViewItem(
    private val application: Application,
    private val applicationInfo: ApplicationInfo
) : ViewItem {
    override val viewType: Int = R.id.application_header_view_type_id

    val name: CharSequence by lazy {
        applicationInfo.loadLabel(application.packageManager)
    }

    val icon: Drawable by lazy {
        applicationInfo.loadIcon(application.packageManager)
    }

    override fun areItemsTheSame(other: ViewItem): Boolean {
        return other is ApplicationHeaderViewItem
                && applicationInfo == other.applicationInfo
    }

    override fun areContentsTheSame(other: ViewItem): Boolean {
        return other is ApplicationHeaderViewItem
                && applicationInfo == other.applicationInfo
    }
}
