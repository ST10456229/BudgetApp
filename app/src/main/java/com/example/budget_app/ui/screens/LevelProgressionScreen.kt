package com.example.budget_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budget_app.ui.theme.BluePrimary
import com.example.budget_app.ui.viewmodel.BudgetViewModel

@Composable
fun LevelProgressionScreen(viewModel: BudgetViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentLevel = uiState.userProfile?.currentLevel ?: 1

    val levels = (1..10).map { lvl ->
        LevelInfo(
            level = lvl,
            title = viewModel.getLevelTitle(lvl),
            requiredXP = (lvl - 1) * 100,
            isReached = lvl <= currentLevel,
            isCurrent = lvl == currentLevel
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Level Progression",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(levels) { level ->
                LevelTierCard(level)
            }
        }
    }
}

@Composable
fun LevelTierCard(level: LevelInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (level.isCurrent) BluePrimary.copy(alpha = 0.1f) 
                             else if (level.isReached) MaterialTheme.colorScheme.surface 
                             else Color.LightGray.copy(alpha = 0.2f)
        ),
        border = if (level.isCurrent) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BluePrimary)) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (level.isReached) BluePrimary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (level.isReached) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = BluePrimary)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Level ${level.level}: ${level.title}",
                    fontWeight = FontWeight.Bold,
                    color = if (level.isReached) Color.Unspecified else Color.Gray
                )
                Text(
                    text = "${level.requiredXP} XP Required",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            if (level.isCurrent) {
                Badge(containerColor = BluePrimary) {
                    Text("CURRENT", color = Color.White, modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}

data class LevelInfo(
    val level: Int,
    val title: String,
    val requiredXP: Int,
    val isReached: Boolean,
    val isCurrent: Boolean
)
