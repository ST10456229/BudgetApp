package com.example.budget_app.model

data class Transaction(
    var transactionId: String = "",
    val transaction_name: String = "",
    val category: Category = Category(),
    val transaction_amount: Double = 0.0,
    val transaction_date: String = "",
    val imageUrl: String = "",
    val accountId: String = ""
)
