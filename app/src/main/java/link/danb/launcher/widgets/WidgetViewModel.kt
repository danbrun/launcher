package link.danb.launcher.widgets

import android.app.Application
import android.appwidget.AppWidgetHost
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import link.danb.launcher.R
import link.danb.launcher.model.LauncherDatabase
import link.danb.launcher.model.WidgetMetadata
import javax.inject.Inject

@HiltViewModel
class WidgetViewModel @Inject constructor(
    private val application: Application,
    private val launcherDatabase: LauncherDatabase,
    private val appWidgetHost: AppWidgetHost,
) : ViewModel() {

    private val widgetMetadata by lazy {
        launcherDatabase.widgetMetadata()
    }

    private val _widgets: MutableStateFlow<List<WidgetMetadata>> = MutableStateFlow(listOf())
    val widgets: StateFlow<List<WidgetMetadata>> = _widgets

    private val _widgetToEdit: MutableStateFlow<Int?> = MutableStateFlow(null)
    val widgetToEdit: StateFlow<Int?> = _widgetToEdit

    fun startEditing(widgetId: Int) {
        _widgetToEdit.value = widgetId
    }

    fun finishEditing() {
        _widgetToEdit.value = null
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshInBackground()
        }
    }

    fun delete(widgetId: Int) {
        appWidgetHost.deleteAppWidgetId(widgetId)
        refresh()
    }

    fun moveUp(widgetId: Int) {
        adjustPosition(widgetId, -1)
    }

    fun moveDown(widgetId: Int) {
        adjustPosition(widgetId, 1)
    }

    fun setHeight(widgetId: Int, height: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = widgetMetadata.get().first { it.widgetId == widgetId }

            widgetMetadata.put(
                metadata.copy(
                    height = height.coerceIn(
                        application.resources.getDimensionPixelSize(
                            R.dimen.widget_min_height
                        ), application.resources.getDimensionPixelSize(
                            R.dimen.widget_max_height
                        )
                    )
                )
            )
            refreshInBackground()
        }
    }

    private fun adjustPosition(widgetId: Int, positionChange: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val widgets = widgetMetadata.get().sortedBy { it.position }.toMutableList()
            val widget = widgets.first { it.widgetId == widgetId }
            val originalPosition = widgets.indexOf(widget)
            val targetPosition = originalPosition + positionChange

            if (targetPosition in 0 until widgets.size) {
                widgets[originalPosition] = widgets[targetPosition]
                widgets[targetPosition] = widget
                widgets.indices.forEach {
                    widgetMetadata.put(widgets[it].copy(position = it))
                }
                refreshInBackground()
            }
        }
    }

    private fun refreshInBackground() {
        var widgetMetadataList = widgetMetadata.get()
        val widgetIds = appWidgetHost.appWidgetIds.toList()

        // Remove metadata for any unbound widgets.
        widgetMetadataList = widgetMetadataList.filter {
            if (!widgetIds.contains(it.widgetId)) {
                widgetMetadata.delete(it)
                false
            } else {
                true
            }
        }

        // Add metadata for any bound widgets missing from the database.
        var position = widgetMetadataList.maxOfOrNull { it.position } ?: 0
        widgetIds.forEach { widgetId ->
            if (widgetMetadataList.none { it.widgetId == widgetId }) {
                widgetMetadata.put(
                    WidgetMetadata(
                        widgetId,
                        position++,
                        height = application.resources.getDimensionPixelSize(R.dimen.widget_min_height)
                    )
                )
            }
        }

        _widgets.value = widgetMetadata.get().sortedBy { it.position }
    }
}
