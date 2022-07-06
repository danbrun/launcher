package link.danb.launcher.list

import android.content.pm.LauncherActivityInfo
import android.graphics.drawable.Drawable

class AppItem(
    val info: LauncherActivityInfo,
    override val name: CharSequence,
    override val icon: Drawable
) : ListItem {
    override val id: Any
        get() = info
}
