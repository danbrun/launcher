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

class AppListFragment : Fragment() {

    private val appViewModel: AppViewModel by activityViewModels()
    private val appItemList = ArrayList<AppItem>()
    private val adapter = AppItem.Adapter(appItemList, 3)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_app_list, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.app_list)
        recyclerView.setOnApplyWindowInsetsListener { _, insets ->
            val systemInsets =
                WindowInsetsCompat
                    .toWindowInsetsCompat(insets, recyclerView)
                    .getInsets(WindowInsetsCompat.Type.systemBars())
            recyclerView.updatePadding(top = systemInsets.top, bottom = systemInsets.bottom)
            insets
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        onAppViewModelUpdate(appViewModel.apps.value)
        appViewModel.apps.observe(viewLifecycleOwner, { onAppViewModelUpdate(it) })

        return view
    }

    private fun onAppViewModelUpdate(apps: List<AppItem>?) {
        appItemList.apply {
            clear()
            if (apps != null) {
                addAll(apps)
                sortBy { it.name.value.lowercase() }
            }
        }
        adapter.notifyDataSetChanged()
    }
}