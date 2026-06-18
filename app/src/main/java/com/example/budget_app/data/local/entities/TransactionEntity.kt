package com.example.budget_app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val description: String,
    val category: String,
    val date: Long,
    val type: String, // "Income" or "Expense"
    val accountId: Long
)
