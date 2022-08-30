package link.danb.launcher.list

import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable

class ShortcutItem(
    val info: ShortcutInfo,
    override val name: CharSequence,
    override val icon: Drawable
) : ListItem {
    override fun areItemsTheSame(other: ListItem): Boolean {
        return other is ShortcutItem
                && info.`package` == other.info.`package`
                && info.id == other.info.id
    }

    override fun areContentsTheSame(other: ListItem): Boolean {
        return false
    }
}
