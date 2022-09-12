package link.danb.launcher.list

import android.graphics.drawable.Drawable
import link.danb.launcher.model.LauncherActivityData

class ActivityItem(val launcherActivityData: LauncherActivityData) : ListItem {
    override val name: CharSequence
        get() = launcherActivityData.name

    override val icon: Drawable
        get() = launcherActivityData.icon

    override fun areItemsTheSame(other: ListItem): Boolean {
        return other is ActivityItem
                && launcherActivityData.component == other.launcherActivityData.component
                && launcherActivityData.user == other.launcherActivityData.user
    }

    override fun areContentsTheSame(other: ListItem): Boolean {
        return other is ActivityItem
                && launcherActivityData.timestamp == other.launcherActivityData.timestamp
    }
}
