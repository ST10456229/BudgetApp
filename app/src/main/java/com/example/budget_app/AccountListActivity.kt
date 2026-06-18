package com.example.budget_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.adapter.AccountAdapter
import com.example.budget_app.model.Account
import com.example.budget_app.utils.Constants
import com.example.budget_app.utils.NavigationHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AccountListActivity : AppCompatActivity() {

    private lateinit var rvAccounts: RecyclerView
    private lateinit var accountAdapter: AccountAdapter
    private val accountList = mutableListOf<Account>()
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val TAG = "AccountListActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_list)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)

        findViewById<android.widget.ImageView>(R.id.ivBack).setOnClickListener { finish() }

        rvAccounts = findViewById(R.id.rvAccounts)
        rvAccounts.layoutManager = LinearLayoutManager(this)
        
        accountAdapter = AccountAdapter(accountList) { account ->
            val intent = Intent(this, AddAccountActivity::class.java)
            intent.putExtra("IS_EDIT", true)
            intent.putExtra("ACCOUNT_ID", account.accountId)
            intent.putExtra("ACCOUNT_NAME", account.name)
            intent.putExtra("ACCOUNT_BALANCE", account.balance)
            intent.putExtra("ACCOUNT_TYPE", account.type)
            startActivity(intent)
        }
        rvAccounts.adapter = accountAdapter

        findViewById<FloatingActionButton>(R.id.fabAddAccount).setOnClickListener {
            startActivity(Intent(this, AddAccountActivity::class.java))
        }

        NavigationHelper.setupNavigation(this)
        fetchAccounts()
    }

    private fun fetchAccounts() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_ACCOUNTS)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing) return
                    accountList.clear()
                    for (data in snapshot.children) {
                        val account = data.getValue(Account::class.java)
                        if (account != null) {
                            val accWithId = account.copy(accountId = data.key ?: "")
                            accountList.add(accWithId)
                        }
                    }
                    accountAdapter.updateList(accountList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error fetching accounts: ${error.message}")
                }
            })
    }
}
