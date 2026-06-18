package com.example.budget_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Redundant activity. Use AddExpenseActivity instead.
 * Overwritten to prevent build failures.
 */
class Addexpense : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Redirecting or just finishing if started by mistake
        finish()
    }
}
