package com.example.datausagemonitor.monitoring

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.datausagemonitor.DataUsageRepository
import android.net.ConnectivityManager

import com.example.datausagemonitor.db.AppDatabase
import kotlinx.coroutines.runBlocking

class UsageMonitorWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val repository = DataUsageRepository(context)
    private val database = AppDatabase.getDatabase(context)

    override fun doWork(): Result {
        Log.d("UsageMonitorWorker", "Background check started...")

        return runBlocking {
            try {
                val wifiLimit = database.usageLimitDao().getLimitForNetwork(ConnectivityManager.TYPE_WIFI)
                val mobileLimit = database.usageLimitDao().getLimitForNetwork(ConnectivityManager.TYPE_MOBILE)

                val monthlyWifi = repository.getMonthlyWifiUsage()
                val monthlyMobile = repository.getMonthlyMobileUsage()

                checkAndLogLimit(wifiLimit, monthlyWifi, "Wi-Fi")
                checkAndLogLimit(mobileLimit, monthlyMobile, "Mobile")

                Result.success()
            } catch (e: Exception) {
                Log.e("UsageMonitorWorker", "Error checking usage", e)
                Result.retry()
            }
        }
    }

    private fun checkAndLogLimit(limit: com.example.datausagemonitor.db.UsageLimit?, used: Long, label: String) {
        if (limit == null || !limit.isEnabled || limit.limitBytes == 0L) return

        val percent = (used.toFloat() / limit.limitBytes.toFloat()) * 100
        if (percent >= 100) {
            Log.w("UsageMonitorWorker", "ALERT: $label usage exceeded limit! ($percent%)")
        } else if (percent >= limit.warningPercentage) {
            Log.w("UsageMonitorWorker", "WARNING: $label usage close to limit. ($percent%)")
        }
    }
}

