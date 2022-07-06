package link.danb.launcher.list

import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable

class ShortcutItem(
    val info: ShortcutInfo,
    override val name: CharSequence,
    override val icon: Drawable
) : ListItem {
    override val id: Any
        get() = info
}
