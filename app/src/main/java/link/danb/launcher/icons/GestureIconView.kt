package link.danb.launcher.icons

import android.content.Context
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.graphics.toRectF
import androidx.core.graphics.withMatrix
import link.danb.launcher.R
import link.danb.launcher.extensions.boundsOnScreen
import link.danb.launcher.gestures.GestureContract

@RequiresApi(Build.VERSION_CODES.Q)
class GestureIconView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
  LinearLayout(context, attrs) {

  private val surfaceView: SurfaceView = SurfaceView(context)

  private val surfaceHolderCallback =
    object : SurfaceHolder.Callback {
      override fun surfaceCreated(holder: SurfaceHolder) {
        update()
        draw()
      }

      override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        update()
        draw()
      }

      override fun surfaceDestroyed(holder: SurfaceHolder) {}
    }

  private val onFinishCallback =
    Message.obtain().apply {
      replyTo =
        Messenger(
          Handler(Looper.getMainLooper()) {
            finish()
            true
          }
        )
    }

  private var gestureAnimationData: GestureAnimationData? = null

  init {
    addView(surfaceView)

    val iconSize = context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)

    surfaceView.apply {
      setZOrderOnTop(true)

      holder.apply {
        setFormat(PixelFormat.TRANSPARENT)
        setFixedSize(iconSize, iconSize)
        addCallback(surfaceHolderCallback)
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  fun animateNavigationGesture(gestureContract: GestureContract, iconView: ImageView) {
    start(GestureAnimationData(gestureContract, iconView, iconView.drawable))
    update()
    draw()
  }

  private fun start(data: GestureAnimationData) {
    gestureAnimationData = data

    data.iconView.setImageDrawable(null)

    with(surfaceView.layoutParams as LayoutParams) {
      val bounds = data.icon.bounds
      width = bounds.width()
      height = bounds.height()
      leftMargin = bounds.left
      topMargin = bounds.top
    }
    surfaceView.requestLayout()

    visibility = View.VISIBLE
  }

  private fun update() {
    val data = gestureAnimationData ?: return
    val surfaceControl = surfaceView.surfaceControl ?: return

    data.gestureContract.sendBounds(
      data.iconView.boundsOnScreen.toRectF(),
      surfaceControl,
      onFinishCallback,
    )
  }

  private fun draw() {
    val gestureAnimationData = gestureAnimationData ?: return

    val bounds = gestureAnimationData.iconView.boundsOnScreen

    val canvas = surfaceView.holder.lockCanvas() ?: return
    canvas.withMatrix(
      Matrix().apply {
        setRectToRect(
          gestureAnimationData.icon.bounds.toRectF(),
          RectF(0f, 0f, bounds.width().toFloat(), bounds.height().toFloat()),
          Matrix.ScaleToFit.FILL,
        )
      }
    ) {
      gestureAnimationData.icon.draw(canvas)
    }
    surfaceView.holder.unlockCanvasAndPost(canvas)
  }

  private fun finish() {
    visibility = View.GONE

    val data = gestureAnimationData ?: return
    data.iconView.setImageDrawable(data.icon)
    gestureAnimationData = null
  }

  data class GestureAnimationData(
    val gestureContract: GestureContract,
    val iconView: ImageView,
    val icon: Drawable,
  )
}
