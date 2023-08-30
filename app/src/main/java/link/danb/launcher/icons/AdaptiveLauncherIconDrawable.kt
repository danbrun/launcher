package link.danb.launcher.icons

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

class AdaptiveLauncherIconDrawable(private val icon: AdaptiveIconDrawable) : Drawable() {

    private val path: Path = Path()
    private val rect: Rect = Rect()

    override fun draw(canvas: Canvas) {
        canvas.save()

        path.reset()
        path.addRoundRect(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            bounds.width() * RADIUS_FRACTION,
            bounds.height() * RADIUS_FRACTION,
            Path.Direction.CW
        )
        canvas.clipPath(path)

        rect.set(bounds)
        rect.inset(getAdaptiveInset(rect.width()), getAdaptiveInset(rect.height()))

        if (icon.background != null) {
            icon.background.bounds = rect
            icon.background.draw(canvas)
        }

        if (icon.foreground != null) {
            icon.foreground.bounds = rect
            icon.foreground.draw(canvas)
        }

        canvas.restore()
    }

    override fun getIntrinsicWidth(): Int = icon.intrinsicWidth
    override fun getIntrinsicHeight(): Int = icon.intrinsicHeight

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated(
        "Deprecated in Java", ReplaceWith("PixelFormat.TRANSPARENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int = PixelFormat.TRANSPARENT

    private fun getAdaptiveInset(dimension: Int) =
        (AdaptiveIconDrawable.getExtraInsetFraction() * dimension * -1).toInt()

    companion object {
        private const val RADIUS_FRACTION = 0.25f
    }
}
