package com.example.budget_app

import android.app.AlertDialog
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
import com.example.budget_app.model.Achievement
import com.example.budget_app.model.Account
import com.example.budget_app.model.Budget
import com.example.budget_app.model.Goal
import com.example.budget_app.model.UserStats
import com.example.budget_app.model.Transaction
import com.example.budget_app.utils.AchievementPopup
import com.example.budget_app.utils.Constants
import com.example.budget_app.utils.GamificationManager
import com.example.budget_app.utils.NavigationHelper
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(), GamificationManager.GamificationListener {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var gamificationManager: GamificationManager
    
    private lateinit var tvTotalBalance: TextView
    private lateinit var tvGreeting: TextView
    private lateinit var tvBudgetAmount: TextView
    private lateinit var tvGoalsAmount: TextView
    private lateinit var ivProfile: ImageView
    private lateinit var cvLevelInfo: View
    private lateinit var tvLevelTitle: TextView
    private lateinit var tvXPProgress: TextView
    private lateinit var levelProgressBar: LinearProgressIndicator
    private lateinit var tvStreakCount: TextView
    private lateinit var tvLevelDisplay: TextView
    private lateinit var llStreakLevel: View
    
    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var rvGoals: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var goalAdapter: GoalAdapter
    
    private val transactionList = mutableListOf<Transaction>()
    private val goalList = mutableListOf<Goal>()
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)
        
        initViews()
        setupRecyclerViews()
        setupClickListeners()
        NavigationHelper.setupNavigation(this)
        
        gamificationManager = GamificationManager.getInstance(this)
        gamificationManager.addListener(this)

        fetchUserData()
        fetchTransactions()
        fetchTotalBalance()
        fetchBudgetAndGoalsSummary()
        fetchGoals()
        
        gamificationManager.updateStreak()
        
        cvLevelInfo.visibility = View.GONE
        llStreakLevel.visibility = View.GONE
    }

    private fun initViews() {
        tvTotalBalance = findViewById(R.id.tvTotalBalance)
        tvGreeting = findViewById(R.id.tvGreeting)
        tvBudgetAmount = findViewById(R.id.tvBudgetAmount)
        tvGoalsAmount = findViewById(R.id.tvGoalsAmount)
        ivProfile = findViewById(R.id.ivProfile)
        cvLevelInfo = findViewById(R.id.cvLevelInfo)
        tvLevelTitle = findViewById(R.id.tvLevelTitle)
        tvXPProgress = findViewById(R.id.tvXPProgress)
        levelProgressBar = findViewById(R.id.levelProgressBar)
        tvStreakCount = findViewById(R.id.tvStreakCount)
        tvLevelDisplay = findViewById(R.id.tvLevelDisplay)
        llStreakLevel = findViewById(R.id.llStreakLevel)
        rvRecentTransactions = findViewById(R.id.rvRecentTransactions)
        rvGoals = findViewById(R.id.rvGoals)
    }

    private fun setupRecyclerViews() {
        rvRecentTransactions.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(transactionList)
        rvRecentTransactions.adapter = transactionAdapter

        rvGoals.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        goalAdapter = GoalAdapter(goalList)
        rvGoals.adapter = goalAdapter
    }

    private fun setupClickListeners() {
        ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        cvLevelInfo.setOnClickListener {
            startActivity(Intent(this, LevelViewerActivity::class.java))
        }

        llStreakLevel.setOnClickListener {
            startActivity(Intent(this, LevelViewerActivity::class.java))
        }

        findViewById<View>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        findViewById<View>(R.id.btnAddAccountAction).setOnClickListener {
            startActivity(Intent(this, AddAccountActivity::class.java))
        }

        findViewById<View>(R.id.btnAccounts).setOnClickListener {
            startActivity(Intent(this, AccountListActivity::class.java))
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
    }

    override fun onStatsUpdated(stats: UserStats) {
        runOnUiThread {
            if (::tvLevelTitle.isInitialized) {
                tvLevelTitle.text = "⭐ Level ${stats.level}: ${stats.levelTitle}"
                
                if (stats.level >= 10) {
                    tvXPProgress.text = "MAX LEVEL REACHED"
                    levelProgressBar.progress = 100
                } else {
                    val xpToGo = stats.xpToNextLevel - stats.xp
                    tvXPProgress.text = "${stats.xp} / ${stats.xpToNextLevel} XP ($xpToGo to go)"
                    levelProgressBar.progress = (stats.progressToNextLevel * 100).toInt()
                }
                
                tvLevelDisplay.text = "lvl: ${stats.level}"
                
                if (stats.streakLost) {
                    tvStreakCount.visibility = View.VISIBLE
                    tvStreakCount.text = "🧊 Streak Lost!"
                } else if (stats.currentStreak > 0) {
                    tvStreakCount.visibility = View.VISIBLE
                    tvStreakCount.text = "🔥 ${stats.currentStreak}"
                } else {
                    tvStreakCount.visibility = View.GONE
                }
                
                if (stats.currentStreak > 0 || stats.streakLost) {
                    llStreakLevel.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onLevelUp(newLevel: Int, title: String) {
        runOnUiThread {
            showTemporaryLevelInfo()
            AlertDialog.Builder(this)
                .setTitle("LEVEL UP! 🎉")
                .setMessage("Congratulations! You've reached Level $newLevel: $title")
                .setPositiveButton("Awesome!") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    override fun onAchievementUnlocked(achievement: Achievement) {
        runOnUiThread {
            AchievementPopup(this).show(achievement)
        }
    }

    private fun showTemporaryLevelInfo() {
        cvLevelInfo.visibility = View.VISIBLE
        llStreakLevel.visibility = View.VISIBLE
        cvLevelInfo.postDelayed({
            if (!isFinishing) {
                cvLevelInfo.visibility = View.GONE
            }
        }, 7000)
    }

    private fun fetchUserData() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val userRef = database.getReference(Constants.PATH_USERS).child(userId).child("profile")
        
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing) return
                
                val username = snapshot.child("username").getValue(String::class.java)
                val profilePic = snapshot.child("profilePic").getValue(String::class.java)
                
                val displayName = username ?: user.displayName ?: "User"
                tvGreeting.text = String.format("Hello, %s", displayName)
                
                if (profilePic.isNullOrEmpty()) {
                    ivProfile.setImageResource(R.drawable.ic_profile)
                    ivProfile.setColorFilter(ContextCompat.getColor(this@MainActivity, R.color.primary_blue))
                } else {
                    loadProfileImage(profilePic, ivProfile)
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
            Log.e(TAG, "Error loading profile pic")
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
                        if (account != null) {
                            total += account.balance
                        }
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
        
        // Budgets
        database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_BUDGETS)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing) return
                    var totalBudget = 0.0
                    for (data in snapshot.children) {
                        val budget = data.getValue(Budget::class.java)
                        if (budget != null) {
                            totalBudget += budget.targetAmount
                        }
                    }
                    tvBudgetAmount.text = String.format(Locale.US, "R %.2f", totalBudget)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
            
        // Goals
        database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_GOALS)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing) return
                    var totalGoal = 0.0
                    for (data in snapshot.children) {
                        val goal = data.getValue(Goal::class.java)
                        if (goal != null) {
                            totalGoal += goal.targetAmount
                        }
                    }
                    tvGoalsAmount.text = String.format(Locale.US, "R %.2f", totalGoal)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchTransactions() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_TRANSACTIONS)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing) return
                    val oldSize = transactionList.size
                    transactionList.clear()
                    for (data in snapshot.children) {
                        val transaction = data.getValue(Transaction::class.java)
                        if (transaction != null) {
                            transactionList.add(transaction)
                        }
                    }
                    transactionList.reverse()
                    transactionAdapter.notifyDataSetChanged()
                    
                    if (transactionList.size > oldSize && oldSize > 0) {
                        showTemporaryLevelInfo()
                    }
                    // Triggering onTransactionAdded only when a NEW transaction is actually added
                    // To prevent spamming on initial load, we might need more logic, 
                    // but usually it's triggered from the 'Save' activity.
                    // However, the original code had it here.
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
                        if (goal != null) {
                            goalList.add(goal)
                        }
                    }
                    goalAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
