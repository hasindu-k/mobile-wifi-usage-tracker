package com.example.datausagemonitor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.datausagemonitor.R
import android.net.ConnectivityManager

import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.datausagemonitor.ByteFormatter
import com.example.datausagemonitor.DataUsageRepository
import com.example.datausagemonitor.db.AppDatabase
import com.example.datausagemonitor.db.UsageLimit
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class LimitsFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var repository: DataUsageRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_limits, container, false)
        database = AppDatabase.getDatabase(requireContext())
        repository = DataUsageRepository(requireContext())

        setupUI(view)
        observeLimits(view)

        return view
    }

    private fun setupUI(view: View) {
        // Load actual monthly usage
        thread {
            val wifiUsed = repository.getMonthlyWifiUsage()
            val mobileUsed = repository.getMonthlyMobileUsage()

            activity?.runOnUiThread {
                view.findViewById<TextView>(R.id.tv_wifi_used_val).text = "Used: ${ByteFormatter.format(wifiUsed)}"
                view.findViewById<TextView>(R.id.tv_mobile_used_val).text = "Used: ${ByteFormatter.format(mobileUsed)}"
            }
        }

        view.findViewById<MaterialButton>(R.id.btn_set_wifi_limit).setOnClickListener {
            showSetLimitDialog(ConnectivityManager.TYPE_WIFI)
        }

        view.findViewById<MaterialButton>(R.id.btn_set_mobile_limit).setOnClickListener {
            showSetLimitDialog(ConnectivityManager.TYPE_MOBILE)
        }
    }

    private fun observeLimits(view: View) {
        lifecycleScope.launch {
            database.usageLimitDao().getAllLimits().collect { limits ->
                val wifiUsed = repository.getMonthlyWifiUsage() // Ideally cached
                val mobileUsed = repository.getMonthlyMobileUsage()

                limits.forEach { limit ->
                    if (limit.networkType == ConnectivityManager.TYPE_WIFI) {
                        updateLimitUI(view, R.id.tv_wifi_limit_val, R.id.progress_wifi_limit, limit, wifiUsed)
                    } else {
                        updateLimitUI(view, R.id.tv_mobile_limit_val, R.id.progress_mobile_limit, limit, mobileUsed)
                    }
                }
            }
        }
    }

    private fun updateLimitUI(view: View, textId: Int, progressId: Int, limit: UsageLimit, used: Long) {
        view.findViewById<TextView>(textId).text = ByteFormatter.format(limit.limitBytes)
        val progress = if (limit.limitBytes > 0) ((used.toFloat() / limit.limitBytes.toFloat()) * 100).toInt() else 0
        view.findViewById<LinearProgressIndicator>(progressId).progress = progress.coerceIn(0, 100)
        
        if (progress >= limit.warningPercentage) {
            view.findViewById<View>(R.id.card_warning).visibility = View.VISIBLE
        }
    }

    private fun showSetLimitDialog(networkType: Int) {
        val input = EditText(requireContext())
        input.hint = "Limit in GB"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        AlertDialog.Builder(requireContext())
            .setTitle("Set Monthly Limit")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val gbValue = input.text.toString().toDoubleOrNull() ?: 0.0
                val bytes = (gbValue * 1024 * 1024 * 1024).toLong()
                saveLimit(networkType, bytes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveLimit(networkType: Int, bytes: Long) {
        lifecycleScope.launch {
            database.usageLimitDao().insertOrUpdateLimit(
                UsageLimit(networkType, bytes)
            )
            Toast.makeText(context, "Limit saved!", Toast.LENGTH_SHORT).show()
        }
    }
}

