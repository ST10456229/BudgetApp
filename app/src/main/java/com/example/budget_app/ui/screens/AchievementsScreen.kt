package com.example.budget_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budget_app.ui.theme.BluePrimary
import com.example.budget_app.ui.viewmodel.BudgetViewModel

@Composable
fun AchievementsScreen(viewModel: BudgetViewModel) {
    val achievements = viewModel.getAchievements()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Achievements",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(achievements) { achievement ->
                AchievementItem(
                    name = achievement.name,
                    description = achievement.description,
                    isUnlocked = achievement.isUnlocked
                )
            }
        }
    }
}

@Composable
fun AchievementItem(name: String, description: String, isUnlocked: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface else Color.LightGray.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        if (isUnlocked) BluePrimary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Default.Star else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) BluePrimary else Color.Gray
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color.Unspecified else Color.Gray
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUnlocked) Color.Gray else Color.Gray.copy(alpha = 0.7f)
                )
            }
            if (isUnlocked) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Unlocked",
                    tint = Color(0xFF4CAF50)
                )
            }
        }
    }
}
