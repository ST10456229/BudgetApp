package com.example.budget_app.utils

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.budget_app.*

object NavigationHelper {

    fun setupNavigation(activity: Activity) {
        val navHome = activity.findViewById<View>(R.id.navHome)
        val navReports = activity.findViewById<View>(R.id.navReports)
        val navHistory = activity.findViewById<View>(R.id.navHistory)
        val navProfile = activity.findViewById<View>(R.id.navProfile)
        val fabAdd = activity.findViewById<View>(R.id.fabAdd)

        navHome?.setOnClickListener {
            if (activity !is MainActivity) {
                activity.startActivity(Intent(activity, MainActivity::class.java))
                if (activity !is MainActivity) activity.finish()
            }
        }

        navReports?.setOnClickListener {
            if (activity !is ReportsActivity) {
                activity.startActivity(Intent(activity, ReportsActivity::class.java))
                if (activity !is MainActivity) activity.finish()
            }
        }

        navHistory?.setOnClickListener {
            if (activity !is TransactionHistoryActivity) {
                activity.startActivity(Intent(activity, TransactionHistoryActivity::class.java))
                if (activity !is MainActivity) activity.finish()
            }
        }

        navProfile?.setOnClickListener {
            if (activity !is ProfileActivity) {
                activity.startActivity(Intent(activity, ProfileActivity::class.java))
                if (activity !is MainActivity) activity.finish()
            }
        }

        fabAdd?.setOnClickListener {
            // Only navigate if we're not already in AddExpenseActivity
            if (activity !is AddExpenseActivity) {
                activity.startActivity(Intent(activity, AddExpenseActivity::class.java))
            }
        }

        updateNavItems(activity)
    }

    private fun updateNavItems(activity: Activity) {
        val blue = ContextCompat.getColor(activity, R.color.primary_blue)
        val gray = ContextCompat.getColor(activity, android.R.color.darker_gray)

        when (activity) {
            is MainActivity -> setActive(activity, R.id.navIconHome, R.id.navLabelHome, blue)
            is ReportsActivity -> setActive(activity, R.id.navIconReports, R.id.navLabelReports, blue)
            is TransactionHistoryActivity -> setActive(activity, R.id.navIconHistory, R.id.navLabelHistory, blue)
            is ProfileActivity -> setActive(activity, R.id.navIconProfile, R.id.navLabelProfile, blue)
        }
    }

    private fun setActive(activity: Activity, iconId: Int, labelId: Int, color: Int) {
        activity.findViewById<ImageView>(iconId)?.setColorFilter(color)
        activity.findViewById<TextView>(labelId)?.setTextColor(color)
    }
}
