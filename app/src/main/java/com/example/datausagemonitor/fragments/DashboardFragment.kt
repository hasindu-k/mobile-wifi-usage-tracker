package com.example.datausagemonitor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.datausagemonitor.ByteFormatter
import com.example.datausagemonitor.DataUsageRepository
import com.example.datausagemonitor.HourlyUsageInfo
import com.example.datausagemonitor.R
import com.example.datausagemonitor.UsagePermissionHelper
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread


class DashboardFragment : Fragment() {

    private data class UsageBlock(val bytes: Long, val label: String)

    private lateinit var repository: DataUsageRepository
    private var currentStartTime: Long = 0
    private var currentEndTime: Long = 0
    private var currentPeriodLabel: String = "today"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        repository = DataUsageRepository(requireContext())

        // Initialize with Today
        setRangeToday()

        setupDatePicker(view)
        setupFilterChips(view)
        loadData(view)

        return view
    }

    override fun onResume() {
        super.onResume()
        // Refresh with current range
        view?.let { loadData(it) }
    }

    private fun setupFilterChips(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_history)
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_today -> setRangeToday()
                R.id.chip_yesterday -> setRangeYesterday()
                R.id.chip_week -> setRangeWeek()
                R.id.chip_month -> setRangeMonth()
            }
            loadData(view)
        }
    }

    private fun setRangeToday() {
        val cal = Calendar.getInstance()
        currentEndTime = cal.timeInMillis
        
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        currentStartTime = cal.timeInMillis
        currentPeriodLabel = "Today"
    }


    private fun setRangeYesterday() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        currentEndTime = cal.timeInMillis // Start of today is end of yesterday
        
        cal.add(Calendar.DAY_OF_YEAR, -1)
        currentStartTime = cal.timeInMillis
        currentPeriodLabel = "Yesterday"
    }


    private fun setRangeWeek() {
        val cal = Calendar.getInstance()
        currentEndTime = cal.timeInMillis
        
        cal.add(Calendar.DAY_OF_YEAR, -7)
        currentStartTime = cal.timeInMillis
        currentPeriodLabel = "This Week"
    }


    private fun setRangeMonth() {
        val cal = Calendar.getInstance()
        currentEndTime = cal.timeInMillis
        
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        currentStartTime = cal.timeInMillis
        currentPeriodLabel = "This Month"
    }


    private fun loadData(view: View) {
        if (!UsagePermissionHelper.hasUsageStatsPermission(requireContext())) {
            return
        }

        val loader = view.findViewById<View>(R.id.loader)
        val content = view.findViewById<View>(R.id.dashboard_content)
        
        activity?.runOnUiThread {
            loader.visibility = View.VISIBLE
            content.visibility = View.INVISIBLE
        }

        thread {
            try {
                val wifi = repository.getWifiUsage(currentStartTime, currentEndTime)
                val mobile = repository.getMobileUsage(currentStartTime, currentEndTime)
                val hotspot = repository.getHotspotUsage(currentStartTime, currentEndTime)
                
                val hourlyWifi = repository.getWifiHourlyUsage(currentStartTime, currentEndTime)
                val hourlyMobile = repository.getMobileHourlyUsage(currentStartTime, currentEndTime)

                val monthlyWifi = repository.getMonthlyWifiUsage()
                val monthlyMobile = repository.getMonthlyMobileUsage()

                activity?.runOnUiThread {
                    updateSummaryCards(view, wifi, mobile, monthlyWifi, monthlyMobile)
                    updateChart(view, hourlyWifi, hourlyMobile)
                    updatePeakUsage(view, hourlyWifi, hourlyMobile)
                    
                    // Update Hotspot
                    view.findViewById<TextView>(R.id.tv_hotspot_today).text = ByteFormatter.format(hotspot)
                    view.findViewById<TextView>(R.id.label_hotspot_period).text = "Hotspot $currentPeriodLabel"

                    // Update labels
                    view.findViewById<TextView>(R.id.tv_today_total).text =
                        ByteFormatter.format(wifi + mobile)
                    view.findViewById<TextView>(R.id.tv_total_period).text = currentPeriodLabel
                    
                    view.findViewById<TextView>(R.id.label_wifi_period).text = "Wi-Fi $currentPeriodLabel"
                    view.findViewById<TextView>(R.id.label_mobile_period).text = "Mobile $currentPeriodLabel"
                    updateSectionLabels(view)

                    loader.visibility = View.GONE
                    content.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    loader.visibility = View.GONE
                    content.visibility = View.VISIBLE
                    Toast.makeText(context, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun updateSummaryCards(view: View, todayWifi: Long, todayMobile: Long, monthlyWifi: Long, monthlyMobile: Long) {
        view.findViewById<TextView>(R.id.tv_today_total).text =
            ByteFormatter.format(todayWifi + todayMobile)
        view.findViewById<TextView>(R.id.tv_total_period).text = currentPeriodLabel
        
        view.findViewById<TextView>(R.id.tv_wifi_today).text = ByteFormatter.format(todayWifi)
        view.findViewById<TextView>(R.id.tv_mobile_today).text = ByteFormatter.format(todayMobile)
        
        view.findViewById<TextView>(R.id.tv_monthly_wifi).text = ByteFormatter.format(monthlyWifi)
        view.findViewById<TextView>(R.id.tv_monthly_mobile).text = ByteFormatter.format(monthlyMobile)

        // Update Comparison
        val totalToday = todayWifi + todayMobile
        if (totalToday > 0) {
            val wifiPercent = (todayWifi * 100 / totalToday).toInt()
            val mobilePercent = 100 - wifiPercent
            
            view.findViewById<LinearProgressIndicator>(R.id.progress_comparison).progress = wifiPercent
            view.findViewById<TextView>(R.id.tv_wifi_percentage).text = "Wi-Fi: $wifiPercent%"
            view.findViewById<TextView>(R.id.tv_mobile_percentage).text = "Mobile: $mobilePercent%"
        }
    }

    private fun updateChart(view: View, hourlyWifi: List<HourlyUsageInfo>, hourlyMobile: List<HourlyUsageInfo>) {
        val bars = listOf(
            view.findViewById<View>(R.id.bar1),
            view.findViewById<View>(R.id.bar2),
            view.findViewById<View>(R.id.bar3),
            view.findViewById<View>(R.id.bar4),
            view.findViewById<View>(R.id.bar5),
            view.findViewById<View>(R.id.bar6),
            view.findViewById<View>(R.id.bar7),
            view.findViewById<View>(R.id.bar8)
        )

        // Combine Wi-Fi + mobile usage by hour, or by day for longer periods.
        val maxSize = maxOf(hourlyWifi.size, hourlyMobile.size)

        val hourlyUsage = (0 until maxSize).map { index ->
            val wifiBytes = hourlyWifi.getOrNull(index)?.totalBytes ?: 0L
            val mobileBytes = hourlyMobile.getOrNull(index)?.totalBytes ?: 0L
            wifiBytes + mobileBytes
        }

        val usagePoints = if (isLongPeriod()) {
            val dailyUsage = linkedMapOf<Long, Long>()
            (0 until maxSize).forEach { index ->
                val info = hourlyWifi.getOrNull(index) ?: hourlyMobile.getOrNull(index)
                if (info != null) {
                    val dayStart = Calendar.getInstance().apply {
                        timeInMillis = info.startTime
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    dailyUsage[dayStart] = (dailyUsage[dayStart] ?: 0L) + hourlyUsage[index]
                }
            }
            dailyUsage.toSortedMap().map { (dayStart, bytes) ->
                Triple(dayStart, dayStart + 24 * 60 * 60 * 1000L, bytes)
            }
        } else {
            (0 until maxSize).mapNotNull { index ->
                val info = hourlyWifi.getOrNull(index) ?: hourlyMobile.getOrNull(index)
                info?.let { Triple(it.startTime, it.endTime, hourlyUsage[index]) }
            }
        }

        val blockSize = kotlin.math.ceil(usagePoints.size / 8.0).toInt().coerceAtLeast(1)
        val usageBlocks = mutableListOf<UsageBlock>()

        for (i in 0 until 8) {
            val start = i * blockSize
            val end = ((i + 1) * blockSize).coerceAtMost(usagePoints.size)

            if (start < usagePoints.size) {
                val points = usagePoints.subList(start, end)
                val blockSum = points.sumOf { it.third }
                val label = formatChartRange(points.first().first, points.last().second)
                usageBlocks.add(UsageBlock(blockSum, label))
            } else {
                usageBlocks.add(UsageBlock(0L, "No usage recorded"))
            }
        }

        val maxUsage = usageBlocks.maxOfOrNull { it.bytes }?.coerceAtLeast(1L) ?: 1L

        bars.forEachIndexed { index, bar ->
            val block = usageBlocks.getOrNull(index) ?: UsageBlock(0L, "No usage recorded")
            val usage = block.bytes
            val heightPercent = usage.toFloat() / maxUsage.toFloat()

            val layoutParams = bar.layoutParams
            layoutParams.height = (heightPercent * 400).toInt().coerceAtLeast(10)
            bar.layoutParams = layoutParams

            bar.setOnClickListener {
                Toast.makeText(
                    context,
                    "${block.label}: ${ByteFormatter.format(usage)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun isLongPeriod(): Boolean {
        return currentEndTime - currentStartTime > 24 * 60 * 60 * 1000L
    }

    private fun formatChartRange(startTime: Long, endTime: Long): String {
        val pattern = if (isLongPeriod()) "MMM dd" else "hh:mm a"
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return "${formatter.format(Date(startTime))} - ${formatter.format(Date(endTime))}"
    }

    private fun updateSectionLabels(view: View) {
        val chartLabel = if (isLongPeriod()) {
            "Daily Usage $currentPeriodLabel"
        } else {
            "Hourly Usage $currentPeriodLabel"
        }
        val peakLabel = if (isLongPeriod()) {
            "Peak Usage Period $currentPeriodLabel"
        } else {
            "Peak Usage Time $currentPeriodLabel"
        }

        view.findViewById<TextView>(R.id.label_hourly_usage).text = chartLabel
        view.findViewById<TextView>(R.id.label_comparison).text =
            "Wi-Fi vs Mobile $currentPeriodLabel"
        view.findViewById<TextView>(R.id.label_peak_usage).text = peakLabel
    }

    private fun updatePeakUsage(view: View, hourlyWifi: List<HourlyUsageInfo>, hourlyMobile: List<HourlyUsageInfo>) {
        val peakWifi = hourlyWifi.maxByOrNull { it.totalBytes }
        val peakMobile = hourlyMobile.maxByOrNull { it.totalBytes }

        view.findViewById<TextView>(R.id.tv_peak_wifi_time).text = formatTimeRange(peakWifi)
        view.findViewById<TextView>(R.id.tv_peak_wifi_usage).text = "${ByteFormatter.format(peakWifi?.totalBytes ?: 0L)} used"

        view.findViewById<TextView>(R.id.tv_peak_mobile_time).text = formatTimeRange(peakMobile)
        view.findViewById<TextView>(R.id.tv_peak_mobile_usage).text = "${ByteFormatter.format(peakMobile?.totalBytes ?: 0L)} used"
    }

    private fun formatTimeRange(info: HourlyUsageInfo?): String {
        if (info == null || info.totalBytes == 0L) return "No usage recorded"
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return "${sdf.format(Date(info.startTime))} - ${sdf.format(Date(info.endTime))}"
    }

    private fun setupDatePicker(view: View) {
        view.findViewById<ImageButton>(R.id.btn_custom_date).setOnClickListener {
            val constraintsBuilder = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())

            val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .setSelection(androidx.core.util.Pair(currentStartTime, currentEndTime))
                .setCalendarConstraints(constraintsBuilder.build())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val cal = Calendar.getInstance()
                
                // Clear chip selection
                view.findViewById<ChipGroup>(R.id.chip_group_history).clearCheck()

                cal.timeInMillis = selection.first
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                currentStartTime = cal.timeInMillis
                
                cal.timeInMillis = selection.second
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                currentEndTime = cal.timeInMillis
                
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                val startStr = sdf.format(Date(currentStartTime))
                val endStr = sdf.format(Date(currentEndTime))
                
                currentPeriodLabel = if (startStr == endStr) {
                    "on $startStr"
                } else {
                    "$startStr - $endStr"
                }
                
                loadData(view)
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }
}
