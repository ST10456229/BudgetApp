package com.example.budget_app.model

data class LevelData(
    val level: Int,
    val title: String,
    val xpRequired: Int
)

object LevelConfig {
    const val MAX_LEVEL = 10

    val levels = listOf(
        LevelData(1, "Budget Rookie", 0),
        LevelData(2, "Smart Spender", 100),
        LevelData(3, "Money Tracker", 200),
        LevelData(4, "Budget Builder", 300),
        LevelData(5, "Financial Explorer", 400),
        LevelData(6, "Savings Strategist", 500),
        LevelData(7, "Wealth Planner", 600),
        LevelData(8, "Money Master", 700),
        LevelData(9, "Financial Expert", 800),
        LevelData(10, "Wealth Guardian", 900)
    )

    fun getLevelTitle(level: Int): String {
        return levels.find { it.level == level }?.title ?: "Financial Legend"
    }

    fun getXpToNextLevel(level: Int): Int {
        if (level >= MAX_LEVEL) return 0
        // Based on the requirement: Level 1 -> 2: 100 XP, 2 -> 3: 200 XP, etc.
        // This usually means the XP needed *at* that level to reach the next.
        return level * 100
    }
}
