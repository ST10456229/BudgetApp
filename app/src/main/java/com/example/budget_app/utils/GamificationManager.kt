package com.example.budget_app.utils

import android.content.Context
import android.util.Log
import com.example.budget_app.model.Achievement
import com.example.budget_app.model.LevelConfig
import com.example.budget_app.model.UserStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * GamificationManager: Centralized service for levels, XP, achievements, and streaks.
 * Addresses all QA findings including XP farming protection, calendar streaks, 
 * and achievement implementation gaps.
 */
class GamificationManager private constructor(context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)
    private var userStats = UserStats()
    private val achievements = mutableMapOf<String, Achievement>()
    
    // XP Configuration
    private val DAILY_XP_CAP = 200 
    private val XP_PER_TRANSACTION = 10
    private val XP_GOAL_DEPOSIT = 15
    private val XP_BUDGET_CREATED = 20
    private val XP_GOAL_COMPLETED = 100
    private val XP_MONTHLY_BUDGET_MET = 50

    interface GamificationListener {
        fun onStatsUpdated(stats: UserStats)
        fun onLevelUp(newLevel: Int, title: String)
        fun onAchievementUnlocked(achievement: Achievement)
    }

    private val listeners = mutableListOf<GamificationListener>()

    fun addListener(listener: GamificationListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
        // Trigger initial update if stats already loaded
        if (userStats.totalXP >= 0) {
            listener.onStatsUpdated(userStats)
        }
    }

    fun removeListener(listener: GamificationListener) {
        listeners.remove(listener)
    }

    companion object {
        @Volatile
        private var INSTANCE: GamificationManager? = null

        fun getInstance(context: Context): GamificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GamificationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        const val PATH_GAMIFICATION = "gamification"
        const val PATH_STATS = "stats"
        const val PATH_BADGES = "badges"
    }

    init {
        initializeAchievements()
        loadUserStats()
    }

    private fun initializeAchievements() {
        val badgeList = listOf(
            Achievement("FIRST_EXPENSE", "First Step", "Add your first transaction", 0, "💰", 20),
            Achievement("7_DAY_STREAK", "On Fire!", "7 consecutive days of logging in", 0, "🔥", 50),
            Achievement("BUDGET_HERO", "Budget Hero", "Stay under budget 3 times", 0, "🛡️", 100),
            Achievement("SAVER_LEVEL_1", "Saver Level 1", "Save R1000 total", 0, "🏦", 100),
            Achievement("GOAL_CRUSHER", "Goal Crusher", "Complete your first savings goal", 0, "🎯", 100),
            Achievement("TRANSACTION_MILESTONE_10", "Getting Started", "Log 10 transactions", 0, "📊", 50),
            Achievement("TRANSACTION_MILESTONE_50", "Data Lover", "Log 50 transactions", 0, "❤️", 150),
            Achievement("NO_SPEND_DAY", "Discipline Master", "Complete a day with zero expenses", 0, "🧘", 30),
            Achievement("PERFECT_WEEK", "Perfect Week", "Log expenses every day for 7 days", 0, "✨", 100),
            Achievement("BUDGET_MASTER", "Budget Master", "Stay under budget for 3 consecutive months", 0, "👑", 250)
        )
        badgeList.forEach { achievements[it.id] = it }
    }

    private fun loadUserStats() {
        val userId = auth.currentUser?.uid ?: return
        val ref = database.getReference(Constants.PATH_USERS).child(userId).child(PATH_GAMIFICATION)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val statsSnapshot = snapshot.child(PATH_STATS)
                userStats = statsSnapshot.getValue(UserStats::class.java) ?: UserStats()
                
                val badgesSnapshot = snapshot.child(PATH_BADGES)
                for (badgeSnap in badgesSnapshot.children) {
                    val badgeId = badgeSnap.key ?: continue
                    val achieved = badgeSnap.child("achieved").getValue(Boolean::class.java) ?: false
                    val date = badgeSnap.child("achievedDate").getValue(Long::class.java) ?: 0L
                    achievements[badgeId]?.let {
                        it.achieved = achieved
                        it.achievedDate = date
                    }
                }
                
                userStats.xpToNextLevelOverride = LevelConfig.getXpToNextLevel(userStats.level)
                userStats.levelTitleOverride = LevelConfig.getLevelTitle(userStats.level)
                
                listeners.forEach { it.onStatsUpdated(userStats) }
                
                updateStreak()
                checkNoSpendDay()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GamificationManager", "Error loading stats: ${error.message}")
            }
        })
    }

    /**
     * Triggered when a transaction is added. Handles XP, farming protection, 
     * milestones, and "Perfect Week" logic. Optimized to use local counter.
     */
    fun onTransactionAdded() {
        awardXP(XP_PER_TRANSACTION, "Transaction Added")
        
        val userId = auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()
        
        // Update stats locally and in DB
        userStats.totalTransactions++
        
        // Perfect Week Logic
        if (!isSameDay(userStats.lastExpenseDate, now)) {
            val lastExpenseCal = Calendar.getInstance().apply { timeInMillis = userStats.lastExpenseDate }
            val nowCal = Calendar.getInstance().apply { timeInMillis = now }
            
            val d1 = Calendar.getInstance().apply { 
                set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val d2 = Calendar.getInstance().apply { 
                set(lastExpenseCal.get(Calendar.YEAR), lastExpenseCal.get(Calendar.MONTH), lastExpenseCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diff = TimeUnit.MILLISECONDS.toDays(d1.timeInMillis - d2.timeInMillis)
            
            if (diff == 1L) {
                userStats.consecutiveExpenseDays++
                if (userStats.consecutiveExpenseDays >= 7) {
                    unlockAchievement("PERFECT_WEEK")
                }
            } else if (diff > 1L || userStats.lastExpenseDate == 0L) {
                userStats.consecutiveExpenseDays = 1
            }
            
            userStats.lastExpenseDate = now
        }

        // Milestone Checks - Now using optimized counter
        checkTransactionMilestones(userStats.totalTransactions)
        saveStats()
    }

    /**
     * Checks if yesterday was a no-spend day.
     */
    private fun checkNoSpendDay() {
        val now = System.currentTimeMillis()
        val lastActive = userStats.lastActiveDate
        val lastExpense = userStats.lastExpenseDate

        if (lastActive == 0L) return

        if (!isSameDay(lastActive, now)) {
            val yesterday = Calendar.getInstance().apply { 
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -1)
            }
            
            if (!isSameDay(lastExpense, yesterday.timeInMillis) && lastExpense < yesterday.timeInMillis) {
                unlockAchievement("NO_SPEND_DAY")
            }
        }
    }

    /**
     * Awards XP with farming protection (Daily Cap).
     */
    fun awardXP(amount: Int, reason: String = "", bypassCap: Boolean = false) {
        val userId = auth.currentUser?.uid ?: return
        val statsRef = database.getReference(Constants.PATH_USERS)
            .child(userId).child(PATH_GAMIFICATION).child(PATH_STATS)

        statsRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val stats = mutableData.getValue(UserStats::class.java) ?: UserStats()
                if (stats.level >= LevelConfig.MAX_LEVEL) return Transaction.success(mutableData)

                val now = System.currentTimeMillis()
                
                if (!isSameDay(stats.lastXpAwardDate, now)) {
                    stats.dailyXpEarned = 0
                }
                
                val amountToAward = if (bypassCap) {
                    amount
                } else {
                    val remaining = DAILY_XP_CAP - stats.dailyXpEarned
                    if (remaining <= 0) 0 else minOf(amount, remaining)
                }

                if (amountToAward <= 0) return Transaction.success(mutableData)

                stats.xp += amountToAward
                stats.totalXP += amountToAward
                stats.dailyXpEarned += amountToAward
                stats.lastXpAwardDate = now

                var currentXpToNext = LevelConfig.getXpToNextLevel(stats.level)
                while (currentXpToNext > 0 && stats.xp >= currentXpToNext) {
                    stats.xp -= currentXpToNext
                    stats.level++
                    
                    if (stats.level >= LevelConfig.MAX_LEVEL) {
                        stats.level = LevelConfig.MAX_LEVEL
                        stats.xp = 0
                        currentXpToNext = 0
                    } else {
                        currentXpToNext = LevelConfig.getXpToNextLevel(stats.level)
                    }
                }

                mutableData.value = stats
                return Transaction.success(mutableData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (committed && snapshot != null) {
                    val newStats = snapshot.getValue(UserStats::class.java) ?: return
                    if (newStats.level > userStats.level) {
                        listeners.forEach { it.onLevelUp(newStats.level, LevelConfig.getLevelTitle(newStats.level)) }
                    }
                }
            }
        })
    }

    /**
     * Updates daily login streak using calendar day comparison.
     */
    fun updateStreak() {
        val now = System.currentTimeMillis()
        val lastActive = userStats.lastActiveDate
        
        if (isSameDay(lastActive, now) && userStats.currentStreak > 0) return

        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        val lastCal = Calendar.getInstance().apply { timeInMillis = lastActive }

        val d1 = Calendar.getInstance().apply { 
            set(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), nowCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val d2 = Calendar.getInstance().apply { 
            set(lastCal.get(Calendar.YEAR), lastCal.get(Calendar.MONTH), lastCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffDays = TimeUnit.MILLISECONDS.toDays(d1.timeInMillis - d2.timeInMillis)

        when {
            diffDays == 1L -> {
                userStats.currentStreak++
                userStats.streakLost = false
                if (userStats.currentStreak > userStats.longestStreak) {
                    userStats.longestStreak = userStats.currentStreak
                }
                
                if (userStats.currentStreak == 7) {
                    unlockAchievement("7_DAY_STREAK")
                }
                awardXP(5, "Daily login")
            }
            diffDays > 1L -> {
                userStats.streakLost = true
                userStats.currentStreak = 1
                awardXP(5, "Daily login")
            }
            diffDays == 0L || userStats.currentStreak == 0 -> {
                if (userStats.currentStreak == 0) {
                    userStats.currentStreak = 1
                    awardXP(5, "Daily login")
                }
            }
        }

        userStats.lastActiveDate = now
        saveStats()
    }

    fun unlockAchievement(id: String) {
        val achievement = achievements[id] ?: return
        if (!achievement.achieved) {
            achievement.achieved = true
            achievement.achievedDate = System.currentTimeMillis()
            awardXP(achievement.xpReward, "Achievement: ${achievement.name}", bypassCap = true)
            saveAchievement(achievement)
            listeners.forEach { it.onAchievementUnlocked(achievement) }
        }
    }

    fun onBudgetCreated() {
        awardXP(XP_BUDGET_CREATED, "Budget Created")
    }

    fun recordGoalDeposit(amount: Double) {
        userStats.totalSaved += amount
        saveStats()
        awardXP(XP_GOAL_DEPOSIT, "Goal Deposit")
        if (userStats.totalSaved >= 1000) unlockAchievement("SAVER_LEVEL_1")
    }

    fun onGoalCompleted() {
        awardXP(XP_GOAL_COMPLETED, "Goal Completed", bypassCap = true)
        unlockAchievement("GOAL_CRUSHER")
    }

    fun recordBudgetCompliance(isCompliant: Boolean, month: Int, year: Int) {
        // Only allow one check per month (Calendar.MONTH is 0-indexed)
        if (userStats.lastComplianceCheckMonth == month && userStats.lastComplianceCheckYear == year) return

        if (isCompliant) {
            userStats.budgetsMetCount++
            userStats.consecutiveMonthsUnderBudget++
            awardXP(XP_MONTHLY_BUDGET_MET, "Monthly Budget Met", bypassCap = true)
            
            if (userStats.budgetsMetCount >= 3) unlockAchievement("BUDGET_HERO")
            if (userStats.consecutiveMonthsUnderBudget >= 3) unlockAchievement("BUDGET_MASTER")
        } else {
            userStats.consecutiveMonthsUnderBudget = 0
        }

        userStats.lastComplianceCheckMonth = month
        userStats.lastComplianceCheckYear = year
        saveStats()
    }

    private fun saveStats() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference(Constants.PATH_USERS)
            .child(userId)
            .child(PATH_GAMIFICATION)
            .child(PATH_STATS)
            .setValue(userStats)
    }

    private fun saveAchievement(achievement: Achievement) {
        val userId = auth.currentUser?.uid ?: return
        database.getReference(Constants.PATH_USERS)
            .child(userId)
            .child(PATH_GAMIFICATION)
            .child(PATH_BADGES)
            .child(achievement.id)
            .setValue(achievement)
    }
    
    fun getStats() = userStats
    fun getAchievements() = achievements.values.toList()

    private fun checkTransactionMilestones(count: Int) {
        if (count >= 1) unlockAchievement("FIRST_EXPENSE")
        if (count >= 10) unlockAchievement("TRANSACTION_MILESTONE_10")
        if (count >= 50) unlockAchievement("TRANSACTION_MILESTONE_50")
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        if (t1 == 0L || t2 == 0L) return false
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
