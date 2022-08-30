package link.danb.launcher.list

import android.graphics.drawable.Drawable

interface ListItem {
    val name: CharSequence
    val icon: Drawable

    fun areItemsTheSame(other: ListItem): Boolean
    fun areContentsTheSame(other: ListItem): Boolean
}
