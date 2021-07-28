package link.danb.launcher

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_app_list, container, false)

        val context = requireContext()

        val recyclerView = view.findViewById<RecyclerView>(R.id.app_list)
        recyclerView.setOnApplyWindowInsetsListener { _, insets ->
            val systemInsets =
                WindowInsetsCompat
                    .toWindowInsetsCompat(insets, recyclerView)
                    .getInsets(WindowInsetsCompat.Type.systemBars())
            recyclerView.updatePadding(top = systemInsets.top, bottom = systemInsets.bottom)
            insets
        }

        val launcherApps =
            requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        val appsList = ArrayList<AppItem>()
        val adapter = AppItem.Adapter(appsList, 3)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        val updateApps = {
            appsList.clear()
            appsList.addAll(getLauncherApps(launcherApps))
            adapter.notifyDataSetChanged()
        }

        updateApps()

        launcherApps.registerCallback(
            object : LauncherApps.Callback() {
                override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                    updateApps()
                }

                override fun onPackageAdded(packageName: String?, user: UserHandle?) {
                    updateApps()
                }

                override fun onPackageChanged(packageName: String?, user: UserHandle?) {
                    updateApps()
                }

                override fun onPackagesAvailable(
                    packageNames: Array<out String>?,
                    user: UserHandle?,
                    replacing: Boolean
                ) {
                    TODO("Not yet implemented")
                }

                override fun onPackagesUnavailable(
                    packageNames: Array<out String>?,
                    user: UserHandle?,
                    replacing: Boolean
                ) {
                    TODO("Not yet implemented")
                }

            }
        )

        return view
    }

    private fun getLauncherApps(launcherApps: LauncherApps): List<AppItem> {

        return launcherApps.profiles
            .flatMap { launcherApps.getActivityList(null, it) }
            .map {
                AppItem(it)
            }
            .sortedBy { it.name.value.lowercase() }
    }
}