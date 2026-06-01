package com.example.budget_app

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.Transaction_Adapter.TransactionAdapter
import com.example.budget_app.adapter.GoalAdapter
import com.example.budget_app.model.Account
import com.example.budget_app.model.Transaction
import com.example.budget_app.model.Budget
import com.example.budget_app.model.Goal
import com.example.budget_app.utils.Constants
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    
    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()
    
    private lateinit var rvGoals: RecyclerView
    private lateinit var goalAdapter: GoalAdapter
    private val goalList = mutableListOf<Goal>()
    
    private lateinit var tvTotalBalance: TextView
    private lateinit var tvGreeting: TextView
    private lateinit var tvBudgetAmount: TextView
    private lateinit var tvGoalsAmount: TextView
    private lateinit var ivProfile: ImageView

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)
        
        // Initialize UI components
        tvTotalBalance = findViewById(R.id.tvTotalBalance)
        tvGreeting = findViewById(R.id.tvGreeting)
        tvBudgetAmount = findViewById(R.id.tvBudgetAmount)
        tvGoalsAmount = findViewById(R.id.tvGoalsAmount)
        ivProfile = findViewById(R.id.ivProfile)
        
        rvRecentTransactions = findViewById(R.id.rvRecentTransactions)
        rvGoals = findViewById(R.id.rvGoals)
        
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)

        // Setup Recent Transactions RecyclerView
        rvRecentTransactions.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(transactionList)
        rvRecentTransactions.adapter = transactionAdapter

        // Setup Goals RecyclerView
        rvGoals.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        goalAdapter = GoalAdapter(goalList)
        rvGoals.adapter = goalAdapter

        setupClickListeners(fabAdd, bottomNavigation)
        
        // Fetch data
        fetchUserData()
        fetchTransactions()
        fetchTotalBalance()
        fetchBudgetAndGoalsSummary()
        fetchGoals()
    }

    private fun setupClickListeners(fabAdd: FloatingActionButton, bottomNavigation: BottomNavigationView) {
        ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<View>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        findViewById<View>(R.id.btnAddAccountAction).setOnClickListener {
            startActivity(Intent(this, AddAccountActivity::class.java))
        }

        findViewById<View>(R.id.btnAccounts).setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }

        findViewById<View>(R.id.btnMoreAction).setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }

        findViewById<View>(R.id.cvBudget).setOnClickListener {
            startActivity(Intent(this, CreateBudgetActivity::class.java))
        }

        findViewById<View>(R.id.cvGoals).setOnClickListener {
            startActivity(Intent(this, CreateGoalActivity::class.java))
        }

        findViewById<View>(R.id.tvSeeAll).setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        fabAdd.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        // Bottom Navigation Logic
        bottomNavigation.selectedItemId = R.id.nav_home
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> true
                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportsActivity::class.java))
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, TransactionHistoryActivity::class.java))
                    true
                }
                R.id.nav_more -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun fetchUserData() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val userRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_PROFILE)
        
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing) return
                
                val username = snapshot.child("username").getValue(String::class.java)
                val profilePic = snapshot.child("profilePic").getValue(String::class.java)
                
                // Update Greeting
                tvGreeting.text = String.format(Locale.getDefault(), getString(R.string.hello_format), username ?: user.displayName ?: "User")
                
                if (!profilePic.isNullOrEmpty()) {
                    loadProfileImage(profilePic, ivProfile)
                } else {
                    ivProfile.setImageResource(R.drawable.ic_profile)
                    ivProfile.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.primary_blue))
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
            }
        })
    }

    private fun loadProfileImage(profilePic: String, imageView: ImageView) {
        try {
            val uri = Uri.parse(profilePic)
            val path = uri.path
            if (path != null && File(path).exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                imageView.setImageBitmap(bitmap)
                imageView.colorFilter = null
            } else {
                imageView.setImageURI(uri)
                imageView.colorFilter = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile pic: ${e.message}")
            imageView.setImageResource(R.drawable.ic_profile)
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.primary_blue))
        }
    }

    private fun fetchTotalBalance() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_ACCOUNTS)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing) return
                    var total = 0.0
                    for (data in snapshot.children) {
                        val account = data.getValue(Account::class.java)
                        if (account != null) total += account.balance
                    }
                    tvTotalBalance.text = String.format(Locale.US, "R %.2f", total)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Total balance error: ${error.message}")
                }
            })
    }

    private fun fetchBudgetAndGoalsSummary() {
        val userId = auth.currentUser?.uid ?: return
        
        database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_BUDGETS)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing) return
                    var totalBudget = 0.0
                    for (data in snapshot.children) {
                        val budget = data.getValue(Budget::class.java)
                        if (budget != null) totalBudget += budget.targetAmount
                    }
                    tvBudgetAmount.text = String.format(Locale.US, "R %.2f", totalBudget)
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_GOALS)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing) return
                    var totalGoal = 0.0
                    for (data in snapshot.children) {
                        val goal = data.getValue(Goal::class.java)
                        if (goal != null) totalGoal += goal.targetAmount
                    }
                    tvGoalsAmount.text = String.format(Locale.US, "R %.2f", totalGoal)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchTransactions() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_TRANSACTIONS)
            .limitToLast(10).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing) return
                    transactionList.clear()
                    for (data in snapshot.children) {
                        val transaction = data.getValue(Transaction::class.java)
                        if (transaction != null) {
                            if (transaction.transactionId.isEmpty()) transaction.transactionId = data.key ?: ""
                            transactionList.add(transaction)
                        }
                    }
                    transactionList.reverse()
                    transactionAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
    
    private fun fetchGoals() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_GOALS)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing) return
                    goalList.clear()
                    for (data in snapshot.children) {
                        val goal = data.getValue(Goal::class.java)
                        if (goal != null) goalList.add(goal)
                    }
                    goalAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
