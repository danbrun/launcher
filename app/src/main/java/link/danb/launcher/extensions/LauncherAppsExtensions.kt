package link.danb.launcher.extensions

import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import link.danb.launcher.database.ActivityData

fun LauncherApps.resolveActivity(
  componentName: ComponentName,
  userHandle: UserHandle
): LauncherActivityInfo = resolveActivity(Intent().setComponent(componentName), userHandle)

fun LauncherApps.resolveActivity(activityData: ActivityData): LauncherActivityInfo =
  resolveActivity(activityData.componentName, activityData.userHandle)
