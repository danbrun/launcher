package link.danb.launcher

import android.app.Application
import android.content.pm.LauncherActivityInfo
import android.graphics.*
import android.graphics.drawable.*
import android.os.Process
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class ActivityIconViewModel(application: Application) : AndroidViewModel(application) {

    private val mutableModel: MutableLiveData<ActivityIconViewModel> = MutableLiveData(this)
    val observableModel: LiveData<ActivityIconViewModel> get() = mutableModel

    private val iconMap: ConcurrentHashMap<LauncherActivityInfo, Drawable> = ConcurrentHashMap()
    private val iconTimestampMap: ConcurrentHashMap<LauncherActivityInfo, Long> =
        ConcurrentHashMap()

    private val placeholder: Drawable by lazy {
        AppCompatResources.getDrawable(application, R.drawable.launcher_icon_background)!!
    }

    fun getIcon(activityInfo: LauncherActivityInfo): Drawable {
        if (iconMap.containsKey(activityInfo)) {
            return iconMap[activityInfo]!!
        }

        // Insert placeholder drawable until load completes.
        iconMap[activityInfo] = placeholder
        iconTimestampMap[activityInfo] = System.currentTimeMillis()

        // TODO: store in LruCache instead of a HashMap?
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val resources = getApplication<Application>().resources
                iconMap[activityInfo] = BitmapDrawable(resources, renderIcon(activityInfo))
                iconTimestampMap[activityInfo] = System.currentTimeMillis()
                mutableModel.postValue(this@ActivityIconViewModel)
            }
        }

        return placeholder
    }

    fun getIconTimestamp(activityInfo: LauncherActivityInfo): Long {
        return iconTimestampMap[activityInfo] ?: 0
    }

    private val iconSize: Int by lazy {
        application.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
    }
    private val iconRadius: Int by lazy {
        application.resources.getDimensionPixelSize(R.dimen.launcher_icon_radius)
    }
    private val iconPadding: Int by lazy {
        application.resources.getDimensionPixelSize(R.dimen.launcher_icon_padding)
    }
    private val workBadge: Drawable by lazy {
        AppCompatResources.getDrawable(
            application,
            R.drawable.launcher_icon_badge
        )!!
    }

    private fun renderIcon(info: LauncherActivityInfo): Bitmap {
        val icon = info.getIcon(0)
        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).also {
            it.clipPath(Path().apply {
                addRoundRect(
                    0f,
                    0f,
                    iconSize.toFloat(),
                    iconSize.toFloat(),
                    iconRadius.toFloat(),
                    iconRadius.toFloat(),
                    Path.Direction.CW
                )
            })
        }

        if (icon is AdaptiveIconDrawable) {
            val padding = (iconSize * AdaptiveIconDrawable.getExtraInsetFraction()).toInt()
            val adaptiveIconBounds =
                Rect(-padding, -padding, iconSize + padding, iconSize + padding)

            listOf(icon.background, icon.foreground).forEach {
                it.bounds = adaptiveIconBounds
                it.draw(canvas)
            }
        } else {
            GradientDrawable().run {
                setColor(Palette.from(icon.toBitmap()).generate().getDominantColor(0))
                setBounds(0, 0, iconSize, iconSize)
                draw(canvas)
            }
            icon.run {
                setBounds(
                    iconPadding,
                    iconPadding,
                    iconSize - iconPadding,
                    iconSize - iconPadding
                )
                draw(canvas)
            }
        }

        if (info.user != Process.myUserHandle()) {
            workBadge.apply {
                setBounds(0, 0, iconSize, iconSize)
                draw(canvas)
            }
        }

        return bitmap
    }
}
