package com.example.datausagemonitor.monitoring

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.datausagemonitor.DataUsageRepository

class UsageMonitorWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val repository = DataUsageRepository(context)

    override fun doWork(): Result {
        Log.d("UsageMonitorWorker", "Background check started...")

        try {
            val todayWifi = repository.getTodayWifiUsage()
            val todayMobile = repository.getTodayMobileUsage()

            Log.d("UsageMonitorWorker", "Today's Usage - WiFi: $todayWifi, Mobile: $todayMobile")

            // Future: Check limits and trigger notifications here
            
            return Result.success()
        } catch (e: Exception) {
            Log.e("UsageMonitorWorker", "Error checking usage", e)
            return Result.retry()
        }
    }
}
