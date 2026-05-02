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
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class AppsFragment : Fragment() {

    private lateinit var repository: DataUsageRepository
    private lateinit var adapter: AppUsageAdapter
    private var allApps = listOf<AppUsageInfo>()
    private var isWifiView = true
    private var selectedStartMillis: Long = getStartOfToday()
    private var selectedEndMillis: Long = getEndOfToday()
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    private fun getStartOfToday(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfToday(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

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
        setupDatePicker(view)
        
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

    private fun setupDatePicker(view: View) {
        val btnDate = view.findViewById<MaterialButton>(R.id.btn_date)
        btnDate.setOnClickListener {
            val constraintsBuilder = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())

            val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .setSelection(
                    androidx.core.util.Pair(selectedStartMillis, selectedEndMillis)
                )
                .setCalendarConstraints(constraintsBuilder.build())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val cal = Calendar.getInstance()
                
                cal.timeInMillis = selection.first
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                selectedStartMillis = cal.timeInMillis

                cal.timeInMillis = selection.second
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                selectedEndMillis = cal.timeInMillis

                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                val startStr = sdf.format(Date(selectedStartMillis))
                val endStr = sdf.format(Date(selectedEndMillis))

                if (startStr == endStr) {
                    btnDate.text = if (selectedStartMillis == getStartOfToday()) "Today" else startStr
                } else {
                    btnDate.text = "$startStr - $endStr"
                }

                loadData(view)
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
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
                val actualEndTime = if (selectedEndMillis > System.currentTimeMillis()) System.currentTimeMillis() else selectedEndMillis
                
                val data = if (isWifiView) {
                    repository.getWifiAppUsage(selectedStartMillis, actualEndTime)
                } else {
                    repository.getMobileAppUsage(selectedStartMillis, actualEndTime)
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



