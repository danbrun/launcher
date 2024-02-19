package link.danb.launcher.widgets

import android.appwidget.AppWidgetHost
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import link.danb.launcher.database.LauncherDatabase
import link.danb.launcher.database.WidgetData

@HiltViewModel
class WidgetsViewModel
@Inject
constructor(
  private val launcherDatabase: LauncherDatabase,
  private val appWidgetHost: AppWidgetHost,
  private val widgetManager: WidgetManager,
  private val widgetSizeUtil: WidgetSizeUtil,
) : ViewModel() {

  private val widgetData by lazy { launcherDatabase.widgetData() }

  fun delete(widgetId: Int) {
    viewModelScope.launch(Dispatchers.IO) {
      appWidgetHost.deleteAppWidgetId(widgetId)
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
    }
  }

  private suspend fun adjustPosition(widgetId: Int, positionChange: Int) {
    val widgets = widgetData.get().sortedBy { it.position }.toMutableList()
    val widget = widgets.first { it.widgetId == widgetId }
    val originalPosition = widgets.indexOf(widget)

    widgets.remove(widget)
    widgets.add((originalPosition + positionChange).coerceIn(0, widgets.size), widget)

    updatePositions(widgets)
  }

  private suspend fun updatePositions(
    widgets: List<WidgetData> = widgetData.get().sortedBy { it.position }
  ) {
    widgetData.put(
      *widgets.mapIndexed { index, widgetData -> widgetData.copy(position = index) }.toTypedArray()
    )
  }
}
