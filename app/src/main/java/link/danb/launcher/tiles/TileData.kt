package link.danb.launcher.tiles

import link.danb.launcher.database.ActivityData
import link.danb.launcher.shortcuts.ConfigurableShortcutData
import link.danb.launcher.shortcuts.ShortcutData

sealed interface TileData {
  fun areItemsTheSame(other: TileData): Boolean
}

@JvmInline
value class ActivityTileData(val activityData: ActivityData) : TileData {
  override fun areItemsTheSame(other: TileData): Boolean =
    other is ActivityTileData &&
      activityData.componentName == other.activityData.componentName &&
      activityData.userHandle == other.activityData.userHandle
}

@JvmInline
value class ShortcutTileData(val shortcutData: ShortcutData) : TileData {
  override fun areItemsTheSame(other: TileData): Boolean = this == other
}

@JvmInline
value class ConfigurableShortcutTileData(val configurableShortcutData: ConfigurableShortcutData) :
  TileData {
  override fun areItemsTheSame(other: TileData): Boolean = this == other
}
