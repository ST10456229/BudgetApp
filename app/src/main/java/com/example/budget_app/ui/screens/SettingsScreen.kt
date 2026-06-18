package com.example.budget_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.budget_app.ui.navigation.Screen
import com.example.budget_app.ui.theme.BluePrimary
import com.example.budget_app.ui.viewmodel.BudgetEvent
import com.example.budget_app.ui.viewmodel.BudgetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: BudgetViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.userProfile
    
    var username by remember(profile) { mutableStateOf(profile?.username ?: "") }
    var isDarkMode by remember(profile) { mutableStateOf(profile?.isDarkMode ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Image Selection
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.Gray)
                }
                IconButton(
                    onClick = { /* Image Picker */ },
                    modifier = Modifier
                        .background(BluePrimary, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Change Picture", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Username Edit
            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it
                    viewModel.onEvent(BudgetEvent.UpdateUsername(it))
                },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            // Gamification Shortcuts
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { navController.navigate(Screen.LevelProgression.route) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary.copy(alpha = 0.1f), contentColor = BluePrimary)
                ) {
                    Text("Levels")
                }
                Button(
                    onClick = { navController.navigate(Screen.Achievements.route) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary.copy(alpha = 0.1f), contentColor = BluePrimary)
                ) {
                    Text("Badges")
                }
            }

            // App Preferences
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("App Preferences", style = MaterialTheme.typography.titleSmall, color = BluePrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Appearance")
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { 
                                isDarkMode = it
                                viewModel.onEvent(BudgetEvent.UpdateTheme(it))
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = { /* Logout Logic */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("LOGOUT ACCOUNT", fontWeight = FontWeight.Bold)
            }
        }
    }
}
