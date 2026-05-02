package com.example.datausagemonitor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.datausagemonitor.AppUsageInfo
import com.example.datausagemonitor.R
import com.google.android.material.button.MaterialButton


class AppsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_apps, container, false)
        
        setupRecyclerView(view)
        setupSorting(view)
        
        return view
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_apps)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        val sampleData = listOf(
            AppUsageInfo("YouTube", "com.google.android.youtube", 1200000000L, 50000000L),
            AppUsageInfo("Chrome", "com.android.chrome", 850000000L, 20000000L),
            AppUsageInfo("Instagram", "com.instagram.android", 620000000L, 30000000L),
            AppUsageInfo("WhatsApp", "com.whatsapp", 240000000L, 10000000L),
            AppUsageInfo("TikTok", "com.zhiliaoapp.musically", 190000000L, 5000000L)
        )
        
        recyclerView.adapter = AppUsageAdapter(sampleData)
    }

    private fun setupSorting(view: View) {
        val btnSort = view.findViewById<MaterialButton>(R.id.btn_sort)
        btnSort.setOnClickListener {
            val popup = PopupMenu(context, btnSort)
            popup.menu.add("Highest Usage")
            popup.menu.add("Lowest Usage")
            popup.menu.add("Alphabetical")
            popup.show()
        }
    }
}


