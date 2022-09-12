package link.danb.launcher

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import link.danb.launcher.widgets.WidgetViewModel

class LauncherActivity : AppCompatActivity() {

    private val widgetViewModel: WidgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.launcher_activity)

        lifecycle.addObserver(widgetViewModel)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.activity_frame, LauncherFragment())
                .commit()
        }
    }

    override fun onBackPressed() {
        // Intercept back button.
    }
}
