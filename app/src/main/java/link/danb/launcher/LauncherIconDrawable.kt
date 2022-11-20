package link.danb.launcher

import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette

/** Drawable for rendering icons in a custom shape. */
class LauncherIconDrawable(private val icon: Drawable) : Drawable() {

    private val palette: Palette by lazy {
        Palette.from(icon.toBitmap()).generate()
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(getClipPath())

        if (icon is AdaptiveIconDrawable) {
            drawAdaptiveIcon(canvas, icon)
        } else {
            drawLegacyIcon(canvas, icon)
        }

        canvas.restore()
    }

    override fun getIntrinsicWidth(): Int = icon.intrinsicWidth
    override fun getIntrinsicHeight(): Int = icon.intrinsicHeight

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSPARENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int = PixelFormat.TRANSPARENT

    private fun getClipPath() = Path().apply {
        addRoundRect(
            RectF(bounds),
            bounds.width() * RADIUS_FRACTION,
            bounds.height() * RADIUS_FRACTION,
            Path.Direction.CW
        )
    }

    private fun drawAdaptiveIcon(canvas: Canvas, icon: AdaptiveIconDrawable) {
        if (icon.background != null) {
            icon.background.bounds = getAdaptiveBounds()
            icon.background.draw(canvas)
        }

        if (icon.foreground != null) {
            icon.foreground.bounds = getAdaptiveBounds()
            icon.foreground.draw(canvas)
        }
    }

    private fun getAdaptiveBounds() = Rect(bounds).apply {
        inset(getAdaptiveInset(width()), getAdaptiveInset(height()))
    }

    private fun getAdaptiveInset(dimension: Int) =
        (AdaptiveIconDrawable.getExtraInsetFraction() * dimension * -1).toInt()

    private fun drawLegacyIcon(canvas: Canvas, icon: Drawable) {
        val background = GradientDrawable()
        background.setColor(palette.getDominantColor(0))
        background.bounds = Rect(bounds)
        background.draw(canvas)

        icon.bounds = getLegacyBounds()
        icon.draw(canvas)
    }

    private fun getLegacyBounds() = Rect(bounds).apply {
        inset(getLegacyInset(width()), getLegacyInset(height()))
    }

    private fun getLegacyInset(dimension: Int) = (LEGACY_INSET_FRACTION * dimension).toInt()

    companion object {
        private const val RADIUS_FRACTION = 0.25f
        private const val LEGACY_INSET_FRACTION = 0.1f
    }
}
