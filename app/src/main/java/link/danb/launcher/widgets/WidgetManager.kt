package link.danb.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import link.danb.launcher.R
import link.danb.launcher.database.LauncherDatabase
import link.danb.launcher.database.WidgetData
import link.danb.launcher.extensions.allowPendingIntentBackgroundActivityStart
import link.danb.launcher.extensions.makeScaleUpAnimation

@Singleton
class WidgetManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val appWidgetHost: AppWidgetHost,
  launcherDatabase: LauncherDatabase,
) {

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
      .stateIn(MainScope(), SharingStarted.WhileSubscribed(replayExpirationMillis = 0), listOf())

  val data: Flow<List<WidgetData>> =
    combine(widgets, launcherDatabase.widgetData().getFlow()) { widgets, data ->
        val dataMap =
          data
            .associateBy { it.widgetId }
            .withDefault {
              WidgetData(it, it, context.resources.getDimensionPixelSize(R.dimen.widget_min_height))
            }

        val dataList = widgets.map { dataMap.getValue(it) }.sortedBy { it.position }

        val updatedList =
          dataList.mapIndexed { index, widgetData -> widgetData.copy(position = index) }

        val changed = updatedList - dataList.toSet()
        if (changed.isNotEmpty()) {
          withContext(Dispatchers.IO) { launcherDatabase.widgetData().put(*changed.toTypedArray()) }
        }

        dataList
      }
      .stateIn(MainScope(), SharingStarted.WhileSubscribed(replayExpirationMillis = 0), listOf())

  fun startConfigurationActivity(activity: Activity, view: View, widgetId: Int) {
    appWidgetHost.startAppWidgetConfigureActivityForResult(
      activity,
      widgetId,
      /* intentFlags = */ 0,
      R.id.app_widget_configure_request_id,
      view.makeScaleUpAnimation().allowPendingIntentBackgroundActivityStart().toBundle(),
    )
  }

  fun notifyChange() {
    context.sendBroadcast(Intent(ACTION_WIDGETS_CHANGED).setPackage(context.packageName))
  }

  companion object {
    const val ACTION_WIDGETS_CHANGED = "link.danb.launcher.ACTION_WIDGETS_CHANGED"
  }
}
