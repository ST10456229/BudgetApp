package com.example.budget_app.data.local.dao

import androidx.room.*
import com.example.budget_app.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    // Account operations
    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    // Transaction operations
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE accountId = :accountId")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    // Savings Goal operations
    @Query("SELECT * FROM savings_goals")
    fun getAllSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity)

    // User Profile operations
    @Query("SELECT * FROM user_profile WHERE id = 0")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Query("SELECT SUM(currentBalance) FROM accounts")
    fun getTotalBalance(): Flow<Double?>
}
