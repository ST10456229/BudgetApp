package com.example.budget_app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.budget_app.ui.viewmodel.BudgetEvent
import com.example.budget_app.ui.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(viewModel: BudgetViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0 for Expense, 1 for Income
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableLongStateOf(-1L) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    
    val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }

    val categories = if (selectedTab == 0) {
        listOf("Food", "Transport", "Rent", "Entertainment", "Shopping", "Health", "Other")
    } else {
        listOf("Salary", "Freelance", "Gift", "Investment", "Other")
    }

    // Set default category and account if available
    LaunchedEffect(selectedTab) {
        selectedCategory = if (selectedTab == 0) "Food" else "Salary"
    }

    LaunchedEffect(uiState.accounts) {
        if (selectedAccountId == -1L && uiState.accounts.isNotEmpty()) {
            selectedAccountId = uiState.accounts.first().id
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedTab == 0) "Add Expense" else "Add Income") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0, 
                    onClick = { selectedTab = 0 },
                    text = { Text("Expense", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1, 
                    onClick = { selectedTab = 1 },
                    text = { Text("Income", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("R ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Account Selector
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = !accountExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedAccount = uiState.accounts.find { it.id == selectedAccountId }
                OutlinedTextField(
                    value = selectedAccount?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Source Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    uiState.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text("${account.name} (R ${String.format(Locale.US, "%.2f", account.currentBalance)})") },
                            onClick = {
                                selectedAccountId = account.id
                                accountExpanded = false
                            }
                        )
                    }
                }
            }

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What was this for?") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category Selector
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Date & Receipt Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val formattedDate = remember(dateState.selectedDateMillis) {
                    val date = dateState.selectedDateMillis?.let { Date(it) } ?: Date()
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
                }
                
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(formattedDate)
                }
                OutlinedButton(
                    onClick = { /* Image Picker Placeholder */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Receipt")
                }
            }

            Spacer(Modifier.weight(1f))

            var isSubmitting by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull() ?: 0.0
                    val type = if (selectedTab == 0) "Expense" else "Income"
                    val dateVal = dateState.selectedDateMillis ?: System.currentTimeMillis()
                    
                    if (selectedAccountId != -1L && amountVal > 0 && !isSubmitting) {
                        isSubmitting = true
                        viewModel.onEvent(BudgetEvent.AddTransaction(
                            amount = amountVal,
                            description = description,
                            category = selectedCategory,
                            type = type,
                            accountId = selectedAccountId,
                            date = dateVal
                        ))
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 16.dp),
                enabled = amount.isNotBlank() && selectedAccountId != -1L && !isSubmitting,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Transaction", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
