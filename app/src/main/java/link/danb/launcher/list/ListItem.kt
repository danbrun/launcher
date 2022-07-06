package link.danb.launcher.list

import android.graphics.drawable.Drawable

interface ListItem {
    val id: Any
    val name: CharSequence
    val icon: Drawable
}
