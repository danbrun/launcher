package link.danb.launcher.gestures

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.RectF
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import link.danb.launcher.R
import link.danb.launcher.ui.LauncherIconData
import link.danb.launcher.ui.MonochromeIconTheme
import link.danb.launcher.ui.drawBadge
import link.danb.launcher.ui.drawLauncherIcon

@RequiresApi(Build.VERSION_CODES.Q)
class GestureIconView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
  LinearLayout(context, attrs) {

  private val iconSize = context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)

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
    surfaceView.apply {
      setZOrderOnTop(true)

      holder.apply {
        setFormat(PixelFormat.TRANSPARENT)
        setFixedSize(iconSize, iconSize)
        addCallback(surfaceHolderCallback)
      }
    }
  }

  var onFinishGestureAnimation: () -> Unit = {}

  @RequiresApi(Build.VERSION_CODES.Q)
  fun animateNavigationGesture(
    gestureContract: GestureContract,
    bounds: RectF,
    launcherIconData: LauncherIconData,
    useMonochromeIcons: Boolean,
  ) {
    gestureAnimationData =
      GestureAnimationData(gestureContract, bounds, launcherIconData, useMonochromeIcons)
    update()
  }

  private fun update() {
    val data = gestureAnimationData ?: return
    val surfaceControl = surfaceView.surfaceControl ?: return

    data.gestureContract.sendBounds(data.bounds, surfaceControl, onFinishCallback)
  }

  private fun draw() {
    val data = gestureAnimationData ?: return

    val canvas = surfaceView.holder.lockCanvas() ?: return
    CanvasDrawScope().draw(
      Density(context),
      LayoutDirection.Ltr,
      Canvas(canvas),
      Size(canvas.width.toFloat(), canvas.height.toFloat()),
    ) {
      clipPath(
        Path().apply {
          addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(size.width * 0.25f)))
        }
      ) {
        val theme =
          if (data.useMonochromeIcons) {
            MonochromeIconTheme.fromContext(context)
          } else {
            MonochromeIconTheme(Color.White, Color.Blue)
          }
        drawLauncherIcon(data.launcherIconData.icon, theme.takeIf { data.useMonochromeIcons })
        drawBadge(data.launcherIconData.badge, theme)
      }
    }
    surfaceView.holder.unlockCanvasAndPost(canvas)

    visibility = View.VISIBLE
  }

  private fun finish() {
    gestureAnimationData = null
    onFinishGestureAnimation()

    visibility = View.GONE
  }

  data class GestureAnimationData(
    val gestureContract: GestureContract,
    val bounds: RectF,
    val launcherIconData: LauncherIconData,
    val useMonochromeIcons: Boolean,
  )
}
