package com.example.budget_app

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.budget_app.model.Category
import com.example.budget_app.model.transactions
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etAmount: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etDate: TextInputEditText
    private lateinit var btnSelectImage: Button
    private lateinit var ivReceiptPreview: ImageView
    private lateinit var btnSaveExpense: Button
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            ivReceiptPreview.setImageURI(it)
            ivReceiptPreview.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize UI components
        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etDate = findViewById(R.id.etDate)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        ivReceiptPreview = findViewById(R.id.ivReceiptPreview)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)

        // Setup Spinner with sample categories
        val categories = arrayOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Date Picker
        etDate.setOnClickListener {
            showDatePicker()
        }

        // Set click listener for Save Button
        btnSaveExpense.setOnClickListener {
            saveExpense()
        }

        // Make Image Selection functional
        btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            etDate.setText(date)
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun saveExpense() {
        val amountStr = etAmount.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val categoryName = spinnerCategory.selectedItem.toString()
        val date = etDate.text.toString().trim()

        // Validation
        if (amountStr.isEmpty()) {
            etAmount.error = "Please enter an amount"
            return
        }

        if (description.isEmpty()) {
            etDescription.error = "Please enter a description"
            return
        }
        
        if (date.isEmpty()) {
            etDate.error = "Please select a date"
            return
        }

        val amount = amountStr.toDoubleOrNull() ?: 0.0
        val userId = auth.currentUser?.uid ?: return
        val transRef = database.getReference("users").child(userId).child("transactions").push()

        val transaction = transactions(
            transaction_name = description,
            category = Category(category_name = categoryName),
            transaction_amamount = amount,
            transaction_date = date
        )

        transRef.setValue(transaction).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Expense Saved Successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to save expense", Toast.LENGTH_SHORT).show()
            }
        }
    }
}