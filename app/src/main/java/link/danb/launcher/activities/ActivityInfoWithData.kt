package link.danb.launcher.activities

import android.content.pm.LauncherActivityInfo
import link.danb.launcher.database.ActivityData

data class ActivityInfoWithData(val info: LauncherActivityInfo, val data: ActivityData)
