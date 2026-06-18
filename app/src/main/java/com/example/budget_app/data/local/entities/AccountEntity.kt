package com.example.budget_app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // e.g., Savings, Credit Card, etc.
    val initialBalance: Double,
    val currentBalance: Double
)
