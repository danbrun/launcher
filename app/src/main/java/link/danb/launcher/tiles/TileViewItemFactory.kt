package link.danb.launcher.tiles

import android.content.pm.LauncherApps
import javax.inject.Inject
import javax.inject.Singleton
import link.danb.launcher.database.ActivityData
import link.danb.launcher.extensions.resolveActivity
import link.danb.launcher.extensions.resolveConfigurableShortcut
import link.danb.launcher.extensions.resolveShortcut
import link.danb.launcher.icons.LauncherIconCache
import link.danb.launcher.shortcuts.ConfigurableShortcutData
import link.danb.launcher.shortcuts.ShortcutData
import link.danb.launcher.tiles.TileViewItem.Style

@Singleton
class TileViewItemFactory
@Inject
constructor(
  private val launcherApps: LauncherApps,
  private val launcherIconCache: LauncherIconCache
) {

  suspend fun getTileViewItem(data: ActivityData, style: Style) =
    TileViewItem(
      style,
      data,
      launcherApps.resolveActivity(data).label.toString(),
      launcherIconCache.get(data)
    ) { other ->
      this is ActivityData &&
        other is ActivityData &&
        componentName == other.componentName &&
        userHandle == other.userHandle
    }

  suspend fun getTileViewItem(data: ShortcutData, style: Style) =
    TileViewItem(
      style,
      data,
      launcherApps.resolveShortcut(data).shortLabel.toString(),
      launcherIconCache.get(data)
    )

  suspend fun getTileViewItem(data: ConfigurableShortcutData, style: Style) =
    TileViewItem(
      style,
      data,
      launcherApps.resolveConfigurableShortcut(data).label.toString(),
      launcherIconCache.get(data)
    )
}