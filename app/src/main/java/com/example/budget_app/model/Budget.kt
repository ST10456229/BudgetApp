package com.example.budget_app.model

data class Budget(
    var budgetId: String = "",
    var name: String = "",
    var targetAmount: Double = 0.0,
    var period: String = "Monthly",
    var minGoal: Double = 0.0,
    var maxGoal: Double = 0.0
)
