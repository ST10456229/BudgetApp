package com.example.budget_app

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budget_app.model.Account
import com.example.budget_app.utils.Constants
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
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    
    private var isEditMode = false
    private var accountId: String? = null
    private val TAG = "AddAccountActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_account)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(Constants.DATABASE_URL)

        etAccountName = findViewById(R.id.etAccountName)
        etBalance = findViewById(R.id.etBalance)
        spinnerAccountType = findViewById(R.id.spinnerAccountType)
        btnSaveAccount = findViewById(R.id.btnSaveAccount)
        val ivBack: ImageView = findViewById(R.id.ivBack)

        ivBack.setOnClickListener { finish() }

        val types = arrayOf("Cash", "Bank Account", "Credit Card", "Savings", "Investment", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, types)
        spinnerAccountType.setAdapter(adapter)

        spinnerAccountType.setOnClickListener {
            spinnerAccountType.showDropDown()
        }

        isEditMode = intent.getBooleanExtra("IS_EDIT", false)
        if (isEditMode) {
            accountId = intent.getStringExtra("ACCOUNT_ID")
            etAccountName.setText(intent.getStringExtra("ACCOUNT_NAME"))
            etBalance.setText(intent.getDoubleExtra("ACCOUNT_BALANCE", 0.0).toString())
            spinnerAccountType.setText(intent.getStringExtra("ACCOUNT_TYPE"), false)
            btnSaveAccount.text = getString(R.string.update_account)
        }

        btnSaveAccount.setOnClickListener {
            saveAccount()
        }
    }

    private fun saveAccount() {
        val name = etAccountName.text.toString().trim()
        val balanceStr = etBalance.text.toString().trim()
        val type = spinnerAccountType.text.toString().trim()

        if (name.isEmpty() || balanceStr.isEmpty() || type.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val balance = balanceStr.toDoubleOrNull()
        if (balance == null) {
            Toast.makeText(this, "Invalid balance number", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        
        btnSaveAccount.isEnabled = false
        val accountRef = if (isEditMode && accountId != null) {
            database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_ACCOUNTS).child(accountId!!)
        } else {
            database.getReference(Constants.PATH_USERS).child(userId).child(Constants.PATH_ACCOUNTS).push()
        }

        val account = Account(
            accountId = accountRef.key ?: "",
            name = name,
            balance = balance,
            type = type
        )

        accountRef.setValue(account).addOnCompleteListener { task ->
            btnSaveAccount.isEnabled = true
            if (task.isSuccessful) {
                Toast.makeText(this, "Account Saved!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Log.e(TAG, "Firebase Error: ${task.exception?.message}")
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
