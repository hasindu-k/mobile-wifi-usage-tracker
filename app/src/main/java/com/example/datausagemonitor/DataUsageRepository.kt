package com.example.datausagemonitor

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import java.util.Calendar

class DataUsageRepository(private val context: Context) {

    private val networkStatsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

    fun getTodayWifiUsage(): Long {
        return getDeviceUsage(
            networkType = ConnectivityManager.TYPE_WIFI,
            startTime = getStartOfToday(),
            endTime = System.currentTimeMillis()
        )
    }

    fun getMonthlyWifiUsage(): Long {
        return getDeviceUsage(
            networkType = ConnectivityManager.TYPE_WIFI,
            startTime = getStartOfMonth(),
            endTime = System.currentTimeMillis()
        )
    }

    fun getTodayMobileUsage(): Long {
        return getDeviceUsage(
            networkType = ConnectivityManager.TYPE_MOBILE,
            startTime = getStartOfToday(),
            endTime = System.currentTimeMillis()
        )
    }

    fun getMonthlyMobileUsage(): Long {
        return getDeviceUsage(
            networkType = ConnectivityManager.TYPE_MOBILE,
            startTime = getStartOfMonth(),
            endTime = System.currentTimeMillis()
        )
    }

    fun getTodayWifiAppUsage(): List<AppUsageInfo> {
        return getAppWiseUsage(
            networkType = ConnectivityManager.TYPE_WIFI,
            startTime = getStartOfToday(),
            endTime = System.currentTimeMillis()
        )
    }

    fun getTodayMobileAppUsage(): List<AppUsageInfo> {
        return getAppWiseUsage(
            networkType = ConnectivityManager.TYPE_MOBILE,
            startTime = getStartOfToday(),
            endTime = System.currentTimeMillis()
        )
    }

    private fun getDeviceUsage(
        networkType: Int,
        startTime: Long,
        endTime: Long
    ): Long {
        return try {
            val bucket = networkStatsManager.querySummaryForDevice(
                networkType,
                null,
                startTime,
                endTime
            )

            bucket.rxBytes + bucket.txBytes

        } catch (e: Exception) {
            0L
        }
    }

    private fun getAppWiseUsage(
        networkType: Int,
        startTime: Long,
        endTime: Long
    ): List<AppUsageInfo> {
        val usageList = mutableListOf<AppUsageInfo>()
        var networkStats: NetworkStats? = null

        try {
            networkStats = networkStatsManager.querySummary(
                networkType,
                null,
                startTime,
                endTime
            )

            val bucket = NetworkStats.Bucket()

            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)

                val uid = bucket.uid
                val rxBytes = bucket.rxBytes
                val txBytes = bucket.txBytes
                val totalBytes = rxBytes + txBytes

                if (totalBytes <= 0) {
                    continue
                }

                val packageNames = context.packageManager.getPackagesForUid(uid)

                if (!packageNames.isNullOrEmpty()) {
                    val packageName = packageNames[0]
                    val appName = getAppName(packageName)

                    usageList.add(
                        AppUsageInfo(
                            appName = appName,
                            packageName = packageName,
                            receivedBytes = rxBytes,
                            transmittedBytes = txBytes
                        )
                    )
                }
            }

        } catch (e: Exception) {
            return emptyList()

        } finally {
            networkStats?.close()
        }

        return usageList
            .groupBy { it.packageName }
            .map { entry ->
                val firstItem = entry.value.first()

                AppUsageInfo(
                    appName = firstItem.appName,
                    packageName = firstItem.packageName,
                    receivedBytes = entry.value.sumOf { it.receivedBytes },
                    transmittedBytes = entry.value.sumOf { it.transmittedBytes }
                )
            }
            .sortedByDescending { it.totalBytes }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val applicationInfo: ApplicationInfo =
                context.packageManager.getApplicationInfo(packageName, 0)

            context.packageManager
                .getApplicationLabel(applicationInfo)
                .toString()

        } catch (e: Exception) {
            packageName
        }
    }

    private fun getStartOfToday(): Long {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }

    private fun getStartOfMonth(): Long {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }
}