package link.danb.launcher

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources

class LauncherIconDrawable(private val context: Context, appItem: AppItem) :
    Drawable() {

    private val size: Int = context.resources.getDimension(R.dimen.launcher_icon_size).toInt()
    private val radius: Float = context.resources.getDimension(R.dimen.launcher_icon_radius)
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmap: Bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

    private val defaultBackground: Drawable by lazy {
        AppCompatResources.getDrawable(context, R.drawable.launcher_icon_background)!!
    }

    init {
        val icon = appItem.info.getIcon(0)
        val canvas = Canvas(bitmap)

        if (icon is AdaptiveIconDrawable) {
            val padding = (size * AdaptiveIconDrawable.getExtraInsetFraction()).toInt()
            val bounds = Rect(-padding, -padding, size + padding, size + padding)

            icon.background.run {
                setBounds(bounds)
                draw(canvas)
            }
            icon.foreground.run {
                setBounds(bounds)
                draw(canvas)
            }
        } else {
            val padding = context.resources.getDimension(R.dimen.launcher_icon_padding).toInt()

            defaultBackground.run {
                setBounds(0, 0, size, size)
                draw(canvas)
            }
            icon.run {
                setBounds(padding, padding, size - padding, size - padding)
                draw(canvas)
            }
        }

        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    override fun getIntrinsicWidth(): Int = size
    override fun getIntrinsicHeight(): Int = size

    override fun draw(canvas: Canvas) {
        canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), radius, radius, paint)
    }

    override fun setAlpha(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun setColorFilter(p0: ColorFilter?) {
        TODO("Not yet implemented")
    }

    override fun getOpacity(): Int {
        TODO("Not yet implemented")
    }
}
