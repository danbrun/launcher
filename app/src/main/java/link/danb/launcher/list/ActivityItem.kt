package link.danb.launcher.list

import android.graphics.drawable.Drawable
import link.danb.launcher.LauncherActivity

class ActivityItem(val launcherActivity: LauncherActivity) : ListItem {
    override val name: CharSequence
        get() = launcherActivity.name

    override val icon: Drawable
        get() = launcherActivity.icon

    override fun areItemsTheSame(other: ListItem): Boolean {
        return other is ActivityItem
                && launcherActivity.component == other.launcherActivity.component
                && launcherActivity.user == other.launcherActivity.user
    }

    override fun areContentsTheSame(other: ListItem): Boolean {
        return other is ActivityItem
                && launcherActivity.timestamp == other.launcherActivity.timestamp
    }
}
