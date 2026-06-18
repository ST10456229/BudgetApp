package com.example.budget_app

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.budget_app.model.Budget
import com.example.budget_app.model.Transaction
import com.example.budget_app.utils.Constants
import com.example.budget_app.utils.GamificationManager
import com.example.budget_app.utils.NavigationHelper
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
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
    private lateinit var btnViewAchievements: MaterialButton
    private lateinit var toggleChartType: MaterialButtonToggleGroup
    
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalSavings: TextView
    private lateinit var tvChartTitle: TextView
    
    private var allTransactions = mutableListOf<Transaction>()
    private var budgetList = mutableListOf<Budget>()
    private lateinit var gamificationManager: GamificationManager
    private val TAG = "ReportsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)
        gamificationManager = GamificationManager.getInstance(this)

        lineChart = findViewById(R.id.lineChart)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        ivProfile = findViewById(R.id.ivProfile)
        ivBack = findViewById(R.id.ivBack)
        btnViewAchievements = findViewById(R.id.btnViewAchievements)
        toggleChartType = findViewById(R.id.toggleChartType)
        
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalSavings = findViewById(R.id.tvTotalSavings)
        tvChartTitle = findViewById(R.id.tvChartTitle)

        NavigationHelper.setupNavigation(this)
        setupClickListeners()
        fetchUserProfile()
        fetchData()
        
        toggleChartType.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) {
                updatePieChart()
            }
        }
    }

    private fun setupClickListeners() {
        ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        ivBack.setOnClickListener {
            finish()
        }

        btnViewAchievements.setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
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
        val transRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_TRANSACTIONS)
        val budgetRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_BUDGETS)

        // Fetch Transactions
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
                updateSummary(allTransactions)
                setupCharts(allTransactions)
                checkBudgetCompliance()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Fetch Budgets
        budgetRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing) return
                budgetList.clear()
                for (data in snapshot.children) {
                    val budget = data.getValue(Budget::class.java)
                    if (budget != null) budgetList.add(budget)
                }
                checkBudgetCompliance()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * Optimized check to verify the PREVIOUS month's budget compliance.
     * This prevents users from unlocking monthly achievements by opening 
     * the app on the first day of a new month.
     */
    private fun checkBudgetCompliance() {
        if (allTransactions.isEmpty() || budgetList.isEmpty()) return

        val calendar = Calendar.getInstance()
        // Check previous month
        calendar.add(Calendar.MONTH, -1)
        
        val targetMonth = calendar.get(Calendar.MONTH) + 1
        val targetYear = calendar.get(Calendar.YEAR)
        val monthIndex = calendar.get(Calendar.MONTH)

        var allBudgetsMet = true
        var hasAtLeastOneMonthlyBudget = false

        for (budget in budgetList) {
            if (budget.period != "Monthly") continue
            hasAtLeastOneMonthlyBudget = true
            
            var spent = 0.0
            for (trans in allTransactions) {
                if (trans.transaction_amount < 0) {
                    val dateParts = trans.transaction_date.split("/")
                    if (dateParts.size == 3) {
                        try {
                            if (dateParts[1].toInt() == targetMonth && dateParts[2].toInt() == targetYear) {
                                if (trans.category.category_name.equals(budget.name, ignoreCase = true)) {
                                    spent += Math.abs(trans.transaction_amount)
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
            
            if (spent > budget.targetAmount) {
                allBudgetsMet = false
                break
            }
        }

        if (hasAtLeastOneMonthlyBudget) {
            gamificationManager.recordBudgetCompliance(allBudgetsMet, monthIndex, targetYear)
        }
    }

    private fun updateSummary(transactionList: List<Transaction>) {
        var income = 0.0
        var expenses = 0.0
        
        transactionList.forEach { 
            if (it.transaction_amount > 0) income += it.transaction_amount
            else expenses += Math.abs(it.transaction_amount)
        }
        
        tvTotalIncome.text = String.format(Locale.US, "R %.2f", income)
        tvTotalExpenses.text = String.format(Locale.US, "R %.2f", expenses)
        tvTotalSavings.text = String.format(Locale.US, "R %.2f", income - expenses)
    }

    private fun setupCharts(transactionList: List<Transaction>) {
        updatePieChart()
        setupBarChart(transactionList)
        setupLineChart(transactionList)
    }

    private fun updatePieChart() {
        when (toggleChartType.checkedButtonId) {
            R.id.btnShowIncome -> {
                tvChartTitle.text = "Income Sources"
                val filtered = allTransactions.filter { it.transaction_amount > 0 }
                setupBreakdownPieChart(filtered, true)
            }
            R.id.btnShowSummary -> {
                tvChartTitle.text = "Financial Summary"
                setupSummaryPieChart()
            }
            else -> { // btnShowExpenses
                tvChartTitle.text = "Spending Breakdown"
                val filtered = allTransactions.filter { it.transaction_amount < 0 }
                setupBreakdownPieChart(filtered, false)
            }
        }
    }

    private fun setupSummaryPieChart() {
        var totalIncome = 0.0
        var totalExpenses = 0.0
        
        allTransactions.forEach {
            if (it.transaction_amount > 0) totalIncome += it.transaction_amount
            else totalExpenses += Math.abs(it.transaction_amount)
        }

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        if (totalIncome > 0) {
            entries.add(PieEntry(totalIncome.toFloat(), "Income"))
            colors.add(ContextCompat.getColor(this, R.color.success_green))
        }
        if (totalExpenses > 0) {
            entries.add(PieEntry(totalExpenses.toFloat(), "Expenses"))
            colors.add(Color.parseColor("#F1C40F")) // Yellow
        }

        renderPieChart(entries, colors)
    }

    private fun setupBreakdownPieChart(filteredTransactions: List<Transaction>, isIncome: Boolean) {
        val primaryTextColor = getThemeColor(android.R.attr.textColorPrimary)

        if (filteredTransactions.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText(if (isIncome) "No income data available" else "No expense data available")
            pieChart.setNoDataTextColor(primaryTextColor)
            return
        }

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        
        val monthData = filteredTransactions.filter {
            val dateParts = it.transaction_date.split("/")
            if (dateParts.size == 3) {
                try {
                    dateParts[1].toInt() == currentMonth && dateParts[2].toInt() == currentYear
                } catch (e: NumberFormatException) { false }
            } else false
        }

        val dataToUse = if (monthData.isEmpty()) filteredTransactions else monthData

        val categoryGroups = dataToUse.groupBy { it.category.category_name }
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        categoryGroups.forEach { (name, transList) ->
            val total = transList.sumOf { Math.abs(it.transaction_amount) }
            entries.add(PieEntry(total.toFloat(), name))
            
            // If category has a specific color, use it. Otherwise use shaded palette.
            val colorStr = transList.first().category.colorCode
            if (!colorStr.isNullOrEmpty() && colorStr.startsWith("#")) {
                try {
                    colors.add(Color.parseColor(colorStr))
                } catch (e: Exception) {
                    colors.add(getThemeShadedColor(isIncome, colors.size))
                }
            } else {
                colors.add(getThemeShadedColor(isIncome, colors.size))
            }
        }

        renderPieChart(entries, colors)
    }

    private fun renderPieChart(entries: List<PieEntry>, colors: List<Int>) {
        val primaryTextColor = getThemeColor(android.R.attr.textColorPrimary)
        
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 13f
        dataSet.sliceSpace = 2f
        dataSet.selectionShift = 8f
        dataSet.valueFormatter = PercentFormatter(pieChart)

        pieChart.data = PieData(dataSet)
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.holeRadius = 40f
        pieChart.transparentCircleRadius = 45f
        
        pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(11f)

        pieChart.legend.apply {
            isEnabled = true
            textColor = primaryTextColor
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
            isWordWrapEnabled = true
            form = Legend.LegendForm.CIRCLE
        }
        
        pieChart.animateY(1400, Easing.EaseInOutQuad)
        pieChart.invalidate()
    }

    private fun getThemeShadedColor(isIncome: Boolean, index: Int): Int {
        return if (isIncome) {
            // Shades of Green
            val greenPalette = intArrayOf(
                Color.parseColor("#2ECC71"), Color.parseColor("#27AE60"), 
                Color.parseColor("#16A085"), Color.parseColor("#1ABC9C"),
                Color.parseColor("#48C9B0"), Color.parseColor("#52BE80")
            )
            greenPalette[index % greenPalette.size]
        } else {
            // Shades of Yellow/Orange
            val yellowPalette = intArrayOf(
                Color.parseColor("#F1C40F"), Color.parseColor("#F39C12"),
                Color.parseColor("#D4AC0D"), Color.parseColor("#B7950B"),
                Color.parseColor("#F4D03F"), Color.parseColor("#F5B041")
            )
            yellowPalette[index % yellowPalette.size]
        }
    }

    private fun getDistinctColor(index: Int): Int {
        val palette = intArrayOf(
            Color.parseColor("#4A86E8"), Color.parseColor("#C0392B"), Color.parseColor("#8BC34A"),
            Color.parseColor("#9B59B6"), Color.parseColor("#16A085"), Color.parseColor("#F39C12"),
            Color.parseColor("#2980B9"), Color.parseColor("#D35400"), Color.parseColor("#27AE60")
        )
        return palette[index % palette.size]
    }

    private fun setupBarChart(transactionList: List<Transaction>) {
        val primaryTextColor = getThemeColor(android.R.attr.textColorPrimary)
        val dividerColor = getThemeColor(android.R.attr.dividerHorizontal)

        if (transactionList.isEmpty()) {
            barChart.clear()
            barChart.setNoDataTextColor(primaryTextColor)
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
                try {
                    val m = dateParts[1].toInt()
                    val y = dateParts[2].toInt()
                    val key = y * 100 + m
                    if (monthMap.containsKey(key)) {
                        val current = monthMap[key] ?: Pair(0.0, 0.0)
                        if (it.transaction_amount > 0) {
                            monthMap[key] = Pair(current.first + it.transaction_amount, current.second)
                        } else {
                            monthMap[key] = Pair(current.first, current.second + Math.abs(it.transaction_amount))
                        }
                    }
                } catch (e: NumberFormatException) {}
            }
        }

        val incomeEntries = mutableListOf<BarEntry>()
        val expenseEntries = mutableListOf<BarEntry>()
        
        keys.forEachIndexed { index, key ->
            val data = monthMap[key] ?: Pair(0.0, 0.0)
            incomeEntries.add(BarEntry(index.toFloat(), data.first.toFloat()))
            expenseEntries.add(BarEntry(index.toFloat(), data.second.toFloat()))
        }

        val incomeSet = BarDataSet(incomeEntries, "Income")
        incomeSet.color = ContextCompat.getColor(this, R.color.success_green)
        incomeSet.valueTextColor = primaryTextColor
        
        val expenseSet = BarDataSet(expenseEntries, "Expenses")
        expenseSet.color = ContextCompat.getColor(this, R.color.red)
        expenseSet.valueTextColor = primaryTextColor

        val barData = BarData(incomeSet, expenseSet)
        barData.barWidth = 0.3f
        
        barChart.data = barData
        barChart.groupBars(0f, 0.4f, 0.02f)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(monthsLabels)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.setCenterAxisLabels(true)
        barChart.xAxis.granularity = 1f
        barChart.xAxis.axisMinimum = 0f
        barChart.xAxis.axisMaximum = keys.size.toFloat()
        barChart.xAxis.textColor = primaryTextColor
        barChart.xAxis.setDrawGridLines(false)
        barChart.axisLeft.textColor = primaryTextColor
        barChart.axisLeft.gridColor = dividerColor
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.legend.textColor = primaryTextColor
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun setupLineChart(transactionList: List<Transaction>) {
        val primaryTextColor = getThemeColor(android.R.attr.textColorPrimary)
        val dividerColor = getThemeColor(android.R.attr.dividerHorizontal)

        if (transactionList.isEmpty()) {
            lineChart.clear()
            lineChart.setNoDataTextColor(primaryTextColor)
            return
        }

        val sortedTransactions = transactionList.sortedBy { 
            val p = it.transaction_date.split("/")
            if (p.size == 3) {
                try {
                    p[2].toInt() * 10000 + p[1].toInt() * 100 + p[0].toInt()
                } catch (e: NumberFormatException) { 0 }
            } else 0
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
        dataSet.apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(this@ReportsActivity, R.drawable.line_gradient)
            color = ContextCompat.getColor(this@ReportsActivity, R.color.primary_blue)
            lineWidth = 3f
            setCircleColor(ContextCompat.getColor(this@ReportsActivity, R.color.primary_blue))
            circleRadius = 5f
            setDrawCircleHole(false)
            valueTextColor = primaryTextColor
            valueTextSize = 0f 
        }
        
        lineChart.data = LineData(dataSet)
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.textColor = primaryTextColor
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.axisLeft.textColor = primaryTextColor
        lineChart.axisRight.isEnabled = false
        lineChart.axisLeft.gridColor = dividerColor
        lineChart.axisLeft.setDrawAxisLine(false)
        lineChart.description.isEnabled = false
        lineChart.legend.textColor = primaryTextColor
        lineChart.animateX(1000)
        lineChart.invalidate()
    }

    private fun getThemeColor(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}
