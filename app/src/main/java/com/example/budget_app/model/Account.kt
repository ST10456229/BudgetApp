package com.example.budget_app.model

data class Account(
    val accountId: String = "",
    val name: String = "",
    var balance: Double = 0.0,
    val type: String = "Cash" // e.g., Cash, Bank, Credit Card
)
