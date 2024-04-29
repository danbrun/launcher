package link.danb.launcher.gestures

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
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
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import link.danb.launcher.R
import link.danb.launcher.ui.LauncherIcon

@RequiresApi(Build.VERSION_CODES.Q)
class GestureIconView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
  LinearLayout(context, attrs) {

  private val iconSize = context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)

  private val surfaceView: SurfaceView = SurfaceView(context)
  private val composeView: ComposeView = ComposeView(context)

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
    surfaceView.apply {
      setZOrderOnTop(true)

      holder.apply {
        setFormat(PixelFormat.TRANSPARENT)
        setFixedSize(iconSize, iconSize)
        addCallback(surfaceHolderCallback)
      }
    }

    addView(composeView)
    composeView.visibility = View.INVISIBLE
  }

  var onFinishGestureAnimation: () -> Unit = {}

  @RequiresApi(Build.VERSION_CODES.Q)
  fun animateNavigationGesture(
    gestureContract: GestureContract,
    bounds: RectF,
    icon: AdaptiveIconDrawable,
    badge: Drawable,
  ) {
    gestureAnimationData = GestureAnimationData(gestureContract, bounds, icon, badge)
    update()
  }

  private fun update() {
    val data = gestureAnimationData ?: return
    val surfaceControl = surfaceView.surfaceControl ?: return

    data.gestureContract.sendBounds(data.bounds, surfaceControl, onFinishCallback)
  }

  private fun draw() {
    val data = gestureAnimationData ?: return

    composeView.setContent {
      LauncherIcon(
        icon = data.icon,
        badge = data.badge,
        Modifier.size(dimensionResource(R.dimen.launcher_icon_size)),
      )
    }
    composeView.post {
      val canvas = surfaceView.holder.lockCanvas() ?: return@post
      composeView.draw(canvas)
      surfaceView.holder.unlockCanvasAndPost(canvas)

      visibility = View.VISIBLE
    }
  }

  private fun finish() {
    gestureAnimationData = null
    onFinishGestureAnimation()

    visibility = View.GONE
  }

  data class GestureAnimationData(
    val gestureContract: GestureContract,
    val bounds: RectF,
    val icon: AdaptiveIconDrawable,
    val badge: Drawable,
  )
}
