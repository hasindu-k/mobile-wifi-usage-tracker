package com.example.datausagemonitor

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var repository: DataUsageRepository

    private lateinit var todayWifiText: TextView
    private lateinit var monthlyWifiText: TextView
    private lateinit var todayMobileText: TextView
    private lateinit var monthlyMobileText: TextView
    private lateinit var wifiAppsText: TextView
    private lateinit var mobileAppsText: TextView
    private lateinit var statusText: TextView

    private lateinit var peakWifiText: TextView
    private lateinit var peakMobileText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = DataUsageRepository(this)

        setupUi()
    }

    override fun onResume() {
        super.onResume()

        if (UsagePermissionHelper.hasUsageStatsPermission(this)) {
            statusText.text = "Permission Status: Usage Access Enabled"
        } else {
            statusText.text = "Permission Status: Usage Access Not Enabled"
        }
    }

    private fun setupUi() {
        val scrollView = ScrollView(this)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 40)
        }

        val titleText = TextView(this).apply {
            text = "Data Usage Monitor"
            textSize = 26f
            gravity = Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "Permission Status: Checking..."
            textSize = 16f
            setPadding(0, 24, 0, 16)
        }

        val permissionButton = Button(this).apply {
            text = "Open Usage Access Settings"
        }

        val loadButton = Button(this).apply {
            text = "Load Data Usage"
        }

        todayWifiText = createResultText("Today's Wi-Fi Usage: Not loaded")
        monthlyWifiText = createResultText("Monthly Wi-Fi Usage: Not loaded")
        todayMobileText = createResultText("Today's Mobile Data Usage: Not loaded")
        monthlyMobileText = createResultText("Monthly Mobile Data Usage: Not loaded")

        peakWifiText = createResultText("Peak Wi-Fi Usage Time: Not loaded")
        peakMobileText = createResultText("Peak Mobile Data Usage Time: Not loaded")

        wifiAppsText = createSectionText("Top Wi-Fi Apps Today:\nNot loaded")
        mobileAppsText = createSectionText("Top Mobile Data Apps Today:\nNot loaded")

        container.addView(titleText)
        container.addView(statusText)
        container.addView(permissionButton)
        container.addView(loadButton)

        container.addView(todayWifiText)
        container.addView(monthlyWifiText)
        container.addView(todayMobileText)
        container.addView(monthlyMobileText)

        container.addView(peakWifiText)
        container.addView(peakMobileText)

        container.addView(wifiAppsText)
        container.addView(mobileAppsText)

        scrollView.addView(container)
        setContentView(scrollView)

        permissionButton.setOnClickListener {
            UsagePermissionHelper.openUsageAccessSettings(this)
        }

        loadButton.setOnClickListener {
            loadDataUsage()
        }
    }

    private fun createResultText(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 18f
            setPadding(0, 24, 0, 8)
        }
    }

    private fun createSectionText(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 15f
            setPadding(0, 32, 0, 8)
        }
    }

    private fun loadDataUsage() {
        if (!UsagePermissionHelper.hasUsageStatsPermission(this)) {
            Toast.makeText(
                this,
                "Please enable Usage Access permission first",
                Toast.LENGTH_LONG
            ).show()

            UsagePermissionHelper.openUsageAccessSettings(this)
            return
        }

        statusText.text = "Loading data usage..."

        thread {
            try {
                val todayWifiUsage = repository.getTodayWifiUsage()
                val monthlyWifiUsage = repository.getMonthlyWifiUsage()

                val todayMobileUsage = repository.getTodayMobileUsage()
                val monthlyMobileUsage = repository.getMonthlyMobileUsage()

                val wifiAppUsage = repository.getTodayWifiAppUsage()
                val mobileAppUsage = repository.getTodayMobileAppUsage()

                val topWifiAppsText = formatAppUsageList(wifiAppUsage)
                val topMobileAppsText = formatAppUsageList(mobileAppUsage)

                val hourlyWifiUsage = repository.getTodayHourlyWifiUsage()
                val hourlyMobileUsage = repository.getTodayHourlyMobileUsage()

                val peakWifiUsage = hourlyWifiUsage.maxByOrNull { it.totalBytes }
                val peakMobileUsage = hourlyMobileUsage.maxByOrNull { it.totalBytes }

                runOnUiThread {
                    todayWifiText.text =
                        "Today's Wi-Fi Usage: ${ByteFormatter.format(todayWifiUsage)}"

                    monthlyWifiText.text =
                        "Monthly Wi-Fi Usage: ${ByteFormatter.format(monthlyWifiUsage)}"

                    todayMobileText.text =
                        "Today's Mobile Data Usage: ${ByteFormatter.format(todayMobileUsage)}"

                    monthlyMobileText.text =
                        "Monthly Mobile Data Usage: ${ByteFormatter.format(monthlyMobileUsage)}"

                    wifiAppsText.text =
                        "Top Wi-Fi Apps Today:\n$topWifiAppsText"

                    mobileAppsText.text =
                        "Top Mobile Data Apps Today:\n$topMobileAppsText"

                    peakWifiText.text =
                        "Peak Wi-Fi Usage Time Today: ${formatPeakUsage(peakWifiUsage)}"

                    peakMobileText.text =
                        "Peak Mobile Data Usage Time Today: ${formatPeakUsage(peakMobileUsage)}"

                    statusText.text = "Data usage loaded successfully"
                }

            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Error loading data usage"
                    Toast.makeText(
                        this,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun formatAppUsageList(appUsageList: List<AppUsageInfo>): String {
        if (appUsageList.isEmpty()) {
            return "No app usage data found"
        }

        return appUsageList
            .take(10)
            .mapIndexed { index, appUsageInfo ->
                val totalUsage = ByteFormatter.format(appUsageInfo.totalBytes)

                "${index + 1}. ${appUsageInfo.appName} - $totalUsage"
            }
            .joinToString("\n")
    }

    private fun formatPeakUsage(hourlyUsageInfo: HourlyUsageInfo?): String {
        if (hourlyUsageInfo == null || hourlyUsageInfo.totalBytes <= 0) {
            return "No usage data found"
        }

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val startTime = timeFormat.format(Date(hourlyUsageInfo.startTime))
        val endTime = timeFormat.format(Date(hourlyUsageInfo.endTime))
        val usage = ByteFormatter.format(hourlyUsageInfo.totalBytes)

        return "$startTime - $endTime, Used: $usage"
    }
}