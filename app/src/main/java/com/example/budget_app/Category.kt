package com.example.budget_app

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.annotation.AttrRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.model.Category as CategoryModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File

class Category : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var tabExpenses: View
    private lateinit var tabIncome: View
    private lateinit var tvExpensesTab: TextView
    private lateinit var tvIncomeTab: TextView
    private lateinit var indicatorExpenses: View
    private lateinit var indicatorIncome: View
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var fabAddCategory: FloatingActionButton
    private lateinit var ivProfile: ImageView
    private lateinit var ivBack: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    private var isExpensesTabSelected = true
    private val categoryList = mutableListOf<CategoryModel>()
    private val TAG = "CategoryActivity"
    private val DB_URL = "https://budgetapp2-6ab44-default-rtdb.europe-west1.firebasedatabase.app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(DB_URL)

        drawerLayout = findViewById(R.id.drawer_layout)
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Views
        rvCategories = findViewById(R.id.rv_categories)
        tabExpenses = findViewById(R.id.tabExpenses)
        tabIncome = findViewById(R.id.tabIncome)
        tvExpensesTab = findViewById(R.id.tvExpensesTab)
        tvIncomeTab = findViewById(R.id.tvIncomeTab)
        indicatorExpenses = findViewById(R.id.indicatorExpenses)
        indicatorIncome = findViewById(R.id.indicatorIncome)
        ivProfile = findViewById(R.id.ivProfile)
        ivBack = findViewById(R.id.ivBack)
        navView = findViewById(R.id.nav_view)
        
        fabAddCategory = findViewById(R.id.fabAdd)
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        ivBack.setOnClickListener { finish() }

        ivProfile.setOnClickListener {
            startActivity(Intent(this, Addexpense::class.java))
        }

        fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        // Navigation Drawer Setup
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java)); finish() }
                R.id.nav_reports -> { startActivity(Intent(this, ReportsActivity::class.java)); finish() }
                R.id.nav_history -> { startActivity(Intent(this, TransactionHistoryActivity::class.java)); finish() }
                R.id.nav_categories -> { drawerLayout.closeDrawer(GravityCompat.START) }
                R.id.nav_settings -> { startActivity(Intent(this, Addexpense::class.java)); finish() }
                R.id.nav_logout -> {
                    auth.signOut()
                    val intent = Intent(this, activity_login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Bottom Navigation Logic
        bottomNavigation.selectedItemId = R.id.nav_home // Ideally this would be a 'categories' ID if it existed in the menu
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java)); finish(); true }
                R.id.nav_reports -> { startActivity(Intent(this, ReportsActivity::class.java)); finish(); true }
                R.id.nav_history -> { startActivity(Intent(this, TransactionHistoryActivity::class.java)); finish(); true }
                R.id.nav_more -> { startActivity(Intent(this, Addexpense::class.java)); true }
                else -> false
            }
        }

        setupTabs()
        fetchUserProfile()
        fetchCategories()
    }

    private fun fetchUserProfile() {
        val user = auth.currentUser ?: return
        val userRef = database.getReference("users").child(user.uid).child("profile")
        
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing) return
                
                val username = snapshot.child("username").getValue(String::class.java) ?: user.displayName
                val profilePic = snapshot.child("profilePic").getValue(String::class.java)
                val email = user.email ?: ""

                if (!profilePic.isNullOrEmpty()) {
                    try {
                        val uri = Uri.parse(profilePic)
                        val path = uri.path
                        if (path != null && File(path).exists()) {
                            ivProfile.setImageBitmap(BitmapFactory.decodeFile(path))
                        } else {
                            ivProfile.setImageURI(uri)
                        }
                        ivProfile.colorFilter = null
                    } catch (e: Exception) {}
                }

                // Update Navigation Header
                val headerView = navView.getHeaderView(0)
                if (headerView != null) {
                    val ivNavProfile = headerView.findViewById<ImageView>(R.id.ivNavProfile)
                    val tvNavUsername = headerView.findViewById<TextView>(R.id.tvNavUsername)
                    val tvNavEmail = headerView.findViewById<TextView>(R.id.tvNavEmail)

                    tvNavUsername?.text = username ?: "User"
                    tvNavEmail?.text = email

                    if (!profilePic.isNullOrEmpty() && ivNavProfile != null) {
                        try {
                            val path = Uri.parse(profilePic).path
                            if (path != null && File(path).exists()) {
                                ivNavProfile.setImageBitmap(BitmapFactory.decodeFile(path))
                                ivNavProfile.colorFilter = null
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showAddCategoryDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Category")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etName = EditText(this).apply { hint = "Category Name" }
        val etDesc = EditText(this).apply { hint = "Description (Optional)" }

        layout.addView(etName)
        layout.addView(etDesc)
        builder.setView(layout)

        builder.setPositiveButton("Add") { _, _ ->
            val name = etName.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            if (name.isNotEmpty()) {
                saveCategoryToFirebase(name, desc)
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun saveCategoryToFirebase(name: String, description: String) {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val categoryRef = database.getReference("users").child(userId).child("custom_categories").push()
        
        val type = if (isExpensesTabSelected) "Expense" else "Income"
        val category = CategoryModel(
            categoryId = categoryRef.key ?: "",
            category_name = name,
            type = type,
            colorCode = "#0056D2",
            description = description
        )

        categoryRef.setValue(category).addOnSuccessListener {
            Toast.makeText(this, "Category added successfully!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to add category: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        tabExpenses.setOnClickListener {
            if (!isExpensesTabSelected) {
                isExpensesTabSelected = true
                updateTabUI()
                updateCategoryList()
            }
        }

        tabIncome.setOnClickListener {
            if (isExpensesTabSelected) {
                isExpensesTabSelected = false
                updateTabUI()
                updateCategoryList()
            }
        }
    }

    private fun updateTabUI() {
        val primaryColor = ContextCompat.getColor(this, R.color.primary_blue)
        val textSecondaryColor = getThemeColor(android.R.attr.textColorSecondary)

        if (isExpensesTabSelected) {
            tvExpensesTab.setTextColor(primaryColor)
            tvExpensesTab.setTypeface(null, android.graphics.Typeface.BOLD)
            indicatorExpenses.setBackgroundColor(primaryColor)
            indicatorExpenses.visibility = View.VISIBLE
            
            tvIncomeTab.setTextColor(textSecondaryColor)
            tvIncomeTab.setTypeface(null, android.graphics.Typeface.NORMAL)
            indicatorIncome.visibility = View.INVISIBLE
        } else {
            tvExpensesTab.setTextColor(textSecondaryColor)
            tvExpensesTab.setTypeface(null, android.graphics.Typeface.NORMAL)
            indicatorExpenses.visibility = View.INVISIBLE
            
            tvIncomeTab.setTextColor(primaryColor)
            tvIncomeTab.setTypeface(null, android.graphics.Typeface.BOLD)
            indicatorIncome.setBackgroundColor(primaryColor)
            indicatorIncome.visibility = View.VISIBLE
        }
    }

    private fun getThemeColor(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun fetchCategories() {
        val userId = auth.currentUser?.uid ?: return
        val customCategoriesRef = database.getReference("users").child(userId).child("custom_categories")

        customCategoriesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryList.clear()
                categoryList.addAll(getDefaultCategories())

                for (data in snapshot.children) {
                    val category = data.getValue(CategoryModel::class.java)
                    if (category != null) {
                        categoryList.add(category)
                    }
                }
                updateCategoryList()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Category, "Error loading categories: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateCategoryList() {
        val filteredList = categoryList.filter { 
            val currentType = if (isExpensesTabSelected) "Expense" else "Income"
            it.type == currentType
        }
        rvCategories.adapter = CategoryAdapter(filteredList)
    }

    private fun getDefaultCategories(): List<CategoryModel> {
        val defaults = mutableListOf<CategoryModel>()
        defaults.add(CategoryModel("d1", "Food & Drink", "Expense", "#FF9800", "Daily dining and takeout"))
        defaults.add(CategoryModel("d2", "Groceries", "Expense", "#4CAF50", "Weekly home supplies"))
        defaults.add(CategoryModel("d3", "Shopping", "Expense", "#E91E63", "Clothing, electronics, etc."))
        defaults.add(CategoryModel("d4", "Transport", "Expense", "#0056D2", "Fuel, public transport, taxi"))
        defaults.add(CategoryModel("d5", "Housing", "Expense", "#795548", "Rent, mortgage, repairs"))
        defaults.add(CategoryModel("d6", "Bills & Utilities", "Expense", "#2196F3", "Electricity, water, internet"))
        defaults.add(CategoryModel("d7", "Entertainment", "Expense", "#9C27B0", "Movies, games, events"))
        defaults.add(CategoryModel("d8", "Health & Wellness", "Expense", "#F44336", "Gym, pharmacy, doctor"))
        defaults.add(CategoryModel("i1", "Salary", "Income", "#4CAF50", "Monthly work paycheck"))
        defaults.add(CategoryModel("i2", "Freelance", "Income", "#0056D2", "Project-based earnings"))
        defaults.add(CategoryModel("i3", "Business", "Income", "#FF9800", "Side hustle or store profit"))
        defaults.add(CategoryModel("i4", "Investment", "Income", "#FFC107", "Stocks, crypto, dividends"))
        return defaults
    }

    inner class CategoryAdapter(private val items: List<CategoryModel>) :
        RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvCategoryName)
            val tvSubtitle: TextView = view.findViewById(R.id.tvCategorySubtitle)
            val ivIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
            val cvIconBackground: CardView = view.findViewById(R.id.cvIconBackground)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.category_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.category_name
            holder.tvSubtitle.text = item.description
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_gallery)
            holder.ivIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)
            try {
                holder.cvIconBackground.setCardBackgroundColor(Color.parseColor(item.colorCode))
            } catch (e: Exception) {
                holder.cvIconBackground.setCardBackgroundColor(ContextCompat.getColor(this@Category, R.color.primary_blue))
            }
        }

        override fun getItemCount() = items.size
    }
}