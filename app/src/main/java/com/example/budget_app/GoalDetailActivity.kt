package com.example.budget_app

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budget_app.model.Goal
import com.example.budget_app.utils.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Locale

class GoalDetailActivity : AppCompatActivity() {

    private lateinit var ivGoalDetailImage: ImageView
    private lateinit var tvGoalDetailName: TextView
    private lateinit var tvGoalDetailDate: TextView
    private lateinit var progressGoalDetail: LinearProgressIndicator
    private lateinit var tvGoalDetailCurrent: TextView
    private lateinit var tvGoalDetailTarget: TextView
    private lateinit var etAddAmount: TextInputEditText
    private lateinit var btnAddMoney: MaterialButton

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var goalId: String? = null
    private val TAG = "GoalDetailActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_detail)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)

        goalId = intent.getStringExtra("GOAL_ID")

        ivGoalDetailImage = findViewById(R.id.ivGoalDetailImage)
        tvGoalDetailName = findViewById(R.id.tvGoalDetailName)
        tvGoalDetailDate = findViewById(R.id.tvGoalDetailDate)
        progressGoalDetail = findViewById(R.id.progressGoalDetail)
        tvGoalDetailCurrent = findViewById(R.id.tvGoalDetailCurrent)
        tvGoalDetailTarget = findViewById(R.id.tvGoalDetailTarget)
        etAddAmount = findViewById(R.id.etAddAmount)
        btnAddMoney = findViewById(R.id.btnAddMoney)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        if (goalId != null) {
            fetchGoalDetails()
        } else {
            Toast.makeText(this, "Error: Goal not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnAddMoney.setOnClickListener {
            addMoneyToGoal()
        }
    }

    private fun fetchGoalDetails() {
        val userId = auth.currentUser?.uid ?: return
        val goalRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_GOALS).child(goalId!!)

        goalRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing) return
                val goal = snapshot.getValue(Goal::class.java)
                if (goal != null) {
                    updateUI(goal)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching goal: ${error.message}")
            }
        })
    }

    private fun updateUI(goal: Goal) {
        tvGoalDetailName.text = goal.name
        tvGoalDetailDate.text = "Target Date: ${goal.targetDate}"
        tvGoalDetailCurrent.text = String.format(Locale.US, "R %.2f", goal.currentAmount)
        tvGoalDetailTarget.text = String.format(Locale.US, "of R %.2f", goal.targetAmount)

        val progress = if (goal.targetAmount > 0) {
            ((goal.currentAmount / goal.targetAmount) * 100).toInt()
        } else 0
        progressGoalDetail.progress = progress.coerceAtMost(100)

        if (goal.imageUrl.isNotEmpty()) {
            try {
                ivGoalDetailImage.setImageURI(Uri.parse(goal.imageUrl))
            } catch (e: Exception) {
                ivGoalDetailImage.setImageResource(R.drawable.ic_goals)
            }
        } else {
            ivGoalDetailImage.setImageResource(R.drawable.ic_goals)
        }
    }

    private fun addMoneyToGoal() {
        val amountStr = etAddAmount.text.toString().trim()
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amountToAdd = amountStr.toDoubleOrNull()
        if (amountToAdd == null || amountToAdd <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val goalRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_GOALS).child(goalId!!)

        goalRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val g = mutableData.getValue(Goal::class.java) ?: return Transaction.success(mutableData)
                val newAmount = g.currentAmount + amountToAdd
                mutableData.value = g.copy(currentAmount = newAmount)
                return Transaction.success(mutableData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (committed) {
                    Toast.makeText(this@GoalDetailActivity, "Money added successfully!", Toast.LENGTH_SHORT).show()
                    etAddAmount.text?.clear()
                } else {
                    Toast.makeText(this@GoalDetailActivity, "Failed to add money: ${error?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
