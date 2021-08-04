package link.danb.launcher

import android.os.Bundle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val ARG_USER_HANDLE = "user_handle"

class AppListFragment : Fragment() {

    private var userHandle: UserHandle? = null

    private val appViewModel: AppViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userHandle = it.getParcelable(ARG_USER_HANDLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_app_list, container, false)

        val adapter = AppItem.Adapter()

        val recyclerView = view.findViewById<RecyclerView>(R.id.app_list)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(context, 3)

        appViewModel.apps.observe(viewLifecycleOwner, { appList ->
            val processedAppList: MutableList<AppItem> = ArrayList(appList)
            if (userHandle != null) {
                processedAppList.retainAll { it.user == userHandle }
            }
            processedAppList.sortBy { it.name.value.lowercase() }
            adapter.submitList(processedAppList)
        })

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(userHandle: UserHandle) =
            AppListFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_USER_HANDLE, userHandle)
                }
            }
    }
}