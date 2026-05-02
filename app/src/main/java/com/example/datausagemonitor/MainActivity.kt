package com.example.datausagemonitor

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

import androidx.fragment.app.Fragment
import com.example.datausagemonitor.fragments.AppsFragment
import com.example.datausagemonitor.fragments.DashboardFragment
import com.example.datausagemonitor.fragments.LimitsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

import com.example.datausagemonitor.fragments.PermissionFragment
import com.example.datausagemonitor.monitoring.MonitorManager

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupNavigation()
        checkPermissionAndLoad()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndLoad()
    }

    private fun checkPermissionAndLoad() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        if (!UsagePermissionHelper.hasUsageStatsPermission(this)) {
            loadFragment(PermissionFragment())
            bottomNavigation.visibility = View.GONE
        } else {
            bottomNavigation.visibility = View.VISIBLE
            // Start background monitoring
            MonitorManager.startMonitoring(this)
            
            // Only load Dashboard if we were on the Permission screen
            val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            if (currentFragment is PermissionFragment || currentFragment == null) {
                loadFragment(DashboardFragment())
                bottomNavigation.selectedItemId = R.id.navigation_dashboard
            }
        }

    }

    private fun setupNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
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