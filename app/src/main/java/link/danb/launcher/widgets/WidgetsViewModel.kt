package link.danb.launcher.widgets

import android.app.Application
import android.appwidget.AppWidgetHost
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
  private val widgetSizeUtil: WidgetSizeUtil,
) : ViewModel() {

  private val widgetData by lazy { launcherDatabase.widgetData() }

  private val _widgets: MutableStateFlow<List<WidgetData>> = MutableStateFlow(listOf())
  private val _widgetToEdit: MutableStateFlow<Int?> = MutableStateFlow(null)

  val widgets: StateFlow<List<WidgetData>> = _widgets
  val widgetToEdit: StateFlow<Int?> = _widgetToEdit

  init {
    viewModelScope.launch(Dispatchers.IO) {
      cleanupUnboundWidgets()
      checkForNewWidgets()
      reloadFromDatabase()
    }
  }

  fun startEditing(widgetId: Int) {
    _widgetToEdit.value = widgetId
  }

  fun finishEditing() {
    _widgetToEdit.value = null
  }

  fun checkForNewWidgets() {
    viewModelScope.launch(Dispatchers.IO) {
      insertNewWidgets()
      updatePositions()
      reloadFromDatabase()
    }
  }

  fun delete(widgetId: Int) {
    viewModelScope.launch(Dispatchers.IO) {
      appWidgetHost.deleteAppWidgetId(widgetId)
      cleanupUnboundWidgets()
      updatePositions()
      reloadFromDatabase()
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
      reloadFromDatabase()
    }
  }

  private fun adjustPosition(widgetId: Int, positionChange: Int) {
    val widgets = widgetData.get().sortedBy { it.position }.toMutableList()
    val widget = widgets.first { it.widgetId == widgetId }
    val originalPosition = widgets.indexOf(widget)

    widgets.remove(widget)
    widgets.add((originalPosition + positionChange).coerceIn(0, widgets.size), widget)

    updatePositions(widgets)
    reloadFromDatabase()
  }

  private fun insertNewWidgets() {
    val widgetIdsToAdd =
      appWidgetHost.appWidgetIds.toSet() - widgetData.get().map { it.widgetId }.toSet()

    widgetData.put(
      *widgetIdsToAdd
        .map {
          WidgetData(
            it,
            Int.MAX_VALUE,
            application.resources.getDimensionPixelSize(R.dimen.widget_min_height)
          )
        }
        .toTypedArray()
    )
  }

  private fun updatePositions(
    widgets: List<WidgetData> = widgetData.get().sortedBy { it.position }
  ) {
    widgetData.put(
      *widgets.mapIndexed { index, widgetData -> widgetData.copy(position = index) }.toTypedArray()
    )
  }

  private fun cleanupUnboundWidgets() {
    widgetData.get().forEach {
      if (!appWidgetHost.appWidgetIds.contains(it.widgetId)) {
        widgetData.delete(it)
      }
    }
  }

  private fun reloadFromDatabase() {
    _widgets.value = widgetData.get().sortedBy { it.position }
  }
}
