package com.example.budget_app.model

data class Category(
    val categoryId: String = "",
    val category_name: String = "",
    val type: String = "Expense",
    val colorCode: String = "#0056D2",
    val description: String = ""
)
