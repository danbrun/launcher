package link.danb.launcher.list

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

class CustomItem(
    private val context: Context,
    @StringRes val nameRes: Int,
    @DrawableRes val iconRes: Int,
    val onClick: ListItemClickHandler?,
    val onLongClick: ListItemClickHandler?
) : ListItem {
    override val name: CharSequence by lazy {
        context.getString(nameRes)
    }

    override val icon: Drawable by lazy {
        val icon = context.getDrawable(iconRes)
        icon ?: ShapeDrawable()
    }

    override fun areItemsTheSame(other: ListItem): Boolean {
        return other is CustomItem
                && nameRes == other.nameRes
                && iconRes == other.iconRes
    }

    override fun areContentsTheSame(other: ListItem): Boolean {
        return false
    }
}
