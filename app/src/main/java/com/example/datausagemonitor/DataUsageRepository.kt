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

    fun getTodayHotspotUsage(): Long {
        return getTetheringUsage(
            startTime = getStartOfToday(),
            endTime = System.currentTimeMillis()
        )
    }

    fun getMonthlyHotspotUsage(): Long {
        return getTetheringUsage(
            startTime = getStartOfMonth(),
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

    fun getHotspotUsage(startTime: Long, endTime: Long): Long {
        return getTetheringUsage(startTime, endTime)
    }

    
    // ... (keep existing code)

    private fun getTetheringUsage(startTime: Long, endTime: Long): Long {
        var total = 0L
        var networkStats: NetworkStats? = null
        try {
            networkStats = networkStatsManager.querySummary(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                endTime
            )
            val bucket = NetworkStats.Bucket()
            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)
                // Uid -5 is often used for tethering in some Android versions, 
                // but checking for UID_TETHERING is more reliable if available.
                // Alternatively, we check if the UID is NetworkStats.Bucket.UID_TETHERING
                if (bucket.uid == NetworkStats.Bucket.UID_TETHERING) {
                    total += bucket.rxBytes + bucket.txBytes
                }
            }
        } catch (e: Exception) {
            // Fallback for older versions or specific ROMs
        } finally {
            networkStats?.close()
        }
        return total
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

    fun getWifiAppUsage(startTime: Long, endTime: Long): List<AppUsageInfo> {
        return getAppWiseUsage(
            networkType = ConnectivityManager.TYPE_WIFI,
            startTime = startTime,
            endTime = endTime
        )
    }

    fun getMobileAppUsage(startTime: Long, endTime: Long): List<AppUsageInfo> {
        return getAppWiseUsage(
            networkType = ConnectivityManager.TYPE_MOBILE,
            startTime = startTime,
            endTime = endTime
        )
    }

    fun getTodayHourlyWifiUsage(): List<HourlyUsageInfo> {
        return getHourlyUsage(
            networkType = ConnectivityManager.TYPE_WIFI,
            startTime = getStartOfToday(),
            endTime = System.currentTimeMillis()
        )
    }

    fun getTodayHourlyMobileUsage(): List<HourlyUsageInfo> {
        return getHourlyUsage(
            networkType = ConnectivityManager.TYPE_MOBILE,
            startTime = getStartOfToday(),
            endTime = System.currentTimeMillis()
        )
    }

    fun getWifiUsage(startTime: Long, endTime: Long): Long {
        return getDeviceUsage(ConnectivityManager.TYPE_WIFI, startTime, endTime)
    }

    fun getMobileUsage(startTime: Long, endTime: Long): Long {
        return getDeviceUsage(ConnectivityManager.TYPE_MOBILE, startTime, endTime)
    }

    fun getWifiHourlyUsage(startTime: Long, endTime: Long): List<HourlyUsageInfo> {
        return getHourlyUsage(ConnectivityManager.TYPE_WIFI, startTime, endTime)
    }

    fun getMobileHourlyUsage(startTime: Long, endTime: Long): List<HourlyUsageInfo> {
        return getHourlyUsage(ConnectivityManager.TYPE_MOBILE, startTime, endTime)
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

    private fun getHourlyUsage(
        networkType: Int,
        startTime: Long,
        endTime: Long
    ): List<HourlyUsageInfo> {
        val hourlyMap = linkedMapOf<Long, Pair<Long, Long>>()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startTime

        while (calendar.timeInMillis < endTime) {
            hourlyMap[calendar.timeInMillis] = Pair(0L, 0L)
            calendar.add(Calendar.HOUR_OF_DAY, 1)
        }

        var networkStats: NetworkStats? = null

        try {
            networkStats = networkStatsManager.queryDetails(
                networkType,
                null,
                startTime,
                endTime
            )

            val bucket = NetworkStats.Bucket()

            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)

                val bucketStartHour = getStartOfHour(bucket.startTimeStamp)
                val currentValue = hourlyMap[bucketStartHour] ?: Pair(0L, 0L)

                val updatedRx = currentValue.first + bucket.rxBytes
                val updatedTx = currentValue.second + bucket.txBytes

                hourlyMap[bucketStartHour] = Pair(updatedRx, updatedTx)
            }

        } catch (e: Exception) {
            return emptyList()

        } finally {
            networkStats?.close()
        }

        return hourlyMap.map { entry ->
            val rxBytes = entry.value.first
            val txBytes = entry.value.second

            HourlyUsageInfo(
                startTime = entry.key,
                endTime = entry.key + ONE_HOUR_IN_MILLIS,
                receivedBytes = rxBytes,
                transmittedBytes = txBytes
            )
        }
    }

    private fun getStartOfHour(timeMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMillis

        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
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

    companion object {
        private const val ONE_HOUR_IN_MILLIS = 60 * 60 * 1000L
    }
}