package com.example.budget_app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.budget_app.ui.viewmodel.BudgetEvent
import com.example.budget_app.ui.viewmodel.BudgetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(viewModel: BudgetViewModel, navController: NavController) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Savings") }
    var balance by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val accountTypes = listOf("Savings", "Credit Card", "Checking", "Investment", "Cash")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Account") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account Name") },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    accountTypes.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                type = selectionOption
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = balance,
                onValueChange = { balance = it },
                label = { Text("Initial Balance") },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("R ") }
            )

            Button(
                onClick = {
                    val balanceVal = balance.toDoubleOrNull() ?: 0.0
                    viewModel.onEvent(BudgetEvent.AddAccount(name, type, balanceVal))
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && balance.isNotBlank()
            ) {
                Text("Save Account")
            }
        }
    }
}
