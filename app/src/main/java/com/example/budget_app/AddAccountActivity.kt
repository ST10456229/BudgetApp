package com.example.budget_app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budget_app.model.Account
import com.example.budget_app.utils.Constants
import com.example.budget_app.utils.NavigationHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddAccountActivity : AppCompatActivity() {

    private lateinit var etAccountName: TextInputEditText
    private lateinit var etBalance: TextInputEditText
    private lateinit var spinnerAccountType: MaterialAutoCompleteTextView
    private lateinit var btnSaveAccount: MaterialButton
    private lateinit var tvTitle: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var isEdit = false
    private var accountId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_account)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)

        etAccountName = findViewById(R.id.etAccountName)
        etBalance = findViewById(R.id.etBalance)
        spinnerAccountType = findViewById(R.id.spinnerAccountType)
        btnSaveAccount = findViewById(R.id.btnSaveAccount)
        tvTitle = findViewById(R.id.tvTitle)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val types = arrayOf("Savings", "Cheque", "Credit Card", "Wallet", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        spinnerAccountType.setAdapter(adapter)

        // Check if we are editing
        isEdit = intent.getBooleanExtra("IS_EDIT", false)
        if (isEdit) {
            accountId = intent.getStringExtra("ACCOUNT_ID")
            etAccountName.setText(intent.getStringExtra("ACCOUNT_NAME"))
            etBalance.setText(intent.getDoubleExtra("ACCOUNT_BALANCE", 0.0).toString())
            spinnerAccountType.setText(intent.getStringExtra("ACCOUNT_TYPE"), false)
            tvTitle.text = "Edit Account"
            btnSaveAccount.text = "Update Account"
        }

        btnSaveAccount.setOnClickListener {
            saveAccount()
        }

        NavigationHelper.setupNavigation(this)
    }

    private fun saveAccount() {
        val name = etAccountName.text.toString().trim()
        val balanceStr = etBalance.text.toString().trim()
        val type = spinnerAccountType.text.toString().trim()

        if (name.isEmpty() || balanceStr.isEmpty() || type.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val balance = balanceStr.toDoubleOrNull() ?: 0.0
        val userId = auth.currentUser?.uid ?: return
        
        val accountsRef = database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_ACCOUNTS)
        val accountRef = if (isEdit && accountId != null) {
            accountsRef.child(accountId!!)
        } else {
            accountsRef.push()
        }

        val account = Account(
            accountId = accountRef.key ?: "",
            name = name,
            balance = balance,
            type = type
        )

        accountRef.setValue(account).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val message = if (isEdit) "Account updated successfully" else "Account added successfully"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to save account", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
