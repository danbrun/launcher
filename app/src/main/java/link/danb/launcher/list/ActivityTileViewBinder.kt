package link.danb.launcher.list

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.model.LauncherActivityData
import link.danb.launcher.utils.inflate
import link.danb.launcher.utils.setSize

class ActivityTileViewBinder(private val activityTileListener: ActivityTileListener? = null) :
    ViewBinder {
    override val viewType: Int = R.id.activity_tile_view_type_id

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return ActivityTileViewHolder(parent.inflate(R.layout.activity_tile_view))
    }

    override fun bindViewHolder(holder: ViewHolder, viewItem: ViewItem) {
        holder as ActivityTileViewHolder
        viewItem as ActivityTileViewItem

        holder.textView.apply {
            text = viewItem.name
            viewItem.icon.setSize(
                context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
            )
            setCompoundDrawables(viewItem.icon, null, null, null)

            isClickable = activityTileListener == null
            setOnClickListener { activityTileListener?.onClick(it, viewItem) }
            setOnLongClickListener { activityTileListener?.onLongClick(it, viewItem); true }
        }
    }

    private class ActivityTileViewHolder(view: View) : ViewHolder(view) {
        val textView = view as TextView
    }
}

class ActivityTileViewItem(val launcherActivityData: LauncherActivityData) : ViewItem {
    override val viewType: Int = R.id.activity_tile_view_type_id

    val name: CharSequence
        get() = launcherActivityData.name

    val icon: Drawable
        get() = launcherActivityData.icon

    override fun areItemsTheSame(other: ViewItem): Boolean {
        return other is ActivityTileViewItem
                && launcherActivityData.component == other.launcherActivityData.component
                && launcherActivityData.user == other.launcherActivityData.user
    }

    override fun areContentsTheSame(other: ViewItem): Boolean {
        return other is ActivityTileViewItem
                && launcherActivityData.timestamp == other.launcherActivityData.timestamp
    }
}

fun interface ActivityTileListener {
    fun onClick(view: View, activityViewItem: ActivityTileViewItem)
    fun onLongClick(view: View, activityViewItem: ActivityTileViewItem) {}
}
