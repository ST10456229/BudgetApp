package com.example.budget_app

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.Transaction_Adapter.TransactionAdapter
import com.example.budget_app.model.Transaction
import com.example.budget_app.utils.Constants
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var rvTransactionHistory: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var ivProfile: ImageView
    
    private val TAG = "TransactionHistory"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)
        
        rvTransactionHistory = findViewById(R.id.rvTransactionHistory)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        ivProfile = findViewById(R.id.ivProfile)
        
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)
        val ivBack: ImageView = findViewById(R.id.ivBack)

        ivBack.setOnClickListener { finish() }
        ivProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }

        // Setup RecyclerView
        rvTransactionHistory.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(transactionList)
        rvTransactionHistory.adapter = transactionAdapter

        fabAdd.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        setupNavigation(bottomNavigation)
        fetchUserProfile()
        fetchFullHistory()
    }

    private fun setupNavigation(bottomNavigation: BottomNavigationView) {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java)); finish() }
                R.id.nav_reports -> { startActivity(Intent(this, ReportsActivity::class.java)); finish() }
                R.id.nav_history -> { drawerLayout.closeDrawer(GravityCompat.START) }
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

    private fun fetchFullHistory() {
        val userId = auth.currentUser?.uid ?: return
        val transRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_TRANSACTIONS)

        transRef.addValueEventListener(object : ValueEventListener {
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
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TransactionHistoryActivity, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
