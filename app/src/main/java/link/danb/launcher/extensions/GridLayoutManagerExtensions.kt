package link.danb.launcher.extensions

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup

fun GridLayoutManager.setSpanSizeProvider(provider: (position: Int, spanCount: Int) -> Int) {
  spanSizeLookup =
    object : SpanSizeLookup() {
      override fun getSpanSize(position: Int): Int = provider(position, spanCount)
    }
}
