package link.danb.launcher.tiles

import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import javax.inject.Inject
import javax.inject.Singleton
import link.danb.launcher.R
import link.danb.launcher.database.ActivityData
import link.danb.launcher.extensions.resolveActivity
import link.danb.launcher.extensions.resolveConfigurableShortcut
import link.danb.launcher.extensions.resolveShortcut
import link.danb.launcher.icons.LauncherIconCache
import link.danb.launcher.shortcuts.ConfigurableShortcutData
import link.danb.launcher.shortcuts.ShortcutData

@Singleton
class TileViewItemFactory
@Inject
constructor(
  private val launcherApps: LauncherApps,
  private val launcherIconCache: LauncherIconCache
) {

  suspend fun getCardTileViewItem(data: Any): TileViewItem =
    TileViewItem(R.id.card_tile_view_type_id, getTileData(data), getName(data), getIcon(data))

  suspend fun getTransparentTileViewItem(data: Any): TileViewItem =
    TileViewItem(
      R.id.transparent_tile_view_type_id,
      getTileData(data),
      getName(data),
      getIcon(data)
    )

  private fun getTileData(data: Any): TileData =
    when (data) {
      is ActivityData -> ActivityTileData(data)
      is ShortcutData -> ShortcutTileData(data)
      is ConfigurableShortcutData -> ConfigurableShortcutTileData(data)
      else -> throw NotImplementedError()
    }

  private fun getName(data: Any): String =
    when (data) {
      is ActivityData -> launcherApps.resolveActivity(data).label.toString()
      is ShortcutData -> launcherApps.resolveShortcut(data).shortLabel.toString()
      is ConfigurableShortcutData -> launcherApps.resolveConfigurableShortcut(data).label.toString()
      else -> throw NotImplementedError()
    }

  private suspend fun getIcon(data: Any): Drawable =
    when (data) {
      is ActivityData -> launcherIconCache.get(data)
      is ShortcutData -> launcherIconCache.get(data)
      is ConfigurableShortcutData -> launcherIconCache.get(data)
      else -> throw NotImplementedError()
    }
}
