package link.danb.launcher.widgets

import android.appwidget.AppWidgetProviderInfo
import android.os.Parcel
import android.os.Parcelable

/** Represents an allocated widget ID and corresponding widget provider. */
data class WidgetHandle(val id: Int, val info: AppWidgetProviderInfo) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        AppWidgetProviderInfo.CREATOR.createFromParcel(parcel)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        info.writeToParcel(parcel, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WidgetHandle> {
        override fun createFromParcel(parcel: Parcel): WidgetHandle {
            return WidgetHandle(parcel)
        }

        override fun newArray(size: Int): Array<WidgetHandle?> {
            return arrayOfNulls(size)
        }
    }
}
