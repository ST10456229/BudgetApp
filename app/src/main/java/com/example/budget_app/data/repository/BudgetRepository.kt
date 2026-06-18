package com.example.budget_app.data.repository

import com.example.budget_app.data.local.dao.BudgetDao
import com.example.budget_app.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class BudgetRepository(private val budgetDao: BudgetDao) {

    val allAccounts: Flow<List<AccountEntity>> = budgetDao.getAllAccounts()
    val allTransactions: Flow<List<TransactionEntity>> = budgetDao.getAllTransactions()
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = budgetDao.getAllSavingsGoals()
    val userProfile: Flow<UserProfileEntity?> = budgetDao.getUserProfile()
    val totalBalance: Flow<Double?> = budgetDao.getTotalBalance()

    suspend fun addAccount(name: String, type: String, balance: Double) {
        val account = AccountEntity(name = name, type = type, initialBalance = balance, currentBalance = balance)
        budgetDao.insertAccount(account)
        addXP(10)
    }

    suspend fun addTransaction(amount: Double, description: String, category: String, type: String, accountId: Long, date: Long) {
        val transaction = TransactionEntity(
            amount = amount,
            description = description,
            category = category,
            date = date,
            type = type,
            accountId = accountId
        )
        budgetDao.insertTransaction(transaction)

        val account = budgetDao.getAccountById(accountId)
        if (account != null) {
            val newBalance = if (type == "Income") {
                account.currentBalance + amount
            } else {
                account.currentBalance - amount
            }
            budgetDao.updateAccount(account.copy(currentBalance = newBalance))
        }

        addXP(20)
    }

    suspend fun addSavingsGoal(name: String, targetAmount: Double, deadline: Long, imageUri: String?) {
        val goal = SavingsGoalEntity(
            name = name,
            targetAmount = targetAmount,
            currentAmount = 0.0,
            deadline = deadline,
            imageUri = imageUri
        )
        budgetDao.insertSavingsGoal(goal)
        addXP(15)
    }

    private suspend fun addXP(amount: Int) {
        val currentProfile = userProfile.firstOrNull() ?: UserProfileEntity(username = "User", currentXP = 0, currentLevel = 1, isDarkMode = false)
        val newXP = currentProfile.currentXP + amount
        val newLevel = (newXP / 100) + 1
        
        budgetDao.insertUserProfile(currentProfile.copy(currentXP = newXP, currentLevel = newLevel))
    }

    suspend fun updateTheme(isDarkMode: Boolean) {
        val currentProfile = userProfile.firstOrNull() ?: UserProfileEntity(username = "User", currentXP = 0, currentLevel = 1, isDarkMode = false)
        budgetDao.insertUserProfile(currentProfile.copy(isDarkMode = isDarkMode))
    }
    
    suspend fun updateUsername(name: String) {
        val currentProfile = userProfile.firstOrNull() ?: UserProfileEntity(username = "User", currentXP = 0, currentLevel = 1, isDarkMode = false)
        budgetDao.insertUserProfile(currentProfile.copy(username = name))
    }

    suspend fun updateProfileImage(uri: String) {
        val currentProfile = userProfile.firstOrNull() ?: UserProfileEntity(username = "User", currentXP = 0, currentLevel = 1, isDarkMode = false)
        budgetDao.insertUserProfile(currentProfile.copy(profileImageUri = uri))
    }
}
