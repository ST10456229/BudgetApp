package com.example.budget_app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.Transaction_Adapter.TransactionAdapter
import com.example.budget_app.model.transactions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvTransactionHistory: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<transactions>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        drawerLayout = findViewById(R.id.drawer_layout)
        rvTransactionHistory = findViewById(R.id.rvTransactionHistory)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Setup RecyclerView
        rvTransactionHistory.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(transactionList)
        rvTransactionHistory.adapter = transactionAdapter

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Click Listeners
        findViewById<ImageView>(R.id.ivMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                R.id.nav_categories -> startActivity(Intent(this, Category::class.java))
                R.id.nav_goals -> startActivity(Intent(this, activity_creategoal::class.java))
                R.id.nav_history -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_help -> Toast.makeText(this, "Help Centre", Toast.LENGTH_SHORT).show()
                R.id.nav_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, activity_login::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        fetchFullHistory()
    }

    private fun fetchFullHistory() {
        val userId = auth.currentUser?.uid ?: return
        val transRef = database.getReference("users").child(userId).child("transactions")

        transRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transactionList.clear()
                for (data in snapshot.children) {
                    val transaction = data.getValue(transactions::class.java)
                    if (transaction != null) {
                        transactionList.add(transaction)
                    }
                }
                transactionList.reverse() // Most recent first
                transactionAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TransactionHistoryActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}