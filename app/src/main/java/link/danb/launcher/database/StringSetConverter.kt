package link.danb.launcher.database

import androidx.room.TypeConverter
import org.json.JSONArray

class StringSetConverter {
    @TypeConverter
    fun toJson(stringSet: Set<String>): String {
        return JSONArray(stringSet).toString()
    }

    @TypeConverter
    fun toStringSet(json: String): Set<String> {
        return buildSet {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                add(array.getString(i))
            }
        }
    }
}
