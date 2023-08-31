package link.danb.launcher.widgets

import android.app.Application
import link.danb.launcher.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetSizeUtil @Inject constructor(application: Application) {

  private val minHeight: Int by lazy {
    application.resources.getDimensionPixelSize(R.dimen.widget_min_height)
  }

  private val maxHeight: Int by lazy {
    application.resources.getDimensionPixelSize(R.dimen.widget_max_height)
  }

  fun getWidgetHeight(height: Int): Int = height.coerceIn(minHeight, maxHeight)
}
