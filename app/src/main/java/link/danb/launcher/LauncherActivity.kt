package link.danb.launcher

import android.app.role.RoleManager
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.fragment.app.commitNow
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LauncherActivity : AppCompatActivity() {

  private val setHomeActivityResultLauncher =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    } else {
      null
    }

  private var hasRequestedHomeRole: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContentView(R.layout.launcher_activity)

    if (savedInstanceState == null) {
      supportFragmentManager.commitNow { replace(R.id.activity_frame, LauncherFragment()) }
    } else {
      hasRequestedHomeRole = savedInstanceState.getBoolean(EXTRA_HAS_REQUESTED_HOME_ROLE)
    }

    onBackPressedDispatcher.addCallback(
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          // Do not close app.
        }
      }
    )
  }

  override fun onResume() {
    super.onResume()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasRequestedHomeRole) {
      hasRequestedHomeRole = true
      requestHomeRole()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)

    outState.putBoolean(EXTRA_HAS_REQUESTED_HOME_ROLE, hasRequestedHomeRole)
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private fun requestHomeRole() {
    val roleManager: RoleManager = getSystemService() ?: return
    if (
      roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
        !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
    ) {
      setHomeActivityResultLauncher?.launch(
        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
      )
    }
  }

  companion object {
    private const val EXTRA_HAS_REQUESTED_HOME_ROLE = "extra_has_requested_home_role"
  }
}
