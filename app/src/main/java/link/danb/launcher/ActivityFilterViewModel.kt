package link.danb.launcher

import android.app.Application
import android.content.pm.LauncherActivityInfo
import android.os.Process
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ActivityFilterViewModel(application: Application) : AndroidViewModel(application) {

    private val mutableFilter: MutableLiveData<ActivityFilter> =
        MutableLiveData(ActivityFilter.PERSONAL)
    val filter: LiveData<ActivityFilter> get() = mutableFilter

    fun setFilter(filter: ActivityFilter) {
        mutableFilter.value = filter
    }

    data class ActivityFilter(
        @StringRes val nameResId: Int,
        val filterFunction: (LauncherActivityInfo) -> Boolean
    ) {
        companion object {
            val ALL = ActivityFilter(R.string.all) { true }
            val PERSONAL = ActivityFilter(R.string.personal) { it.user == Process.myUserHandle() }
            val WORK = ActivityFilter(R.string.work) { it.user != Process.myUserHandle() }
        }
    }

}
