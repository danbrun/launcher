package link.danb.launcher

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val widgetViewModel: WidgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycle.addObserver(widgetViewModel)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.activity_frame, AppListFragment())
            .commit()
    }

    override fun onBackPressed() {
        // Intercept back button.
    }
}
