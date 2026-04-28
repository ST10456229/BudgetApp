package com.example.budget_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class activity_splash : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash2)

        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User already logged in → go to MainActivity directly
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Setup button listeners for the splash screen UI
        val btnGetStarted: Button = findViewById(R.id.btnGetStarted)
        val btnSignIn: Button = findViewById(R.id.btnSignIn)

        btnGetStarted.setOnClickListener {
            // Navigate to Registration/Sign Up screen
            val intent = Intent(this, activity_register::class.java)
            startActivity(intent)
        }

        btnSignIn.setOnClickListener {
            // Navigate to Login screen
            val intent = Intent(this, activity_login::class.java)
            startActivity(intent)
        }
    }
}