package link.danb.launcher.icons

import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Drawable for rendering icons in a custom shape. */
class LegacyLauncherIconDrawable private constructor(
    private val icon: Drawable, private val palette: Palette
) : Drawable() {

    private val background: GradientDrawable = GradientDrawable().apply {
        setColor(palette.getMutedColor(Color.WHITE))
    }
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
        background.bounds = rect
        background.draw(canvas)

        rect.inset(getLegacyInset(rect.width()), getLegacyInset(rect.height()))
        icon.bounds = rect
        icon.draw(canvas)

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

    private fun getLegacyInset(dimension: Int) = (LEGACY_INSET_FRACTION * dimension).toInt()

    companion object {
        private const val RADIUS_FRACTION = 0.25f
        private const val LEGACY_INSET_FRACTION = 0.1f

        suspend fun create(icon: Drawable): LegacyLauncherIconDrawable = withContext(Dispatchers.IO) {
            LegacyLauncherIconDrawable(
                icon, Palette.from(icon.toBitmap()).generate()
            )
        }
    }
}
