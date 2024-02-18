package link.danb.launcher.widgets

import android.appwidget.AppWidgetHost
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import link.danb.launcher.R
import link.danb.launcher.database.LauncherDatabase
import link.danb.launcher.database.WidgetData

@Singleton
class WidgetManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
  appWidgetHost: AppWidgetHost,
  launcherDatabase: LauncherDatabase,
) {

  val isInEditMode: MutableStateFlow<Boolean> = MutableStateFlow(false)

  val widgets: Flow<List<Int>> =
    callbackFlow {
        trySend(appWidgetHost.appWidgetIds.toList())

        val broadcastReceiver =
          object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
              trySend(appWidgetHost.appWidgetIds.toList())
            }
          }

        ContextCompat.registerReceiver(
          context,
          broadcastReceiver,
          IntentFilter(ACTION_WIDGETS_CHANGED),
          ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        awaitClose { context.unregisterReceiver(broadcastReceiver) }
      }
      .shareIn(MainScope(), SharingStarted.WhileSubscribed(replayExpirationMillis = 0), replay = 1)

  val data: Flow<List<WidgetData>> =
    combine(widgets, launcherDatabase.widgetData().getFlow()) { widgets, data ->
        val dataMap =
          data
            .associateBy { it.widgetId }
            .withDefault {
              WidgetData(it, it, context.resources.getDimensionPixelSize(R.dimen.widget_min_height))
            }

        widgets.map { dataMap.getValue(it) }.sortedBy { it.position }
      }
      .shareIn(MainScope(), SharingStarted.WhileSubscribed(replayExpirationMillis = 0), replay = 1)

  fun notifyChange() {
    context.sendBroadcast(Intent(ACTION_WIDGETS_CHANGED).setPackage(context.packageName))
  }

  companion object {
    const val ACTION_WIDGETS_CHANGED = "link.danb.launcher.ACTION_WIDGETS_CHANGED"
  }
}
