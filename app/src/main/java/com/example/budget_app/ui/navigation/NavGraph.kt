package com.example.budget_app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.budget_app.ui.screens.*
import com.example.budget_app.ui.viewmodel.BudgetViewModel

sealed class Screen(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Home", Icons.Rounded.Home)
    data object Analytics : Screen("analytics", "Reports", Icons.Rounded.Assessment)
    data object History : Screen("history", "History", Icons.AutoMirrored.Rounded.ReceiptLong)
    data object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
    data object AddAccount : Screen("add_account")
    data object YourAccounts : Screen("your_accounts")
    data object AddTransaction : Screen("add_transaction")
    data object Achievements : Screen("achievements")
    data object LevelProgression : Screen("level_progression")

    companion object {
        val bottomNavItems = listOf(Home, Analytics, History, Settings)
    }
}

@Composable
fun BudgetNavGraph(
    navController: NavHostController,
    viewModel: BudgetViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(viewModel, navController)
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(viewModel)
        }
        composable(Screen.History.route) {
            HistoryScreen(viewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel, navController)
        }
        composable(Screen.AddAccount.route) {
            AddAccountScreen(viewModel, navController)
        }
        composable(Screen.YourAccounts.route) {
            YourAccountsScreen(viewModel, navController)
        }
        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(viewModel, navController)
        }
        composable(Screen.Achievements.route) {
            AchievementsScreen(viewModel)
        }
        composable(Screen.LevelProgression.route) {
            LevelProgressionScreen(viewModel)
        }
    }
}
