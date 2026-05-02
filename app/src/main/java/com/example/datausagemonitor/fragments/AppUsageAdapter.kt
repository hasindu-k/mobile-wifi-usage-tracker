package com.example.datausagemonitor.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.datausagemonitor.AppUsageInfo
import com.example.datausagemonitor.ByteFormatter
import com.example.datausagemonitor.R
import com.google.android.material.progressindicator.LinearProgressIndicator

class AppUsageAdapter(private var apps: List<AppUsageInfo>) :
    RecyclerView.Adapter<AppUsageAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.tv_app_name)
        val usageAmount: TextView = view.findViewById(R.id.tv_usage_amount)
        val progress: LinearProgressIndicator = view.findViewById(R.id.progress_usage)
        val icon: ImageView = view.findViewById(R.id.iv_app_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.appName
        holder.usageAmount.text = ByteFormatter.format(app.totalBytes)
        
        // Mock progress for demo
        val maxUsage = apps.firstOrNull()?.totalBytes ?: 1L
        holder.progress.progress = ((app.totalBytes.toFloat() / maxUsage.toFloat()) * 100).toInt()
    }

    override fun getItemCount() = apps.size

    fun updateData(newApps: List<AppUsageInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
