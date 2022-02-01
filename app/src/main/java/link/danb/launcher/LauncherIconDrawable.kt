package link.danb.launcher

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import kotlin.math.roundToInt

class LauncherIconDrawable(private val context: Context, appItem: AppItem) :
    Drawable() {

    private val size: Float = context.resources.getDimension(R.dimen.launcher_icon_size)
    private val sizeInt: Int = size.roundToInt()
    private val radius: Float = context.resources.getDimension(R.dimen.launcher_icon_radius)
    private val foregroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val foreground: Bitmap = Bitmap.createBitmap(sizeInt, sizeInt, Bitmap.Config.ARGB_8888)
    private val background: Bitmap = Bitmap.createBitmap(sizeInt, sizeInt, Bitmap.Config.ARGB_8888)

    private val defaultBackground: Drawable by lazy {
        AppCompatResources.getDrawable(context, R.drawable.launcher_icon_background)!!
    }

    init {
        val icon = appItem.info.getIcon(0)
        if (icon is AdaptiveIconDrawable) {
            render(icon.foreground, foreground)
            render(icon.background, background)
        } else {
            render(icon, foreground)
            render(defaultBackground, background)
        }

        foregroundPaint.shader =
            BitmapShader(foreground, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        backgroundPaint.shader =
            BitmapShader(background, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    override fun getIntrinsicWidth(): Int = sizeInt
    override fun getIntrinsicHeight(): Int = sizeInt

    override fun draw(canvas: Canvas) {
        canvas.drawRoundRect(0f, 0f, size, size, radius, radius, backgroundPaint)
        canvas.drawRoundRect(0f, 0f, size, size, radius, radius, foregroundPaint)
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

    private fun render(drawable: Drawable, bitmap: Bitmap) {
        drawable.run {
            setBounds(0, 0, sizeInt, sizeInt)
            draw(Canvas(bitmap))
        }
    }
}
