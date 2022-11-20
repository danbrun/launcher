package link.danb.launcher.list

import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import link.danb.launcher.R
import link.danb.launcher.utils.inflate
import link.danb.launcher.utils.setSize

class ShortcutTileViewBinder(private val shortcutTileListener: ShortcutTileListener? = null) :
    ViewBinder {
    override val viewType: Int = R.id.shortcut_tile_view_type_id

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return ShortcutTileViewHolder(parent.inflate(R.layout.shortcut_tile_view))
    }

    override fun bindViewHolder(holder: ViewHolder, viewItem: ViewItem) {
        holder as ShortcutTileViewHolder
        viewItem as ShortcutTileViewItem

        holder.textView.apply {
            text = viewItem.name
            viewItem.icon.setSize(
                context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
            )
            setCompoundDrawables(viewItem.icon, null, null, null)

            isClickable = shortcutTileListener != null
            setOnClickListener { shortcutTileListener?.onClick(it, viewItem) }
            setOnLongClickListener { shortcutTileListener?.onLongClick(it, viewItem); true }
        }
    }

    private class ShortcutTileViewHolder(view: View) : ViewHolder(view) {
        val textView = view as TextView
    }
}

class ShortcutTileViewItem(val info: ShortcutInfo, val name: CharSequence, val icon: Drawable) :
    ViewItem {
    override val viewType: Int = R.id.shortcut_tile_view_type_id

    override fun areItemsTheSame(other: ViewItem): Boolean {
        return other is ShortcutTileViewItem
                && info.`package` == other.info.`package`
                && info.id == other.info.id
    }

    override fun areContentsTheSame(other: ViewItem): Boolean {
        return false
    }
}

fun interface ShortcutTileListener {
    fun onClick(view: View, shortcutTileViewItem: ShortcutTileViewItem)
    fun onLongClick(view: View, shortcutTileViewItem: ShortcutTileViewItem) {}
}