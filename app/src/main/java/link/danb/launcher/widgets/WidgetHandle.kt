package link.danb.launcher.widgets

import android.appwidget.AppWidgetProviderInfo
import android.os.Parcelable
import android.os.UserHandle
import kotlinx.parcelize.Parcelize

/** Represents an allocated widget ID and corresponding widget provider. */
@Parcelize
data class WidgetHandle(val id: Int, val info: AppWidgetProviderInfo, val user: UserHandle) :
  Parcelable
