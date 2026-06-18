package com.example.budget_app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.budget_app.data.local.AppDatabase
import com.example.budget_app.data.repository.BudgetRepository
import com.example.budget_app.ui.navigation.BudgetNavGraph
import com.example.budget_app.ui.navigation.Screen
import com.example.budget_app.ui.theme.BudgetAppTheme
import com.example.budget_app.ui.viewmodel.BudgetViewModel
import com.example.budget_app.ui.viewmodel.BudgetViewModelFactory

class MainActivity : ComponentActivity() {
    
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { BudgetRepository(database.budgetDao()) }
    private val viewModel: BudgetViewModel by viewModels {
        BudgetViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            BudgetAppTheme(darkTheme = uiState.userProfile?.isDarkMode ?: false) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                val bottomBarRoutes = Screen.bottomNavItems.map { it.route }
                val showBottomBar = currentDestination?.route in bottomBarRoutes

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp,
                                modifier = Modifier.height(80.dp)
                            ) {
                                Screen.bottomNavItems.forEach { screen ->
                                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                    NavigationBarItem(
                                        icon = { 
                                            screen.icon?.let { 
                                                Icon(
                                                    imageVector = it, 
                                                    contentDescription = screen.label,
                                                    modifier = Modifier.size(26.dp)
                                                ) 
                                            } 
                                        },
                                        label = { 
                                            Text(
                                                text = screen.label ?: "",
                                                style = MaterialTheme.typography.labelMedium
                                            ) 
                                        },
                                        selected = isSelected,
                                        onClick = {
                                            if (!isSelected) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (showBottomBar) {
                            FloatingActionButton(
                                onClick = { navController.navigate(Screen.AddTransaction.route) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = CircleShape,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add, 
                                    contentDescription = "Add Transaction",
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    },
                    floatingActionButtonPosition = FabPosition.End,
                ) { innerPadding ->
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)) {
                        BudgetNavGraph(navController = navController, viewModel = viewModel)
                    }
                }
            }
        }
    }
}
