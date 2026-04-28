package com.example.budget_app

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budget_app.model.Account
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddAccountActivity : AppCompatActivity() {

    private lateinit var etAccountName: TextInputEditText
    private lateinit var etBalance: TextInputEditText
    private lateinit var btnSaveAccount: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_account)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        etAccountName = findViewById(R.id.etAccountName)
        etBalance = findViewById(R.id.etBalance)
        btnSaveAccount = findViewById(R.id.btnSaveAccount)

        btnSaveAccount.setOnClickListener {
            saveAccount()
        }
    }

    private fun saveAccount() {
        val name = etAccountName.text.toString().trim()
        val balanceStr = etBalance.text.toString().trim()

        if (name.isEmpty() || balanceStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val balance = balanceStr.toDoubleOrNull() ?: 0.0
        val userId = auth.currentUser?.uid ?: return
        val accountRef = database.getReference("users").child(userId).child("accounts").push()

        val account = Account(
            accountId = accountRef.key ?: "",
            name = name,
            balance = balance,
            type = "Custom"
        )

        accountRef.setValue(account).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Account added successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to add account", Toast.LENGTH_SHORT).show()
            }
        }
    }
}