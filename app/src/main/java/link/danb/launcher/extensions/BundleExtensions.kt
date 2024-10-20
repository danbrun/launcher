package link.danb.launcher.extensions

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat

/** Convenience method to get a [Parcelable] from a [Bundle] using the appropriate API. */
inline fun <reified T> Bundle.getParcelableCompat(key: String): T? =
  BundleCompat.getParcelable(this, key, T::class.java)
