package link.danb.launcher.ui

import android.content.Context
import android.view.View.OnLayoutChangeListener
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DynamicGridLayoutManager(context: Context, @DimenRes minColumnWidthRes: Int) :
  GridLayoutManager(context, 1) {

  private val minColumnWidth = context.resources.getDimensionPixelSize(minColumnWidthRes)

  private val onLayoutChangeListener = OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
    spanCount = view.measuredWidth / minColumnWidth
  }

  override fun onAttachedToWindow(view: RecyclerView) {
    super.onAttachedToWindow(view)

    view.addOnLayoutChangeListener(onLayoutChangeListener)
  }

  override fun onDetachedFromWindow(view: RecyclerView, recycler: RecyclerView.Recycler) {
    super.onDetachedFromWindow(view, recycler)

    view.removeOnLayoutChangeListener(onLayoutChangeListener)
  }
}
