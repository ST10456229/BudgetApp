package com.example.budget_app

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
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
import com.example.budget_app.model.Transaction
import com.example.budget_app.utils.Constants
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var lineChart: LineChart
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var ivProfile: ImageView
    private lateinit var ivBack: ImageView
    private lateinit var drawerLayout: DrawerLayout
    
    private var allTransactions = mutableListOf<Transaction>()
    private val TAG = "ReportsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)

        lineChart = findViewById(R.id.lineChart)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        ivProfile = findViewById(R.id.ivProfile)
        ivBack = findViewById(R.id.ivBack)
        drawerLayout = findViewById(R.id.drawer_layout)

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)

        setupClickListeners(fabAdd, bottomNavigation)
        fetchUserProfile()
        fetchTransactionData()
    }

    private fun setupClickListeners(fabAdd: FloatingActionButton, bottomNavigation: BottomNavigationView) {
        fabAdd.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        ivBack.setOnClickListener {
            finish()
        }

        bottomNavigation.selectedItemId = R.id.nav_reports
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_reports -> true
                R.id.nav_history -> {
                    startActivity(Intent(this, TransactionHistoryActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_more -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
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

    private fun fetchTransactionData() {
        val userId = auth.currentUser?.uid ?: return
        val transRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_TRANSACTIONS)

        transRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing) return
                allTransactions.clear()
                for (data in snapshot.children) {
                    val transaction = data.getValue(Transaction::class.java)
                    if (transaction != null) {
                        allTransactions.add(transaction)
                    }
                }
                setupCharts(allTransactions)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ReportsActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupCharts(transactionList: List<Transaction>) {
        val expenses = transactionList.filter { it.transaction_amount < 0 }
        setupPieChart(expenses)
        setupBarChart(transactionList)
        setupLineChart(transactionList)
    }

    private fun setupPieChart(expenses: List<Transaction>) {
        if (expenses.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("No expense data available")
            return
        }

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        val monthExpenses = expenses.filter {
            val dateParts = it.transaction_date.split("/")
            if (dateParts.size == 3) {
                dateParts[1].toInt() == currentMonth && dateParts[2].toInt() == currentYear
            } else false
        }

        if (monthExpenses.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("No expenses for this month")
            return
        }

        val categoryMap = monthExpenses.groupBy { it.category.category_name }
            .mapValues { entry -> entry.value.sumOf { Math.abs(it.transaction_amount) } }

        val entries = categoryMap.map { PieEntry(it.value.toFloat(), it.key) }

        val dataSet = PieDataSet(entries, "Category Spending")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.centerText = "Monthly Expenses"
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun setupBarChart(transactionList: List<Transaction>) {
        if (transactionList.isEmpty()) {
            barChart.clear()
            return
        }

        val monthMap = mutableMapOf<Int, Pair<Double, Double>>()
        val monthsLabels = mutableListOf<String>()
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        val keys = mutableListOf<Int>()
        for (i in 5 downTo 0) {
            val tempCal = Calendar.getInstance()
            tempCal.add(Calendar.MONTH, -i)
            val m = tempCal.get(Calendar.MONTH) + 1
            val y = tempCal.get(Calendar.YEAR)
            val key = y * 100 + m
            monthMap[key] = Pair(0.0, 0.0)
            keys.add(key)
            monthsLabels.add(monthNames[tempCal.get(Calendar.MONTH)])
        }

        transactionList.forEach {
            val dateParts = it.transaction_date.split("/")
            if (dateParts.size == 3) {
                val m = dateParts[1].toInt()
                val y = dateParts[2].toInt()
                val key = y * 100 + m
                if (monthMap.containsKey(key)) {
                    val current = monthMap[key]!!
                    if (it.transaction_amount > 0) {
                        monthMap[key] = Pair(current.first + it.transaction_amount, current.second)
                    } else {
                        monthMap[key] = Pair(current.first, current.second + Math.abs(it.transaction_amount))
                    }
                }
            }
        }

        val incomeEntries = mutableListOf<BarEntry>()
        val expenseEntries = mutableListOf<BarEntry>()
        
        keys.forEachIndexed { index, key ->
            val data = monthMap[key]!!
            incomeEntries.add(BarEntry(index.toFloat(), data.first.toFloat()))
            expenseEntries.add(BarEntry(index.toFloat(), data.second.toFloat()))
        }

        val incomeSet = BarDataSet(incomeEntries, "Income")
        incomeSet.color = Color.GREEN
        
        val expenseSet = BarDataSet(expenseEntries, "Expenses")
        expenseSet.color = Color.RED

        val barData = BarData(incomeSet, expenseSet)
        barData.barWidth = 0.3f
        
        barChart.data = barData
        barChart.groupBars(0f, 0.4f, 0.02f)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(monthsLabels)
        barChart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.setCenterAxisLabels(true)
        barChart.xAxis.granularity = 1f
        barChart.xAxis.axisMinimum = 0f
        barChart.description.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun setupLineChart(transactionList: List<Transaction>) {
        if (transactionList.isEmpty()) {
            lineChart.clear()
            return
        }

        val sortedTransactions = transactionList.sortedBy { 
            val p = it.transaction_date.split("/")
            if (p.size == 3) p[2].toInt() * 10000 + p[1].toInt() * 100 + p[0].toInt() else 0
        }

        var runningBalance = 0.0
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        sortedTransactions.forEachIndexed { index, transaction ->
            runningBalance += transaction.transaction_amount
            entries.add(Entry(index.toFloat(), runningBalance.toFloat()))
            labels.add(transaction.transaction_date.substringBeforeLast("/"))
        }

        val dataSet = LineDataSet(entries, "Balance Evolution")
        dataSet.color = Color.BLUE
        dataSet.setCircleColor(Color.BLUE)
        dataSet.lineWidth = 2f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.BLUE
        dataSet.fillAlpha = 50
        
        lineChart.data = LineData(dataSet)
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        lineChart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        lineChart.description.isEnabled = false
        lineChart.animateX(1000)
        lineChart.invalidate()
    }
}
