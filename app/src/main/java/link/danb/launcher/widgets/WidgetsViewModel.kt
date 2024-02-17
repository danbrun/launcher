package link.danb.launcher.widgets

import android.app.Application
import android.appwidget.AppWidgetHost
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import link.danb.launcher.R
import link.danb.launcher.database.LauncherDatabase
import link.danb.launcher.database.WidgetData

@HiltViewModel
class WidgetsViewModel
@Inject
constructor(
  private val application: Application,
  private val launcherDatabase: LauncherDatabase,
  private val appWidgetHost: AppWidgetHost,
  private val widgetManager: WidgetManager,
  private val widgetSizeUtil: WidgetSizeUtil,
) : ViewModel() {

  private val widgetData by lazy { launcherDatabase.widgetData() }

  init {
    viewModelScope.launch(Dispatchers.IO) {
      cleanupUnboundWidgets()
      checkForNewWidgets()
      widgetManager.notifyChange()
    }
  }

  fun checkForNewWidgets() {
    viewModelScope.launch(Dispatchers.IO) {
      insertNewWidgets()
      updatePositions()
      widgetManager.notifyChange()
    }
  }

  fun delete(widgetId: Int) {
    viewModelScope.launch(Dispatchers.IO) {
      appWidgetHost.deleteAppWidgetId(widgetId)
      cleanupUnboundWidgets()
      updatePositions()
      widgetManager.notifyChange()
    }
  }

  fun moveUp(widgetId: Int) {
    viewModelScope.launch(Dispatchers.IO) { adjustPosition(widgetId, -1) }
  }

  fun moveDown(widgetId: Int) {
    viewModelScope.launch(Dispatchers.IO) { adjustPosition(widgetId, 1) }
  }

  fun setHeight(widgetId: Int, height: Int) {
    viewModelScope.launch(Dispatchers.IO) {
      widgetData.put(
        widgetData
          .get()
          .first { it.widgetId == widgetId }
          .copy(height = widgetSizeUtil.getWidgetHeight(height))
      )
      widgetManager.notifyChange()
    }
  }

  private suspend fun adjustPosition(widgetId: Int, positionChange: Int) {
    val widgets = widgetData.get().sortedBy { it.position }.toMutableList()
    val widget = widgets.first { it.widgetId == widgetId }
    val originalPosition = widgets.indexOf(widget)

    widgets.remove(widget)
    widgets.add((originalPosition + positionChange).coerceIn(0, widgets.size), widget)

    updatePositions(widgets)
    widgetManager.notifyChange()
  }

  private suspend fun insertNewWidgets() {
    val widgetIdsToAdd =
      appWidgetHost.appWidgetIds.toSet() - widgetData.get().map { it.widgetId }.toSet()

    widgetData.put(
      *widgetIdsToAdd
        .map {
          WidgetData(
            it,
            Int.MAX_VALUE,
            application.resources.getDimensionPixelSize(R.dimen.widget_min_height),
          )
        }
        .toTypedArray()
    )
  }

  private suspend fun updatePositions(
    widgets: List<WidgetData> = widgetData.get().sortedBy { it.position }
  ) {
    widgetData.put(
      *widgets.mapIndexed { index, widgetData -> widgetData.copy(position = index) }.toTypedArray()
    )
  }

  private suspend fun cleanupUnboundWidgets() {
    widgetData.get().forEach {
      if (!appWidgetHost.appWidgetIds.contains(it.widgetId)) {
        widgetData.delete(it)
      }
    }
  }
}
