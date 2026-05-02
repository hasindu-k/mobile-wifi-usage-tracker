package com.example.datausagemonitor.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_limits")
data class UsageLimit(
    @PrimaryKey val networkType: Int, // ConnectivityManager.TYPE_WIFI or TYPE_MOBILE
    val limitBytes: Long,
    val warningPercentage: Int = 80,
    val isEnabled: Boolean = true
)
