package com.example.budget_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.budget_app.ui.components.TransactionItem
import com.example.budget_app.ui.navigation.Screen
import com.example.budget_app.ui.theme.BluePrimary
import com.example.budget_app.ui.theme.BlueSecondary
import com.example.budget_app.ui.theme.BlueTertiary
import com.example.budget_app.ui.viewmodel.BudgetViewModel
import java.util.*

@Composable
fun HomeScreen(viewModel: BudgetViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile = uiState.userProfile

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, ${userProfile?.username ?: "User"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Streak",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Lvl ${userProfile?.currentLevel ?: 1}: ${viewModel.getLevelTitle(userProfile?.currentLevel ?: 1)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(BlueSecondary)
                        .clickable { navController.navigate(Screen.Settings.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                }
            }
        }

        // Balance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Balance", color = Color.White.copy(alpha = 0.8f))
                    Text(
                        text = "R ${String.format(Locale.US, "%.2f", uiState.totalBalance)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }

        // Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickActionItem(Icons.Default.History, "History") { navController.navigate(Screen.History.route) }
                QuickActionItem(Icons.Default.AddCard, "Add Account") { navController.navigate(Screen.AddAccount.route) }
                QuickActionItem(Icons.Default.AccountBalanceWallet, "Accounts") { navController.navigate(Screen.YourAccounts.route) }
                QuickActionItem(Icons.Default.Settings, "Settings") { navController.navigate(Screen.Settings.route) }
            }
        }

        // Budget & Savings Summary
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Monthly Budget", "R 5,000", BluePrimary, Modifier.weight(1f))
                SummaryCard(
                    title = "Savings Goals",
                    amount = "R ${String.format(Locale.US, "%.0f", uiState.savingsGoals.sumOf { it.currentAmount })}",
                    color = Color.LightGray,
                    modifier = Modifier.weight(1f),
                    textColor = Color.Black
                )
            }
        }

        // Savings Goals List
        item {
            Column {
                Text("Savings Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (uiState.savingsGoals.isEmpty()) {
                    Text("No goals yet. Start saving today!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.savingsGoals) { goal ->
                            SavingsGoalCard(goal.name, goal.currentAmount, goal.targetAmount)
                        }
                    }
                }
            }
        }

        // Recent Transactions
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "See All",
                        modifier = Modifier.clickable { navController.navigate(Screen.History.route) },
                        color = BluePrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (uiState.transactions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "No transactions yet. Tap + to add one.",
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    uiState.transactions.take(5).forEach { transaction ->
                        TransactionItem(
                            title = transaction.description,
                            category = transaction.category,
                            amount = transaction.amount,
                            type = transaction.type
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(BlueTertiary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = BluePrimary, modifier = Modifier.size(24.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SummaryCard(title: String, amount: String, color: Color, modifier: Modifier, textColor: Color = Color.White) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = textColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            Text(amount, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun SavingsGoalCard(name: String, current: Double, target: Double) {
    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(Color.LightGray.copy(alpha = 0.2f)))
            Spacer(Modifier.height(8.dp))
            Text(name, fontWeight = FontWeight.Bold, maxLines = 1)
            val progress = (if (target > 0) current / target else 0.0).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                color = BluePrimary,
                trackColor = BlueTertiary.copy(alpha = 0.3f)
            )
            Text(
                text = "R ${String.format(Locale.US, "%.2f", current)} / R ${String.format(Locale.US, "%.2f", target)}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
