package com.example.datausagemonitor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.datausagemonitor.fragments.AppsFragment
import com.example.datausagemonitor.fragments.DashboardFragment
import com.example.datausagemonitor.fragments.LimitsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.navigation_apps -> {
                    loadFragment(AppsFragment())
                    true
                }
                R.id.navigation_limits -> {
                    loadFragment(LimitsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}