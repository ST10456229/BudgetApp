package com.example.budget_app

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.budget_app.model.Account
import com.example.budget_app.model.Category
import com.example.budget_app.model.Transaction
import com.example.budget_app.utils.Constants
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etAmount: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var spinnerCategory: MaterialAutoCompleteTextView
    private lateinit var spinnerAccount: MaterialAutoCompleteTextView
    private lateinit var etDate: TextInputEditText
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var ivReceiptPreview: ImageView
    private lateinit var btnSaveExpense: MaterialButton
    private lateinit var toggleTransactionType: MaterialButtonToggleGroup
    private lateinit var ivProfile: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val accountList = mutableListOf<Account>()
    private var internalReceiptUri: String = ""
    private val TAG = "AddExpenseActivity"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleReceiptSelection(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)

        // Bind UI components
        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerAccount = findViewById(R.id.spinnerAccount)
        etDate = findViewById(R.id.etDate)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        ivReceiptPreview = findViewById(R.id.ivReceiptPreview)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)
        toggleTransactionType = findViewById(R.id.toggleTransactionType)
        ivProfile = findViewById(R.id.ivProfile)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        findViewById<View>(R.id.ivMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Initialize with default states to ensure they are clickable immediately
        spinnerAccount.setSimpleItems(arrayOf("Loading accounts..."))
        spinnerCategory.setSimpleItems(arrayOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Health", "Other"))

        // Explicit click listeners for better responsiveness in ExposedDropdownMenu
        spinnerAccount.setOnClickListener { spinnerAccount.showDropDown() }
        spinnerCategory.setOnClickListener { spinnerCategory.showDropDown() }

        // Handle item selection (e.g., navigating to Add Account)
        spinnerAccount.setOnItemClickListener { parent, _, position, _ ->
            val selection = parent.getItemAtPosition(position).toString()
            if (selection == "Add Account +") {
                spinnerAccount.setText("", false)
                startActivity(Intent(this, AddAccountActivity::class.java))
            }
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java)); finish() }
                R.id.nav_reports -> { startActivity(Intent(this, ReportsActivity::class.java)); finish() }
                R.id.nav_history -> { startActivity(Intent(this, TransactionHistoryActivity::class.java)); finish() }
                R.id.nav_categories -> { startActivity(Intent(this, CategoryActivity::class.java)); finish() }
                R.id.nav_goals -> { startActivity(Intent(this, CreateGoalActivity::class.java)); finish() }
                R.id.nav_settings -> { startActivity(Intent(this, ProfileActivity::class.java)); finish() }
                R.id.nav_logout -> {
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        fetchAccounts()
        setupCategories()
        fetchUserProfile()

        etDate.setOnClickListener { showDatePicker() }
        btnSaveExpense.setOnClickListener {
            saveTransaction() 
        }
        btnSelectImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        ivProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }

        bottomNavigation.selectedItemId = R.id.nav_history
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java)); finish(); true }
                R.id.nav_reports -> { startActivity(Intent(this, ReportsActivity::class.java)); finish(); true }
                R.id.nav_history -> true
                R.id.nav_more -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                else -> false
            }
        }
        
        // Default date to today
        val calendar = Calendar.getInstance()
        etDate.setText(String.format(Locale.US, "%02d/%02d/%d", 
            calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)))
    }

    private fun handleReceiptSelection(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "receipt_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
            internalReceiptUri = Uri.fromFile(file).toString()
            ivReceiptPreview.setImageURI(Uri.parse(internalReceiptUri))
            ivReceiptPreview.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchUserProfile() {
        val user = auth.currentUser ?: return
        database.getReference(Constants.PATH_USERS).child(user.uid).child(Constants.PATH_PROFILE)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing) return
                    val username = snapshot.child("username").getValue(String::class.java) ?: user.displayName
                    val profilePic = snapshot.child("profilePic").getValue(String::class.java)
                    val email = user.email ?: ""

                    if (!profilePic.isNullOrEmpty()) {
                        loadProfileImage(profilePic, ivProfile)
                    }

                    val headerView = navView.getHeaderView(0)
                    if (headerView != null) {
                        val ivNavProfile = headerView.findViewById<ImageView>(R.id.ivNavProfile)
                        val tvNavUsername = headerView.findViewById<TextView>(R.id.tvNavUsername)
                        val tvNavEmail = headerView.findViewById<TextView>(R.id.tvNavEmail)
                        tvNavUsername?.text = username ?: "User"
                        tvNavEmail?.text = email
                        if (!profilePic.isNullOrEmpty() && ivNavProfile != null) {
                            loadProfileImage(profilePic, ivNavProfile)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadProfileImage(profilePic: String, imageView: ImageView) {
        try {
            val uri = Uri.parse(profilePic)
            val path = uri.path
            if (path != null && File(path).exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageURI(uri)
            }
            imageView.colorFilter = null
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile pic")
        }
    }

    private fun fetchAccounts() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_ACCOUNTS)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing) return
                    accountList.clear()
                    val names = mutableListOf<String>()
                    
                    if (snapshot.exists()) {
                        for (data in snapshot.children) {
                            val account = data.getValue(Account::class.java)
                            if (account != null) {
                                val accWithId = account.copy(accountId = data.key ?: "")
                                accountList.add(accWithId)
                                names.add(account.name)
                            }
                        }
                    }
                    
                    if (names.isEmpty()) {
                        names.add("Add Account +")
                    }
                    
                    // setSimpleItems is optimized for Material3 Exposed Dropdowns
                    spinnerAccount.setSimpleItems(names.toTypedArray())
                    
                    // Auto-select the first actual account if one exists and nothing is selected
                    if (accountList.isNotEmpty() && spinnerAccount.text.isEmpty()) {
                        spinnerAccount.setText(accountList[0].name, false)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AddExpenseActivity, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupCategories() {
        val userId = auth.currentUser?.uid ?: return

        // Start with defaults immediately
        val categoryNames = mutableListOf(
            "Food", "Transport", "Bills", "Shopping", "Entertainment", "Health", "Other"
        )

        // Create ONE adapter and reuse it
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        spinnerCategory.setAdapter(adapter)

        database.getReference(Constants.PATH_USERS)
            .child(userId)
            .child(Constants.PATH_CUSTOM_CATEGORIES)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing) return

                    // Reset to defaults, then append custom ones
                    categoryNames.clear()
                    categoryNames.addAll(
                        listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Health", "Other")
                    )

                    for (data in snapshot.children) {
                        val catName = data.child("category_name").getValue(String::class.java)
                        if (!catName.isNullOrBlank() && !categoryNames.contains(catName)) {
                            categoryNames.add(catName)
                        }
                    }

                    // Notify the existing adapter instead of replacing it
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load categories: ${error.message}")
                }
            })
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, day ->
            etDate.setText(String.format(Locale.US, "%02d/%02d/%d", day, month + 1, year))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun saveTransaction() {
        val amountStr = etAmount.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val categoryName = spinnerCategory.text.toString()
        val accountName = spinnerAccount.text.toString()
        val date = etDate.text.toString().trim()
        val isIncome = toggleTransactionType.checkedButtonId == R.id.btnIncome

        if (amountStr.isEmpty() || categoryName.isEmpty() || accountName.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull() ?: 0.0
        if (amount <= 0) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedAccount = accountList.find { it.name == accountName }
        if (selectedAccount == null && accountName != "Add Account +") {
             Toast.makeText(this, "Please select a valid account", Toast.LENGTH_SHORT).show()
             return
        }
        
        if (accountName == "Add Account +") {
            Toast.makeText(this, "Please add and select an account first", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User session expired. Re-login required.", Toast.LENGTH_SHORT).show()
            return
        }

        btnSaveExpense.isEnabled = false
        val transRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_TRANSACTIONS).push()

        val transaction = Transaction(
            transactionId = transRef.key ?: "",
            transaction_name = if (description.isEmpty()) categoryName else description,
            category = Category(category_name = categoryName, type = if (isIncome) "Income" else "Expense"),
            transaction_amount = if (isIncome) amount else -amount,
            transaction_date = date,
            imageUrl = internalReceiptUri,
            accountId = selectedAccount?.accountId ?: ""
        )

        transRef.setValue(transaction).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                selectedAccount?.let { updateAccountBalance(it, if (isIncome) amount else -amount) }
                Toast.makeText(this, "Transaction Saved Successfully!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                btnSaveExpense.isEnabled = true
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateAccountBalance(account: Account, amountChange: Double) {
        val userId = auth.currentUser?.uid ?: return
        val newBalance = account.balance + amountChange
        database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_ACCOUNTS)
            .child(account.accountId).child("balance").setValue(newBalance)
    }
}