package com.example.budget_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class Createbudget : AppCompatActivity() {
    
    private var drawerLayout: DrawerLayout? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Set content view
        setContentView(R.layout.activity_createbudget)

        auth = FirebaseAuth.getInstance()
        
        // 2. Find and assign views
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        
        if (drawer == null) {
            Toast.makeText(this, "Layout Error", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        this.drawerLayout = drawer

        // 3. Apply window insets to the root drawer layout
        ViewCompat.setOnApplyWindowInsetsListener(drawer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 4. Initialize buttons
        val ivMenu = findViewById<ImageView>(R.id.ivMenu)
        val tvAccountsTab = findViewById<TextView>(R.id.tvAccountsTab)
        val btnCreateBudgets = findViewById<TextView>(R.id.btnCreateBudgets)
        val btnCreateGoals = findViewById<TextView>(R.id.btnCreateGoals)

        // Switch to Accounts (Dashboard)
        tvAccountsTab?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnCreateBudgets?.setOnClickListener {
            Toast.makeText(this, "Budget Creation coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnCreateGoals?.setOnClickListener {
            startActivity(Intent(this, activity_creategoal::class.java))
        }

        ivMenu?.setOnClickListener {
            drawerLayout?.openDrawer(GravityCompat.START)
        }

        // Drawer logic
        navView?.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                R.id.nav_categories -> startActivity(Intent(this, Category::class.java))
                R.id.nav_goals -> startActivity(Intent(this, activity_creategoal::class.java))
                R.id.nav_history -> startActivity(Intent(this, TransactionHistoryActivity::class.java))
                R.id.nav_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, activity_login::class.java))
                    finish()
                }
            }
            drawerLayout?.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onBackPressed() {
        if (drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            drawerLayout?.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}