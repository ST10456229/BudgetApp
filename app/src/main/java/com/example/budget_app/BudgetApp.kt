package com.example.budget_app

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.database.FirebaseDatabase

class BudgetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase with persistent settings if needed, 
        // but here we just ensure the DB URL is accessible or just let activities handle it.
        // Actually, it's better to keep it in activities for now as they use an explicit URL.
        
        // Apply theme preference
        val sharedPrefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        if (sharedPrefs.contains("DarkMode")) {
            val isDarkMode = sharedPrefs.getBoolean("DarkMode", false)
            val mode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}
