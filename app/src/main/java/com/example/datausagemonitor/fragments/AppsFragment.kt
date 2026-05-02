package com.example.datausagemonitor.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.datausagemonitor.AppUsageInfo
import com.example.datausagemonitor.DataUsageRepository
import com.example.datausagemonitor.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlin.concurrent.thread

class AppsFragment : Fragment() {

    private lateinit var repository: DataUsageRepository
    private lateinit var adapter: AppUsageAdapter
    private var allApps = listOf<AppUsageInfo>()
    private var isWifiView = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_apps, container, false)
        repository = DataUsageRepository(requireContext())
        
        setupRecyclerView(view)
        setupToggle(view)
        setupSearch(view)
        setupSorting(view)
        
        loadData(view)
        
        return view
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_apps)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = AppUsageAdapter(emptyList())
        recyclerView.adapter = adapter
    }

    private fun setupToggle(view: View) {
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggle_group)
        toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                isWifiView = checkedId == R.id.btn_wifi
                loadData(view)
            }
        }
    }

    private fun setupSearch(view: View) {
        val etSearch = view.findViewById<EditText>(R.id.et_search)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadData(root: View) {
        val loader = root.findViewById<View>(R.id.loader)
        val content = root.findViewById<View>(R.id.apps_content)
        
        activity?.runOnUiThread {
            loader?.visibility = View.VISIBLE
            content?.visibility = View.INVISIBLE
        }

        thread {
            try {
                val data = if (isWifiView) {
                    repository.getTodayWifiAppUsage()
                } else {
                    repository.getTodayMobileAppUsage()
                }
                
                activity?.runOnUiThread {
                    if (isAdded) {
                        allApps = data
                        adapter.updateData(allApps)
                        loader?.visibility = View.GONE
                        content?.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    if (isAdded) {
                        loader?.visibility = View.GONE
                        content?.visibility = View.VISIBLE
                        Toast.makeText(context, "Error loading apps: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }




    private fun filterApps(query: String) {
        val filtered = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter { it.appName.contains(query, ignoreCase = true) }
        }
        adapter.updateData(filtered)
    }

    private fun setupSorting(view: View) {
        val btnSort = view.findViewById<MaterialButton>(R.id.btn_sort)
        btnSort.setOnClickListener {
            val popup = PopupMenu(context, btnSort)
            popup.menu.add("Highest Usage")
            popup.menu.add("Lowest Usage")
            popup.menu.add("Alphabetical")
            
            popup.setOnMenuItemClickListener { item ->
                val sorted = when (item.title) {
                    "Highest Usage" -> allApps.sortedByDescending { it.totalBytes }
                    "Lowest Usage" -> allApps.sortedBy { it.totalBytes }
                    "Alphabetical" -> allApps.sortedBy { it.appName }
                    else -> allApps
                }
                allApps = sorted
                adapter.updateData(allApps)
                true
            }
            popup.show()
        }
    }
}



