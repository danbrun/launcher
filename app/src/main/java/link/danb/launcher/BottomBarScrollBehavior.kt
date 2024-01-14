package link.danb.launcher

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior

class BottomBarScrollBehavior<V : View>(context: Context, attrs: AttributeSet) :
  HideBottomViewOnScrollBehavior<V>(context, attrs) {

  override fun onNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: V,
    target: View,
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int,
    type: Int,
    consumed: IntArray
  ) {
    if (dyConsumed + dyUnconsumed > 0) {
      slideDown(child)
    } else if (dyConsumed + dyUnconsumed < 0) {
      slideUp(child)
    }
  }
}
