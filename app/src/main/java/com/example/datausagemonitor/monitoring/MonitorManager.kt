package com.example.datausagemonitor.monitoring

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object MonitorManager {
    private const val WORK_NAME = "UsageMonitoringWork"

    fun startMonitoring(context: Context) {
        val constraints = androidx.work.Constraints.Builder()
            .build()

        val workRequest = PeriodicWorkRequestBuilder<UsageMonitorWorker>(
            15, TimeUnit.MINUTES // Minimum interval allowed by Android
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun stopMonitoring(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
