package com.example.budget_app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 0, // Single user profile
    val username: String,
    val currentXP: Int,
    val currentLevel: Int,
    val isDarkMode: Boolean,
    val profileImageUri: String? = null
)
