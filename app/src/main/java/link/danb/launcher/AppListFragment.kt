package link.danb.launcher

import android.os.Bundle
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val ARG_USER_HANDLE = "user_handle"

class AppListFragment : Fragment() {

    private var userHandle: UserHandle? = null

    private val appViewModel: AppViewModel by activityViewModels()
    private val appItemList = ArrayList<AppItem>()
    private val adapter = AppItem.Adapter(appItemList, 3)

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

        val recyclerView = view.findViewById<RecyclerView>(R.id.app_list)
        recyclerView.adapter = adapter

        onAppViewModelUpdate(appViewModel.apps.value)
        appViewModel.apps.observe(viewLifecycleOwner, { onAppViewModelUpdate(it) })

        return view
    }

    private fun onAppViewModelUpdate(apps: List<AppItem>?) {
        appItemList.apply {
            clear()
            if (apps != null) {
                if (userHandle == null) {
                    addAll(apps)
                } else {
                    addAll(apps.filter { it.user == userHandle })
                }
                sortBy { it.name.value.lowercase() }
            }
        }
        adapter.notifyDataSetChanged()
    }

    companion object {
        @JvmStatic fun newInstance(userHandle: UserHandle) =
            AppListFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_USER_HANDLE, userHandle)
                }
            }
    }
}