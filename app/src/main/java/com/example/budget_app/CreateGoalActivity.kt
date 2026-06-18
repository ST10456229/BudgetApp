package com.example.budget_app

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.budget_app.model.Goal
import com.example.budget_app.utils.Constants
import com.example.budget_app.utils.NavigationHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream
import java.util.*

class CreateGoalActivity : AppCompatActivity() {

    private lateinit var etGoalName: TextInputEditText
    private lateinit var etTargetAmount: TextInputEditText
    private lateinit var etTargetDate: TextInputEditText
    private lateinit var ivGoalImage: ImageView
    private lateinit var btnSaveGoal: MaterialButton
    private lateinit var tvToolbarTitle: TextView
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var selectedImageUri: Uri? = null
    private var internalImageUri: String = ""
    private var goalId: String? = null
    private var existingGoal: Goal? = null
    
    private val TAG = "CreateGoal"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageSelection(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creategoal)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)

        etGoalName = findViewById(R.id.etGoalName)
        etTargetAmount = findViewById(R.id.etTargetAmount)
        etTargetDate = findViewById(R.id.etTargetDate)
        ivGoalImage = findViewById(R.id.ivGoalImage)
        btnSaveGoal = findViewById(R.id.btnSaveGoal)
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle)

        goalId = intent.getStringExtra("GOAL_ID")

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvGoalImage).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        etTargetDate.setOnClickListener { showDatePicker() }

        if (goalId != null) {
            tvToolbarTitle.text = "Edit Goal"
            btnSaveGoal.text = "Update Goal"
            fetchGoalData()
        }

        btnSaveGoal.setOnClickListener {
            saveGoal()
        }

        NavigationHelper.setupNavigation(this)
    }

    private fun fetchGoalData() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_GOALS).child(goalId!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    existingGoal = snapshot.getValue(Goal::class.java)
                    existingGoal?.let {
                        etGoalName.setText(it.name)
                        etTargetAmount.setText(it.targetAmount.toString())
                        etTargetDate.setText(it.targetDate)
                        if (it.imageUrl.isNotEmpty()) {
                            try {
                                internalImageUri = it.imageUrl
                                ivGoalImage.setImageURI(Uri.parse(it.imageUrl))
                                ivGoalImage.colorFilter = null
                            } catch (e: Exception) {
                                Log.e(TAG, "Error loading image")
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            selectedImageUri = uri
            ivGoalImage.setImageURI(uri)
            ivGoalImage.colorFilter = null
            
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "goal_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
            internalImageUri = Uri.fromFile(file).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling image selection: ${e.message}")
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, day ->
            val date = String.format(Locale.US, "%02d/%02d/%d", day, month + 1, year)
            etTargetDate.setText(date)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun saveGoal() {
        val name = etGoalName.text.toString().trim()
        val amountStr = etTargetAmount.text.toString().trim()
        val date = etTargetDate.text.toString().trim()

        if (name.isEmpty() || amountStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val targetAmount = amountStr.toDoubleOrNull()
        if (targetAmount == null || targetAmount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: return
        
        btnSaveGoal.isEnabled = false
        val goalRef = if (goalId != null) {
            database.getReference(Constants.PATH_USERS).child(user.uid).child(Constants.PATH_GOALS).child(goalId!!)
        } else {
            database.getReference(Constants.PATH_USERS).child(user.uid).child(Constants.PATH_GOALS).push()
        }

        val goal = Goal(
            goalId = goalRef.key ?: UUID.randomUUID().toString(),
            name = name,
            targetAmount = targetAmount,
            currentAmount = existingGoal?.currentAmount ?: 0.0,
            targetDate = date,
            imageUrl = internalImageUri
        )

        goalRef.setValue(goal).addOnCompleteListener { task ->
            btnSaveGoal.isEnabled = true
            if (task.isSuccessful) {
                Toast.makeText(this, if (goalId != null) "Goal Updated!" else "Goal Created!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
