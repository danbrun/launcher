package link.danb.launcher.ui

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.toRectF

class InvertedCornerDrawable(private val radius: Int) : Drawable() {

    private val blackPaint = Paint().apply { color = Color.BLACK }
    private val eraserPaint: Paint =
        Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(bounds, blackPaint)
        canvas.drawRoundRect(bounds.toRectF(), radius.toFloat(), radius.toFloat(), eraserPaint)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated(
        "Deprecated in Java", ReplaceWith("PixelFormat.TRANSPARENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int = PixelFormat.TRANSPARENT
}
