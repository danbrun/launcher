package link.danb.launcher.database

import androidx.room.TypeConverter
import link.danb.launcher.profiles.Profile

class ProfileConverter {
  @TypeConverter
  fun toInt(profile: Profile): Int =
    when (profile) {
      Profile.PERSONAL -> 1
      Profile.WORK -> 2
      Profile.PRIVATE -> 3
    }

  @TypeConverter
  fun toProfile(int: Int): Profile =
    when (int) {
      1 -> Profile.PERSONAL
      2 -> Profile.WORK
      3 -> Profile.PRIVATE
      else -> throw IllegalArgumentException()
    }
}
