package com.example.budget_app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.utils.NavigationHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class Category : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var tabExpenses: View
    private lateinit var tabIncome: View
    private lateinit var tvExpensesTab: TextView
    private lateinit var tvIncomeTab: TextView
    private lateinit var indicatorExpenses: View
    private lateinit var indicatorIncome: View
    private lateinit var auth: FirebaseAuth
    private lateinit var fabAddCategory: FloatingActionButton

    private var isExpensesTabSelected = true

    private val activeColor = Color.parseColor("#0056D2")
    private val inactiveColor = Color.parseColor("#666666")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category)

        auth = FirebaseAuth.getInstance()

        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
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
        fabAddCategory = findViewById(R.id.fabAdd)
        
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val ivProfile = findViewById<ImageView>(R.id.ivProfile)
        ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // 1. Setup global navigation first (this sets the default FAB listener)
        NavigationHelper.setupNavigation(this)

        // 2. Override the FAB listener for this specific screen AFTER the helper setup
        // This ensures the "Add Category" toast is shown instead of opening AddExpenseActivity
        fabAddCategory.setOnClickListener {
            Toast.makeText(this, "Add new category feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // 3. Ensure RecyclerView has a LayoutManager
        rvCategories.layoutManager = LinearLayoutManager(this)

        setupTabs()
        updateCategoryList()
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
        if (isExpensesTabSelected) {
            tvExpensesTab.setTextColor(activeColor)
            tvExpensesTab.setTypeface(null, android.graphics.Typeface.BOLD)
            indicatorExpenses.setBackgroundColor(activeColor)
            indicatorExpenses.visibility = View.VISIBLE
            
            tvIncomeTab.setTextColor(inactiveColor)
            tvIncomeTab.setTypeface(null, android.graphics.Typeface.NORMAL)
            indicatorIncome.visibility = View.INVISIBLE
        } else {
            tvExpensesTab.setTextColor(inactiveColor)
            tvExpensesTab.setTypeface(null, android.graphics.Typeface.NORMAL)
            indicatorExpenses.visibility = View.INVISIBLE
            
            tvIncomeTab.setTextColor(activeColor)
            tvIncomeTab.setTypeface(null, android.graphics.Typeface.BOLD)
            indicatorIncome.setBackgroundColor(activeColor)
            indicatorIncome.visibility = View.VISIBLE
        }
    }

    private fun updateCategoryList() {
        val categories = if (isExpensesTabSelected) {
            getExpenseCategories()
        } else {
            getIncomeCategories()
        }
        rvCategories.adapter = CategoryAdapter(categories)
    }

    private fun getExpenseCategories(): List<CategoryItem> {
        return listOf(
            CategoryItem("Food & Drink", "#FF9800", android.R.drawable.ic_menu_gallery, "Daily dining and takeout"),
            CategoryItem("Groceries", "#4CAF50", android.R.drawable.ic_menu_gallery, "Weekly home supplies"),
            CategoryItem("Shopping", "#E91E63", android.R.drawable.ic_menu_gallery, "Clothing, electronics, etc."),
            CategoryItem("Transport", "#0056D2", android.R.drawable.ic_menu_gallery, "Fuel, public transport, taxi"),
            CategoryItem("Housing", "#795548", android.R.drawable.ic_menu_gallery, "Rent, mortgage, repairs"),
            CategoryItem("Bills & Utilities", "#2196F3", android.R.drawable.ic_menu_gallery, "Electricity, water, internet"),
            CategoryItem("Entertainment", "#9C27B0", android.R.drawable.ic_menu_gallery, "Movies, games, events"),
            CategoryItem("Health & Wellness", "#F44336", android.R.drawable.ic_menu_gallery, "Gym, pharmacy, doctor"),
            CategoryItem("Education", "#607D8B", android.R.drawable.ic_menu_gallery, "Courses, books, tuition"),
            CategoryItem("Personal Care", "#FF4081", android.R.drawable.ic_menu_gallery, "Haircut, cosmetics, spa"),
            CategoryItem("Insurance", "#3F51B5", android.R.drawable.ic_menu_gallery, "Health, car, home insurance"),
            CategoryItem("Travel", "#00BCD4", android.R.drawable.ic_menu_gallery, "Flights, hotels, vacations")
        )
    }

    private fun getIncomeCategories(): List<CategoryItem> {
        return listOf(
            CategoryItem("Salary", "#4CAF50", android.R.drawable.ic_menu_gallery, "Monthly work paycheck"),
            CategoryItem("Freelance", "#0056D2", android.R.drawable.ic_menu_gallery, "Project-based earnings"),
            CategoryItem("Business", "#FF9800", android.R.drawable.ic_menu_gallery, "Side hustle or store profit"),
            CategoryItem("Investment", "#FFC107", android.R.drawable.ic_menu_gallery, "Stocks, crypto, dividends"),
            CategoryItem("Gifts", "#E91E63", android.R.drawable.ic_menu_gallery, "Birthday or holiday money"),
            CategoryItem("Rental Income", "#795548", android.R.drawable.ic_menu_gallery, "Property lease payments"),
            CategoryItem("Others", "#9E9E9E", android.R.drawable.ic_menu_gallery, "Miscellaneous income sources")
        )
    }

    data class CategoryItem(val name: String, val colorCode: String, val iconRes: Int, val subtitle: String)

    inner class CategoryAdapter(private val items: List<CategoryItem>) :
        RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvCategoryName)
            val tvSubtitle: TextView = view.findViewById(R.id.tvCategorySubtitle)
            val ivIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
            val cvIconBackground: CardView = view.findViewById(R.id.cvIconBackground)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.category_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.tvSubtitle.text = item.subtitle
            holder.ivIcon.setImageResource(item.iconRes)
            holder.cvIconBackground.setCardBackgroundColor(Color.parseColor(item.colorCode))
        }

        override fun getItemCount() = items.size
    }
}
