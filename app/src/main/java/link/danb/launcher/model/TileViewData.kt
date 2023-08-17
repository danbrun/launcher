package link.danb.launcher.model

import android.graphics.drawable.Drawable

sealed interface TileViewData {
    val name: CharSequence
    val icon: Drawable?

    fun areItemsTheSame(other: TileViewData): Boolean
    fun areContentsTheSame(other: TileViewData): Boolean
}
