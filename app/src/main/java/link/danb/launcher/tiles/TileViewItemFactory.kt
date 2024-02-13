package link.danb.launcher.tiles

import javax.inject.Inject
import javax.inject.Singleton
import link.danb.launcher.apps.LauncherResourceProvider
import link.danb.launcher.data.UserShortcut
import link.danb.launcher.data.UserShortcutCreator
import link.danb.launcher.database.ActivityData
import link.danb.launcher.tiles.TileViewItem.Style

@Singleton
class TileViewItemFactory
@Inject
constructor(private val launcherResourceProvider: LauncherResourceProvider) {

  suspend fun getTileViewItem(data: ActivityData, style: Style) =
    TileViewItem(
      style,
      data,
      launcherResourceProvider.getLabel(data.userActivity),
      launcherResourceProvider.getIcon(data.userActivity).await(),
    ) { other ->
      this is ActivityData && other is ActivityData && userActivity == other.userActivity
    }

  suspend fun getTileViewItem(data: UserShortcut, style: Style) =
    TileViewItem(
      style,
      data,
      launcherResourceProvider.getLabel(data),
      launcherResourceProvider.getIcon(data).await(),
    )

  suspend fun getTileViewItem(data: UserShortcutCreator, style: Style) =
    TileViewItem(
      style,
      data,
      launcherResourceProvider.getLabel(data),
      launcherResourceProvider.getIcon(data).await(),
    )
}
