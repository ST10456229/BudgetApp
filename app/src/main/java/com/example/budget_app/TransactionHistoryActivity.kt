package com.example.budget_app

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.Transaction_Adapter.TransactionAdapter
import com.example.budget_app.model.Transaction
import com.example.budget_app.utils.Constants
import com.example.budget_app.utils.NavigationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var rvTransactionHistory: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)
        rvTransactionHistory = findViewById(R.id.rvTransactionHistory)

        // Setup RecyclerView
        rvTransactionHistory.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(transactionList)
        rvTransactionHistory.adapter = transactionAdapter

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            onBackPressed()
        }

        NavigationHelper.setupNavigation(this)
        fetchFullHistory()
    }

    private fun fetchFullHistory() {
        val userId = auth.currentUser?.uid ?: return
        val transRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_TRANSACTIONS)

        transRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing) return
                transactionList.clear()
                for (data in snapshot.children) {
                    val transaction = data.getValue(Transaction::class.java)
                    if (transaction != null) {
                        if (transaction.transactionId.isEmpty()) transaction.transactionId = data.key ?: ""
                        transactionList.add(transaction)
                    }
                }
                transactionList.reverse()
                transactionAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TransactionHistoryActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
