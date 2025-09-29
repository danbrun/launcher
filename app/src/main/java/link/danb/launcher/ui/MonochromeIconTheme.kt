package link.danb.launcher.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.google.android.material.R
import com.google.android.material.color.MaterialColors

data class MonochromeIconTheme(val foreground: Color, val background: Color) {

  companion object {
    val theme: MonochromeIconTheme
      @Composable
      @ReadOnlyComposable
      get() =
        MonochromeIconTheme(
          MaterialTheme.colorScheme.primary,
          MaterialTheme.colorScheme.primaryContainer,
        )

    fun fromContext(context: Context): MonochromeIconTheme =
      MonochromeIconTheme(
        Color(
          MaterialColors.getColor(context, R.attr.colorOnPrimary, android.graphics.Color.WHITE)
        ),
        Color(
          MaterialColors.getColor(
            context,
            R.attr.colorPrimaryContainer,
            android.graphics.Color.BLACK,
          )
        ),
      )
  }
}
