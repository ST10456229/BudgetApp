package com.example.budget_app.model

data class Achievement(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconResId: Int = 0, // Using 0 for default or emoji can be handled differently
    val iconEmoji: String = "",
    val xpReward: Int = 0,
    var achieved: Boolean = false,
    var achievedDate: Long = 0L
)
