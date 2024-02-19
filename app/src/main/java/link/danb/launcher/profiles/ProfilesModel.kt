package link.danb.launcher.profiles

import android.os.Process.myUserHandle
import android.os.UserHandle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import link.danb.launcher.extensions.isPersonalProfile

@Singleton
class ProfilesModel @Inject constructor(private val workProfileManager: WorkProfileManager) {

  private val _activeProfile: MutableStateFlow<UserHandle> = MutableStateFlow(personalProfile)

  val activeProfile: StateFlow<UserHandle> = _activeProfile.asStateFlow()

  fun toggleActiveProfile(showWorkProfile: Boolean? = null) {
    _activeProfile.value =
      workProfileManager.workProfile?.takeIf {
        showWorkProfile ?: _activeProfile.value.isPersonalProfile
      } ?: personalProfile
  }

  companion object {
    val personalProfile: UserHandle = myUserHandle()
  }
}
