package com.example.budget_app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Addexpense : AppCompatActivity() {

    private lateinit var etBudgetName: EditText
    private lateinit var etTargetAmount: EditText
    private lateinit var etMinGoal: EditText
    private lateinit var etMaxGoal: EditText
    private lateinit var spinnerPeriod: Spinner
    private lateinit var btnSaveBudget: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_addexpense)

        // Find views
        etBudgetName = findViewById(R.id.etBudgetName)
        etTargetAmount = findViewById(R.id.etTargetAmount)
        etMinGoal = findViewById(R.id.etMinGoal)
        etMaxGoal = findViewById(R.id.etMaxGoal)
        spinnerPeriod = findViewById(R.id.spinnerPeriod)
        btnSaveBudget = findViewById(R.id.btnSaveBudget)

        // Setup Spinner
        val periods = arrayOf("Daily", "Weekly", "Monthly", "Yearly")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPeriod.adapter = adapter
        spinnerPeriod.setSelection(2) // Default to Monthly

        btnSaveBudget.setOnClickListener {
            saveBudget()
        }
    }

    private fun saveBudget() {
        val name = etBudgetName.text.toString().trim()
        val target = etTargetAmount.text.toString().trim()
        val period = spinnerPeriod.selectedItem.toString()
        val min = etMinGoal.text.toString().trim()
        val max = etMaxGoal.text.toString().trim()

        if (name.isEmpty() || target.isEmpty()) {
            Toast.makeText(this, "Please fill in at least the name and target amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Logic to save to Firebase
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val database = FirebaseDatabase.getInstance().getReference("Budgets").child(userId)
            val budgetId = database.push().key ?: return

            val budgetData = mapOf(
                "id" to budgetId,
                "name" to name,
                "targetAmount" to target,
                "period" to period,
                "minGoal" to min,
                "maxGoal" to max,
                "timestamp" to System.currentTimeMillis()
            )

            database.child(budgetId).setValue(budgetData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Budget saved successfully!", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save budget: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFields() {
        etBudgetName.text.clear()
        etTargetAmount.text.clear()
        etMinGoal.text.clear()
        etMaxGoal.text.clear()
    }
}