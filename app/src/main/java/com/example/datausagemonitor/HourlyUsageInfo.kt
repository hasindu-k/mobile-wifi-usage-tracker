package com.example.datausagemonitor

data class HourlyUsageInfo(
    val startTime: Long,
    val endTime: Long,
    val receivedBytes: Long,
    val transmittedBytes: Long
) {
    val totalBytes: Long
        get() = receivedBytes + transmittedBytes
}