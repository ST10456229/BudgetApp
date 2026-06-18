package com.example.budget_app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budget_app.ui.theme.ExpenseRed
import com.example.budget_app.ui.theme.IncomeGreen
import com.example.budget_app.ui.viewmodel.BudgetViewModel
import java.util.Locale

@Composable
fun AnalyticsScreen(viewModel: BudgetViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val totalIncome = uiState.transactions.filter { it.type == "Income" }.sumOf { it.amount }
    val totalExpense = uiState.transactions.filter { it.type == "Expense" }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Reports & Insights", 
            style = MaterialTheme.typography.headlineMedium, 
            fontWeight = FontWeight.Bold
        )

        // Summary Blocks
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricBlock(
                title = "Total Income", 
                amount = "R ${String.format(Locale.US, "%.2f", totalIncome)}", 
                color = IncomeGreen, 
                icon = Icons.AutoMirrored.Filled.TrendingUp, 
                modifier = Modifier.weight(1f)
            )
            MetricBlock(
                title = "Total Expenses", 
                amount = "R ${String.format(Locale.US, "%.2f", totalExpense)}", 
                color = ExpenseRed, 
                icon = Icons.AutoMirrored.Filled.TrendingDown, 
                modifier = Modifier.weight(1f)
            )
        }

        // Spending Breakdown Donut
        val chartPrimary = MaterialTheme.colorScheme.primary
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Spending Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = chartPrimary.copy(alpha = 0.1f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                        )
                        if (totalIncome + totalExpense > 0) {
                            val expenseRatio = (totalExpense / (totalIncome + totalExpense)).toFloat()
                            drawArc(
                                color = ExpenseRed,
                                startAngle = -90f,
                                sweepAngle = expenseRatio * 360f,
                                useCenter = false,
                                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val percentage = if (totalIncome > 0) (totalExpense / totalIncome * 100).toInt() else if (totalExpense > 0) 100 else 0
                        Text("$percentage%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("of Income", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }

        // Trend
        val trendPrimary = MaterialTheme.colorScheme.primary
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Balance Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val transactions = uiState.transactions.take(10).reversed()
                        val points = if (transactions.isEmpty()) listOf(0.5f, 0.5f) else {
                            val list = transactions.map { it.amount.toFloat() }
                            val min = list.minOrNull() ?: 0f
                            val max = list.maxOrNull() ?: 1f
                            val range = (max - min).coerceAtLeast(1f)
                            list.map { (it - min) / range }
                        }
                        val widthStep = size.width / (points.size - 1).coerceAtLeast(1)
                        val height = size.height
                        
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = trendPrimary,
                                start = androidx.compose.ui.geometry.Offset(i * widthStep, height * (1 - points[i])),
                                end = androidx.compose.ui.geometry.Offset((i + 1) * widthStep, height * (1 - points[i + 1])),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBlock(title: String, amount: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier, 
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
