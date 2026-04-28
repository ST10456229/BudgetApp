package com.example.budget_app

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budget_app.model.Goal
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class activity_creategoal : AppCompatActivity() {

    private lateinit var etAmount: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etDate: TextInputEditText
    private lateinit var btnSelectImage: Button
    private lateinit var btnSaveGoal: Button
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense) // Reusing the same layout for now

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize UI components
        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etDate = findViewById(R.id.etDate)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnSaveGoal = findViewById(R.id.btnSaveExpense)

        findViewById<android.widget.TextView>(R.id.tvAddExpenseTitle).text = "Create New Goal"
        btnSaveGoal.text = "Save Goal"
        btnSelectImage.text = "Upload Goal Image"

        // Setup Spinner with sample categories
        val categories = arrayOf("Savings", "Travel", "House", "Car", "Education", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Date Picker
        etDate.setOnClickListener {
            showDatePicker()
        }

        // Set click listener for Save Button
        btnSaveGoal.setOnClickListener {
            saveGoal()
        }

        // Image Selection
        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri? = data.data
            Toast.makeText(this, "Image Selected: $imageUri", Toast.LENGTH_SHORT).show()
            // In a real app, upload this to Firebase Storage
        }
    }

    private fun saveGoal() {
        val amountStr = etAmount.text.toString().trim()
        val name = etDescription.text.toString().trim()
        val date = etDate.text.toString().trim()

        if (amountStr.isEmpty() || name.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val targetAmount = amountStr.toDoubleOrNull() ?: 0.0
        val userId = auth.currentUser?.uid ?: return
        val goalRef = database.getReference("users").child(userId).child("goals").push()

        val goal = Goal(
            goalId = goalRef.key ?: "",
            name = name,
            targetAmount = targetAmount,
            currentAmount = 0.0,
            targetDate = date
        )

        goalRef.setValue(goal).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Goal Created Successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to create goal", Toast.LENGTH_SHORT).show()
            }
        }
    }
}