package link.danb.launcher

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

class DualUserAppListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_dual_user_app_list, container, false)

        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
        viewPager.adapter = Adapter(this)

        return view
    }

    class Adapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        private val launcherApps =
            fragment
                .requireContext()
                .getSystemService(Context.LAUNCHER_APPS_SERVICE)
                    as LauncherApps

        override fun getItemCount() = launcherApps.profiles.size

        override fun createFragment(position: Int): Fragment {
            return AppListFragment.newInstance(launcherApps.profiles[position])
        }
    }
}