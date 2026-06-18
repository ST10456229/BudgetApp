package com.example.budget_app

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.adapter.BudgetAdapter
import com.example.budget_app.model.Budget
import com.example.budget_app.model.Transaction
import com.example.budget_app.utils.Constants
import com.example.budget_app.utils.GamificationManager
import com.example.budget_app.utils.NavigationHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CreateBudgetActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var gamificationManager: GamificationManager

    private lateinit var etBudgetName: MaterialAutoCompleteTextView
    private lateinit var etTargetAmount: TextInputEditText
    private lateinit var spinnerPeriod: MaterialAutoCompleteTextView
    private lateinit var btnSaveBudget: MaterialButton
    private lateinit var rvBudgets: RecyclerView
    private lateinit var ivProfile: ImageView
    
    private val budgetList = mutableListOf<Budget>()
    private val transactionList = mutableListOf<Transaction>()
    private val spentMap = mutableMapOf<String, Double>()
    private lateinit var budgetAdapter: BudgetAdapter

    private val TAG = "CreateBudget"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_createbudget)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)
        gamificationManager = GamificationManager.getInstance(this)
        
        // Initialize views
        etBudgetName = findViewById(R.id.etBudgetName)
        etTargetAmount = findViewById(R.id.etTargetAmount)
        spinnerPeriod = findViewById(R.id.spinnerPeriod)
        btnSaveBudget = findViewById(R.id.btnSaveBudget)
        rvBudgets = findViewById(R.id.rvBudgets)
        ivProfile = findViewById(R.id.ivProfile)
        
        // Setup Budget Tracker RecyclerView
        rvBudgets.layoutManager = LinearLayoutManager(this)
        budgetAdapter = BudgetAdapter(budgetList, spentMap)
        rvBudgets.adapter = budgetAdapter

        setupDropdowns()
        fetchUserProfile()
        setupClickListeners()
        NavigationHelper.setupNavigation(this)
        fetchData()
    }

    private fun setupClickListeners() {
        btnSaveBudget.setOnClickListener { 
            saveBudget() 
        }

        ivProfile.setOnClickListener { 
            startActivity(Intent(this, ProfileActivity::class.java)) 
        }

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            finish()
        }
    }

    private fun setupDropdowns() {
        val defaultCategories = mutableListOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Health", "Other")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, defaultCategories)
        etBudgetName.setAdapter(categoryAdapter)

        val userId = auth.currentUser?.uid ?: return
        val customRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_CUSTOM_CATEGORIES)
        customRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedCategories = mutableListOf<String>().apply { addAll(defaultCategories) }
                for (data in snapshot.children) {
                    val catName = data.child("category_name").getValue(String::class.java)
                    if (catName != null && !updatedCategories.contains(catName)) {
                        updatedCategories.add(catName)
                    }
                }
                val newAdapter = ArrayAdapter(this@CreateBudgetActivity, android.R.layout.simple_spinner_dropdown_item, updatedCategories)
                etBudgetName.setAdapter(newAdapter)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        
        etBudgetName.setOnTouchListener { _, _ ->
            etBudgetName.showDropDown()
            false
        }
        
        val periods = arrayOf("Weekly", "Monthly", "Yearly")
        val periodAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, periods)
        spinnerPeriod.setAdapter(periodAdapter)
        spinnerPeriod.setText("Monthly", false)
        spinnerPeriod.setOnTouchListener { _, _ ->
            spinnerPeriod.showDropDown()
            false
        }
    }

    private fun fetchUserProfile() {
        val user = auth.currentUser ?: return
        database.getReference(Constants.PATH_USERS).child(user.uid).child(Constants.PATH_PROFILE)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing) return
                    val profilePic = snapshot.child("profilePic").getValue(String::class.java)

                    if (!profilePic.isNullOrEmpty()) {
                        loadProfileImage(profilePic, ivProfile)
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

    private fun fetchData() {
        val userId = auth.currentUser?.uid ?: return
        val budgetsRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_BUDGETS)
        val transRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_TRANSACTIONS)

        budgetsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing) return
                budgetList.clear()
                for (data in snapshot.children) {
                    val budget = data.getValue(Budget::class.java)
                    if (budget != null) {
                        if (budget.budgetId.isEmpty()) budget.budgetId = data.key ?: ""
                        budgetList.add(budget)
                    }
                }
                updateSpentMap()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        transRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing) return
                transactionList.clear()
                for (data in snapshot.children) {
                    val trans = data.getValue(Transaction::class.java)
                    if (trans != null) {
                        if (trans.transactionId.isEmpty()) trans.transactionId = data.key ?: ""
                        transactionList.add(trans)
                    }
                }
                updateSpentMap()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateSpentMap() {
        spentMap.clear()
        for (budget in budgetList) {
            var spent = 0.0
            for (trans in transactionList) {
                if (trans.transaction_amount < 0) {
                    val transCategory = trans.category.category_name.trim()
                    val budgetCategory = budget.name.trim()
                    if (transCategory.equals(budgetCategory, ignoreCase = true)) {
                        if (isWithinPeriod(trans.transaction_date, budget.period)) {
                            spent += Math.abs(trans.transaction_amount)
                        }
                    }
                }
            }
            spentMap[budget.budgetId] = spent
        }
        budgetAdapter.notifyDataSetChanged()
    }

    private fun isWithinPeriod(dateStr: String, period: String): Boolean {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val transDate = sdf.parse(dateStr) ?: return false
            val transCal = Calendar.getInstance().apply { time = transDate }
            val nowCal = Calendar.getInstance()
            return when (period) {
                "Weekly" -> nowCal.get(Calendar.WEEK_OF_YEAR) == transCal.get(Calendar.WEEK_OF_YEAR) && nowCal.get(Calendar.YEAR) == transCal.get(Calendar.YEAR)
                "Monthly" -> nowCal.get(Calendar.MONTH) == transCal.get(Calendar.MONTH) && nowCal.get(Calendar.YEAR) == transCal.get(Calendar.YEAR)
                "Yearly" -> nowCal.get(Calendar.YEAR) == transCal.get(Calendar.YEAR)
                else -> true
            }
        } catch (e: Exception) {
            return false
        }
    }

    private fun saveBudget() {
        val categoryName = etBudgetName.text.toString().trim()
        val targetAmountStr = etTargetAmount.text.toString().trim()
        val period = spinnerPeriod.text.toString().trim()

        if (categoryName.isEmpty() || targetAmountStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val targetAmount = targetAmountStr.toDoubleOrNull()
        if (targetAmount == null || targetAmount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Error: Session lost. Please re-login.", Toast.LENGTH_LONG).show()
            return
        }

        btnSaveBudget.isEnabled = false
        Toast.makeText(this, "Processing budget...", Toast.LENGTH_SHORT).show()

        val existingBudget = budgetList.find { it.name.equals(categoryName, ignoreCase = true) && it.period == period }
        val budgetRef = if (existingBudget != null) {
            database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_BUDGETS).child(existingBudget.budgetId)
        } else {
            database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_BUDGETS).push()
        }

        val budget = Budget(
            budgetId = existingBudget?.budgetId ?: budgetRef.key ?: UUID.randomUUID().toString(),
            name = categoryName,
            targetAmount = targetAmount,
            period = period
        )

        budgetRef.setValue(budget).addOnCompleteListener { task ->
            btnSaveBudget.isEnabled = true
            if (task.isSuccessful) {
                // Trigger gamification
                gamificationManager.onBudgetCreated()
                
                Toast.makeText(this, if (existingBudget != null) "Budget Updated!" else "Budget Created!", Toast.LENGTH_SHORT).show()
                etBudgetName.setText("", false)
                etTargetAmount.setText("")
            } else {
                Toast.makeText(this, "Database Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { e ->
            btnSaveBudget.isEnabled = true
            Toast.makeText(this, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
