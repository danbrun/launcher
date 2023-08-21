package link.danb.launcher.tiles

import android.content.pm.LauncherActivityInfo
import android.content.pm.ShortcutInfo

sealed interface TileData {
    fun areItemsTheSame(other: TileData): Boolean
}

class ActivityTileData(val info: LauncherActivityInfo) : TileData {
    override fun areItemsTheSame(other: TileData): Boolean =
        other is ActivityTileData && info.componentName == other.info.componentName && info.user == other.info.user
}

class ShortcutTileData(val info: ShortcutInfo) : TileData {
    override fun areItemsTheSame(other: TileData): Boolean =
        other is ShortcutTileData && info.`package` == other.info.`package` && info.id == other.info.id
}
