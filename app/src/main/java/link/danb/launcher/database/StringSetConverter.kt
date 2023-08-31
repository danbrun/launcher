package link.danb.launcher.database

import androidx.room.TypeConverter
import org.json.JSONArray

class StringSetConverter {
  @TypeConverter fun toJson(stringSet: Set<String>): String = JSONArray(stringSet).toString()

  @TypeConverter
  fun toStringSet(json: String): Set<String> = buildSet {
    val array = JSONArray(json)
    for (i in 0 until array.length()) {
      add(array.getString(i))
    }
  }
}
