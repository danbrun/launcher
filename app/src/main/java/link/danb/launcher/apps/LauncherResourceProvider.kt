package link.danb.launcher.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.PaintDrawable
import android.os.UserHandle
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import link.danb.launcher.R
import link.danb.launcher.components.UserActivity
import link.danb.launcher.components.UserApplication
import link.danb.launcher.components.UserComponent
import link.danb.launcher.components.UserShortcut
import link.danb.launcher.components.UserShortcutCreator
import link.danb.launcher.extensions.resolveActivity
import link.danb.launcher.extensions.resolveApplication
import link.danb.launcher.extensions.resolveShortcut
import link.danb.launcher.ui.LauncherIconData
import link.danb.launcher.ui.LauncherTileData

@Singleton
class LauncherResourceProvider
@Inject
constructor(@ApplicationContext private val context: Context) {

  private val launcherApps: LauncherApps by lazy { checkNotNull(context.getSystemService()) }
  private val density: Int by lazy { context.resources.displayMetrics.densityDpi }
  private val icons: MutableMap<UserComponent, Deferred<AdaptiveIconDrawable>> = mutableMapOf()
  private val badges: MutableMap<UserHandle, Drawable> = mutableMapOf()
  private val coroutineScope: CoroutineScope = MainScope() + Dispatchers.IO

  init {
    launcherApps.registerCallback(
      LauncherAppsCallback { packageNames: List<String>, userHandle: UserHandle ->
        icons.keys.removeIf { it.packageName in packageNames && it.userHandle == userHandle }
      }
    )
  }

  fun getLabel(userComponent: UserComponent): String =
    when (userComponent) {
      is UserApplication -> {
        launcherApps.resolveApplication(userComponent).loadLabel(context.packageManager).toString()
      }
      is UserActivity -> {
        launcherApps.resolveActivity(userComponent).label.toString()
      }
      is UserShortcut -> {
        launcherApps.resolveShortcut(userComponent).shortLabel.toString()
      }
      is UserShortcutCreator -> {
        getLabel(UserActivity(userComponent.componentName, userComponent.userHandle))
      }
    }

  fun getIconWithCache(userComponent: UserComponent): Deferred<AdaptiveIconDrawable> =
    synchronized(this) {
      icons.getOrPut(userComponent) { coroutineScope.async { getIcon(userComponent) } }
    }

  suspend fun getIcon(userComponent: UserComponent): AdaptiveIconDrawable =
    withContext(Dispatchers.IO) { userComponent.getSourceIcon().toAdaptiveIconDrawable() }

  fun getBadge(userHandle: UserHandle): Drawable =
    badges.getOrPut(userHandle) {
      val size = context.resources.getDimensionPixelOffset(R.dimen.launcher_icon_size)
      context.packageManager.getUserBadgedIcon(
        BitmapDrawable(context.resources, Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)),
        userHandle,
      )
    }

  suspend fun getTileData(userComponent: UserComponent): LauncherTileData =
    LauncherTileData(
      LauncherIconData(getIcon(userComponent), getBadge(userComponent.userHandle)),
      getLabel(userComponent),
    )

  suspend fun getTileDataWithCache(userComponent: UserComponent): Deferred<LauncherTileData> =
    coroutineScope.async {
      LauncherTileData(
        LauncherIconData(
          getIconWithCache(userComponent).await(),
          getBadge(userComponent.userHandle),
        ),
        getLabel(userComponent),
      )
    }

  private fun UserComponent.getSourceIcon(): Drawable =
    when (this) {
      is UserApplication -> {
        launcherApps.resolveApplication(this).loadUnbadgedIcon(context.packageManager)
      }
      is UserActivity -> {
        launcherApps.resolveActivity(this).getIcon(density)
      }
      is UserShortcut -> {
        launcherApps.getShortcutIconDrawable(launcherApps.resolveShortcut(this), density)
          ?: UserApplication(packageName, userHandle).getSourceIcon()
      }
      is UserShortcutCreator -> {
        UserActivity(componentName, userHandle).getSourceIcon()
      }
    }

  private suspend fun Drawable.toAdaptiveIconDrawable(): AdaptiveIconDrawable {
    if (this is AdaptiveIconDrawable && foreground != null && background != null) {
      return this
    }

    val palette = withContext(Dispatchers.IO) { Palette.from(toBitmap()).generate() }
    val background = PaintDrawable(palette.getMutedColor(Color.WHITE))
    val foreground = InsetDrawable(this, AdaptiveIconDrawable.getExtraInsetFraction())

    return AdaptiveIconDrawable(background, foreground)
  }
}
