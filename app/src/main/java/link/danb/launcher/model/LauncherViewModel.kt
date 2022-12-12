package link.danb.launcher.model

import android.app.Application
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.UserHandle
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.danb.launcher.utils.getLocationOnScreen
import link.danb.launcher.utils.makeClipRevealAnimation
import javax.inject.Inject

/** View model for launch icons. */
@HiltViewModel
class LauncherViewModel @Inject constructor(application: Application) :
    AndroidViewModel(application) {

    @Inject
    lateinit var launcherDatabase: LauncherDatabase

    private val launcherActivityMetadata by lazy {
        launcherDatabase.launcherActivityMetadata()
    }

    private val launcherApps = application.getSystemService(LauncherApps::class.java)
    private val launcherAppsCallback = LauncherAppsCallback()

    private val mutableLauncherActivities = MutableStateFlow<List<LauncherActivityData>>(listOf())
    val launcherActivities: StateFlow<List<LauncherActivityData>> = mutableLauncherActivities

    val filter: MutableStateFlow<LauncherActivityFilter> =
        MutableStateFlow(LauncherActivityFilter.PERSONAL)

    val filteredLauncherActivities =
        launcherActivities.combine(filter) { launcherActivities, filter ->
            launcherActivities.filter { filter.function(this, it) }
        }

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mutableLauncherActivities.emit(launcherApps.profiles.flatMap {
                    launcherApps.getActivityList(
                        null, it
                    )
                }.map {
                    LauncherActivityData(getApplication(), it, launcherActivityMetadata.get(it))
                })
            }
        }

        launcherApps.registerCallback(launcherAppsCallback)
    }

    override fun onCleared() {
        super.onCleared()

        launcherApps.unregisterCallback(launcherAppsCallback)
    }

    /** Launches the given activity. */
    fun launch(launcherActivityData: LauncherActivityData, view: View) {
        launcherApps.startMainActivity(
            launcherActivityData.component,
            launcherActivityData.user,
            view.getLocationOnScreen(),
            view.makeClipRevealAnimation()
        )
    }

    /** Launches application settings for the given activity. */
    fun manage(launcherActivityData: LauncherActivityData, view: View) {
        launcherApps.startAppDetailsActivity(
            launcherActivityData.component,
            launcherActivityData.user,
            view.getLocationOnScreen(),
            view.makeClipRevealAnimation()
        )
    }

    /** Launches an application uninstall dialog for the given activity. */
    fun uninstall(launcherActivityData: LauncherActivityData, view: View) {
        view.context.startActivity(
            Intent(Intent.ACTION_DELETE).setData(Uri.parse("package:${launcherActivityData.component.packageName}"))
                .putExtra(Intent.EXTRA_USER, launcherActivityData.user)
        )
    }

    /** Sets the list of tags to associate with the given [LauncherActivityData] */
    fun updateTags(launcherActivityData: LauncherActivityData, tags: Set<String>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                launcherActivityMetadata.put(
                    LauncherActivityMetadata(
                        launcherActivityData.component, launcherActivityData.user, tags
                    )
                )
                update(launcherActivityData.component.packageName, launcherActivityData.user)
            }
        }
    }

    /** Sets the visibility of the given app. */
    fun setVisibility(launcherActivityData: LauncherActivityData, isVisible: Boolean) {
        if (isVisible) {
            updateTags(launcherActivityData, launcherActivityData.tags.minus(HIDDEN_TAG))
        } else {
            updateTags(launcherActivityData, launcherActivityData.tags.plus(HIDDEN_TAG))
        }
    }

    /** Returns true if the given app is visible. */
    fun isVisible(launcherActivityData: LauncherActivityData): Boolean {
        return !launcherActivityData.tags.contains(HIDDEN_TAG)
    }

    private fun update(packageName: String, user: UserHandle) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mutableLauncherActivities.value.toMutableList().apply {
                    removeIf { it.component.packageName == packageName && it.user == user }
                    addAll(launcherApps.getActivityList(packageName, user).map {
                        LauncherActivityData(
                            getApplication(), it, launcherActivityMetadata.get(it)
                        )
                    })
                    mutableLauncherActivities.emit(toList())
                }
            }
        }
    }

    inner class LauncherAppsCallback : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            updateSinglePackage(packageName, user)
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            updateSinglePackage(packageName, user)
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            updateSinglePackage(packageName, user)
        }

        override fun onPackagesAvailable(
            packageName: Array<out String>?, user: UserHandle?, replacing: Boolean
        ) {
            updateMultiplePackages(packageName, user)
        }

        override fun onPackagesUnavailable(
            packageName: Array<out String>?, user: UserHandle?, replacing: Boolean
        ) {
            updateMultiplePackages(packageName, user)
        }

        private fun updateSinglePackage(packageName: String?, user: UserHandle?) {
            if (packageName != null && user != null) {
                update(packageName, user)
            }
        }

        private fun updateMultiplePackages(packageName: Array<out String>?, user: UserHandle?) {
            if (user != null) {
                packageName?.forEach {
                    update(it, user)
                }
            }
        }
    }

    companion object {
        private const val HIDDEN_TAG = "hidden"
    }
}
