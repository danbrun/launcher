package link.danb.launcher

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.appcompat.content.res.AppCompatResources

class LauncherIconDrawable(
    private val icon: Drawable,
    private val radius: Float,
    private val padding: Int,
    private val getBackground: () -> Drawable?,
    private val getWorkBadge: () -> Drawable?
) :
    Drawable() {

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        canvas.drawRoundRect(
            0f, 0f, bounds.width().toFloat(), bounds.height().toFloat(), radius, radius, paint
        )
    }

    override fun onBoundsChange(bounds: Rect?) {
        if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) {
            return
        }

        val width = bounds.width()
        val height = bounds.height()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val canvas = Canvas(bitmap)

        if (icon is AdaptiveIconDrawable) {
            val horizontalPadding = (width * AdaptiveIconDrawable.getExtraInsetFraction()).toInt()
            val verticalPadding = (height * AdaptiveIconDrawable.getExtraInsetFraction()).toInt()
            val adaptiveIconBounds = Rect(
                -horizontalPadding,
                -verticalPadding,
                width + horizontalPadding,
                height + verticalPadding
            )

            icon.background.run {
                setBounds(adaptiveIconBounds)
                draw(canvas)
            }
            icon.foreground.run {
                setBounds(adaptiveIconBounds)
                draw(canvas)
            }
        } else {
            getBackground()?.run {
                setBounds(0, 0, width, height)
                draw(canvas)
            }
            icon.run {
                setBounds(padding, padding, width - padding, height - padding)
                draw(canvas)
            }
        }

        getWorkBadge()?.run {
            setBounds(0, 0, width, height)
            draw(canvas)
        }
    }

    override fun getIntrinsicWidth(): Int = icon.intrinsicWidth
    override fun getIntrinsicHeight(): Int = icon.intrinsicHeight

    override fun setAlpha(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun setColorFilter(p0: ColorFilter?) {
        TODO("Not yet implemented")
    }

    override fun getOpacity(): Int {
        TODO("Not yet implemented")
    }

    companion object {
        fun get(context: Context, info: LauncherActivityInfo): LauncherIconDrawable =
            LauncherIconDrawable(
                info.getIcon(0),
                context.resources.getDimension(R.dimen.launcher_icon_radius),
                context.resources.getDimension(R.dimen.launcher_icon_padding).toInt(),
                { AppCompatResources.getDrawable(context, R.drawable.launcher_icon_background) },
                {
                    if (info.user != Process.myUserHandle())
                        AppCompatResources.getDrawable(
                            context,
                            R.drawable.launcher_icon_badge
                        ) else null
                }
            )
    }
}
