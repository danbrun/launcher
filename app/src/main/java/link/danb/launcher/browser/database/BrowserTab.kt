package link.danb.launcher.browser.database

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.io.ByteArrayOutputStream

@Entity
@TypeConverters(BitmapTypeConverters::class)
data class BrowserTab(
    @PrimaryKey(autoGenerate = true) val tabId: Int = 0,
    val url: String = "https://www.google.com",
    val title: String = "Google",
    val progress: Float = 0f,
    val bitmap: Bitmap? = null,
)

data class BrowserUrl(val tabId: Int, val url: String)

data class BrowserTitle(val tabId: Int, val title: String)

data class BrowserProgress(val tabId: Int, val progress: Float)

@TypeConverters(BitmapTypeConverters::class)
data class BrowserBitmap(val tabId: Int, val bitmap: Bitmap?)

class BitmapTypeConverters {
  @TypeConverter
  fun toByteArray(bitmap: Bitmap?): ByteArray? {
    if (bitmap == null) return null
    return ByteArrayOutputStream()
        .apply { bitmap.compress(Bitmap.CompressFormat.WEBP, 80, this) }
        .toByteArray()
  }

  @TypeConverter
  fun fromByteArray(byteArray: ByteArray?): Bitmap? {
    if (byteArray == null) return null
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
  }
}
