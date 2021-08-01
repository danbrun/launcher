package link.danb.launcher

import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.activity_frame, DualUserAppListFragment())
            .commit()
    }
}