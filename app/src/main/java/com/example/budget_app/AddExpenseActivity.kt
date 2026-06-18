package com.example.budget_app

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.budget_app.model.Account
import com.example.budget_app.model.Category
import com.example.budget_app.model.Transaction
import com.example.budget_app.utils.Constants
import com.example.budget_app.utils.GamificationManager
import com.example.budget_app.utils.NavigationHelper
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etAmount: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var spinnerCategory: MaterialAutoCompleteTextView
    private lateinit var spinnerAccount: MaterialAutoCompleteTextView
    private lateinit var etDate: TextInputEditText
    private lateinit var btnSelectImage: Button
    private lateinit var ivReceiptPreview: ImageView
    private lateinit var btnSaveExpense: Button
    private lateinit var toggleTransactionType: MaterialButtonToggleGroup
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var gamificationManager: GamificationManager

    private val accountList = mutableListOf<Account>()
    private var selectedAccountId: String? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            ivReceiptPreview.setImageURI(it)
            ivReceiptPreview.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)
        gamificationManager = GamificationManager.getInstance(this)

        // Initialize UI components
        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerAccount = findViewById(R.id.spinnerAccount)
        etDate = findViewById(R.id.etDate)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        ivReceiptPreview = findViewById(R.id.ivReceiptPreview)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)
        toggleTransactionType = findViewById(R.id.toggleTransactionType)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Setup Categories
        val categories = arrayOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Salary", "Gift", "Other")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        spinnerCategory.setAdapter(categoryAdapter)

        // Setup Accounts
        setupAccountSpinner()

        // Date Picker
        etDate.setOnClickListener {
            showDatePicker()
        }

        // Set click listener for Save Button
        btnSaveExpense.setOnClickListener {
            saveExpense()
        }

        // Make Image Selection functional
        btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        NavigationHelper.setupNavigation(this)
    }

    private fun setupAccountSpinner() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_ACCOUNTS)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    accountList.clear()
                    val accountNames = mutableListOf<String>()
                    for (data in snapshot.children) {
                        val account = data.getValue(Account::class.java)
                        if (account != null) {
                            accountList.add(account)
                            accountNames.add("${account.name} (R${String.format("%.2f", account.balance)})")
                        }
                    }
                    val adapter = ArrayAdapter(this@AddExpenseActivity, android.R.layout.simple_dropdown_item_1line, accountNames)
                    spinnerAccount.setAdapter(adapter)
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        spinnerAccount.setOnItemClickListener { _, _, position, _ ->
            selectedAccountId = accountList[position].accountId
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val date = String.format(Locale.US, "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
            etDate.setText(date)
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun saveExpense() {
        val amountStr = etAmount.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val categoryName = spinnerCategory.text.toString()
        val date = etDate.text.toString().trim()
        val isIncome = toggleTransactionType.checkedButtonId == R.id.btnIncome
        val accountName = spinnerAccount.text.toString()

        // Validation
        if (amountStr.isEmpty()) {
            etAmount.error = "Please enter an amount"
            return
        }

        if (description.isEmpty()) {
            etDescription.error = "Please enter a description"
            return
        }
        
        if (categoryName.isEmpty()) {
            spinnerCategory.error = "Please select a category"
            return
        }

        if (accountName.isEmpty() || selectedAccountId == null) {
            spinnerAccount.error = "Please select an account"
            return
        }

        if (date.isEmpty()) {
            etDate.error = "Please select a date"
            return
        }

        val amount = amountStr.toDoubleOrNull() ?: 0.0
        val finalAmount = if (isIncome) Math.abs(amount) else -Math.abs(amount)
        
        val userId = auth.currentUser?.uid ?: return
        val transRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_TRANSACTIONS).push()

        val transaction = Transaction(
            transactionId = transRef.key ?: "",
            transaction_name = description,
            category = Category(category_name = categoryName),
            transaction_amount = finalAmount,
            transaction_date = date,
            transaction_type = if (isIncome) "Income" else "Expense",
            accountId = selectedAccountId ?: ""
        )

        transRef.setValue(transaction).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Update account balance
                updateAccountBalance(userId, selectedAccountId!!, finalAmount)
                
                // Trigger gamification logic
                gamificationManager.onTransactionAdded()
                
                val type = if (isIncome) "Income" else "Expense"
                Toast.makeText(this, "$type Saved Successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to save transaction", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateAccountBalance(userId: String, accountId: String, amountChange: Double) {
        val accountRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_ACCOUNTS).child(accountId)
        accountRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val account = currentData.getValue(Account::class.java)
                if (account != null) {
                    account.balance += amountChange
                    currentData.value = account
                }
                return com.google.firebase.database.Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    Log.e("AddExpense", "Balance update failed: ${error.message}")
                }
            }
        })
    }
}
