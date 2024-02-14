package link.danb.launcher

import android.app.role.RoleManager
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LauncherActivity : AppCompatActivity() {

  private val roleManager: RoleManager by lazy { getSystemService<RoleManager>()!! }

  private val setHomeActivityResultLauncher =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    } else {
      null
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContentView(R.layout.launcher_activity)

    if (savedInstanceState == null) {
      supportFragmentManager
        .beginTransaction()
        .replace(R.id.activity_frame, LauncherFragment())
        .commit()
    }

    onBackPressedDispatcher.addCallback(
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          // Do not close app.
        }
      }
    )

    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
        !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
    ) {
      setHomeActivityResultLauncher?.launch(
        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
      )
    }
  }
}
