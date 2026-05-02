package com.example.datausagemonitor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.datausagemonitor.R
import com.example.datausagemonitor.UsagePermissionHelper

class PermissionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_permission, container, false)

        view.findViewById<Button>(R.id.btn_grant_permission).setOnClickListener {
            UsagePermissionHelper.openUsageAccessSettings(requireContext())
        }

        return view
    }
}
