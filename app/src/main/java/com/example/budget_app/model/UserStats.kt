package com.example.budget_app.model

import com.google.firebase.database.Exclude

data class UserStats(
    var level: Int = 1,
    var xp: Int = 0,
    var totalXP: Int = 0,
    var currentStreak: Int = 0,
    var longestStreak: Int = 0,
    var lastActiveDate: Long = System.currentTimeMillis(),
    var totalSaved: Double = 0.0,
    var budgetsMetCount: Int = 0,
    var consecutiveMonthsUnderBudget: Int = 0,
    var lastComplianceCheckMonth: Int = -1, // Calendar.MONTH
    var lastComplianceCheckYear: Int = -1,   // Calendar.YEAR
    var streakLost: Boolean = false,
    
    // XP Farming Safeguards
    var lastXpAwardDate: Long = 0,
    var dailyXpEarned: Int = 0,
    
    // Achievement tracking
    var lastExpenseDate: Long = 0,
    var consecutiveExpenseDays: Int = 0,
    var totalTransactions: Int = 0,

    @Exclude @JvmField var levelTitleOverride: String? = null,
    @Exclude @JvmField var xpToNextLevelOverride: Int? = null
) {
    @get:Exclude
    val xpToNextLevel: Int
        get() = xpToNextLevelOverride ?: (100 * level)
    
    @get:Exclude
    val levelTitle: String
        get() = levelTitleOverride ?: when(level) {
            in 1..4 -> "Budget Rookie"
            in 5..9 -> "Money Saver"
            in 10..14 -> "Budget Master"
            in 15..19 -> "Wealth Guardian"
            else -> "Financial Legend"
        }
    
    @get:Exclude
    val progressToNextLevel: Float
        get() = if (xpToNextLevel > 0) xp.toFloat() / xpToNextLevel else 0f
}
