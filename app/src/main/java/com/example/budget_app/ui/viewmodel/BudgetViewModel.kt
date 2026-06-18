package com.example.budget_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budget_app.data.local.entities.*
import com.example.budget_app.data.repository.BudgetRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BudgetUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val userProfile: UserProfileEntity? = null,
    val totalBalance: Double = 0.0,
    val isLoading: Boolean = false
)

sealed class BudgetEvent {
    data class AddAccount(val name: String, val type: String, val balance: Double) : BudgetEvent()
    data class AddTransaction(
        val amount: Double, 
        val description: String, 
        val category: String, 
        val type: String, 
        val accountId: Long,
        val date: Long
    ) : BudgetEvent()
    data class AddSavingsGoal(val name: String, val targetAmount: Double, val deadline: Long, val imageUri: String?) : BudgetEvent()
    data class UpdateTheme(val isDarkMode: Boolean) : BudgetEvent()
    data class UpdateUsername(val name: String) : BudgetEvent()
    data class UpdateProfileImage(val uri: String) : BudgetEvent()
}

class BudgetViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.allAccounts,
                repository.allTransactions,
                repository.allSavingsGoals,
                repository.userProfile,
                repository.totalBalance
            ) { accounts, transactions, goals, profile, total ->
                BudgetUiState(
                    accounts = accounts,
                    transactions = transactions,
                    savingsGoals = goals,
                    userProfile = profile,
                    totalBalance = total ?: 0.0
                )
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun onEvent(event: BudgetEvent) {
        viewModelScope.launch {
            when (event) {
                is BudgetEvent.AddAccount -> repository.addAccount(event.name, event.type, event.balance)
                is BudgetEvent.AddTransaction -> repository.addTransaction(
                    event.amount, 
                    event.description, 
                    event.category, 
                    event.type, 
                    event.accountId,
                    event.date
                )
                is BudgetEvent.AddSavingsGoal -> repository.addSavingsGoal(event.name, event.targetAmount, event.deadline, event.imageUri)
                is BudgetEvent.UpdateTheme -> repository.updateTheme(event.isDarkMode)
                is BudgetEvent.UpdateUsername -> repository.updateUsername(event.name)
                is BudgetEvent.UpdateProfileImage -> repository.updateProfileImage(event.uri)
            }
        }
    }

    fun getLevelTitle(level: Int): String = when (level) {
        1 -> "Budget Rookie"
        2 -> "Smart Spender"
        3 -> "Money Tracker"
        4 -> "Frugal Master"
        5 -> "Savings Ninja"
        6 -> "Budget Boss"
        7 -> "Finance Pro"
        8 -> "Wealth Builder"
        9 -> "Money Magnet"
        10 -> "Wealth Guardian"
        else -> "Beginner"
    }

    fun getAchievements(): List<Achievement> {
        val transactions = _uiState.value.transactions
        val savings = _uiState.value.savingsGoals.sumOf { it.currentAmount }
        
        return listOf(
            Achievement("First Step", "Logged your first transaction", transactions.isNotEmpty()),
            Achievement("Discipline Master", "0 expenses in a day", false),
            Achievement("Saver Level 1", "Total savings >= R1000", savings >= 1000.0)
        )
    }
}

data class Achievement(val name: String, val description: String, val isUnlocked: Boolean)
