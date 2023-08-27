package link.danb.launcher.extensions

import android.appwidget.AppWidgetHostView
import android.os.Build
import android.os.Bundle
import android.util.SizeF

/** Updates app widget size with the appropriate API and catching internal errors. */
fun AppWidgetHostView.updateAppWidgetSize(maxWidthPixels: Int, maxHeightPixels: Int) {
    val maxWidthDips = (maxWidthPixels / resources.displayMetrics.density).toInt()
    val maxHeightDips = (maxHeightPixels / resources.displayMetrics.density).toInt()

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updateAppWidgetSize(
                Bundle(), listOf(SizeF(maxWidthDips.toFloat(), maxHeightDips.toFloat()))
            )
        } else {
            @Suppress("DEPRECATION") updateAppWidgetSize(
                Bundle(),
                /* minWidth = */ 0,
                /* minHeight = */ 0,
                maxWidthDips,
                maxHeightDips
            )
        }
    } catch (exception: NullPointerException) {
        // Ignore if update fails due to improper binding.
    }
}
