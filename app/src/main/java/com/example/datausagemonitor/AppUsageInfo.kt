package com.example.datausagemonitor

data class AppUsageInfo(
    val appName: String,
    val packageName: String,
    val receivedBytes: Long,
    val transmittedBytes: Long
) {
    val totalBytes: Long
        get() = receivedBytes + transmittedBytes
}