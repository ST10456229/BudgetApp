package com.example.budget_app

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream

class Addexpense : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var btnUpdateProfile: Button
    private lateinit var switchDarkMode: SwitchMaterial
    private lateinit var btnLogout: Button
    private lateinit var ivDarkIcon: ImageView
    private lateinit var ivProfileLarge: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    
    private val TAG = "ProfileSettings"
    private val DB_URL = "https://budgetapp2-6ab44-default-rtdb.europe-west1.firebasedatabase.app"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageSelection(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle theme preference BEFORE setting content view
        val sharedPrefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        if (sharedPrefs.contains("DarkMode")) {
            val isDarkMode = sharedPrefs.getBoolean("DarkMode", false)
            val targetMode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                AppCompatDelegate.setDefaultNightMode(targetMode)
            }
        }

        setContentView(R.layout.activity_proflesettings)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(DB_URL)

        // Bind Views
        etUsername = findViewById(R.id.etUsername)
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        btnLogout = findViewById(R.id.btnLogout)
        ivDarkIcon = findViewById(R.id.ivDarkIcon)
        ivProfileLarge = findViewById(R.id.ivProfileLarge)
        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val cvProfileImage = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cvProfileImage)
        
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        val fabAdd: FloatingActionButton = findViewById(R.id.fabAdd)

        fetchUserData()

        // Back Button
        ivBack.setOnClickListener {
            finish()
        }

        // Change Profile Picture
        cvProfileImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Save Username/Changes
        btnUpdateProfile.setOnClickListener {
            val name = etUsername.text.toString().trim()
            if (name.isNotEmpty()) {
                updateUsername(name)
            } else {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
            }
        }

        // Dark Mode Toggle
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        switchDarkMode.isChecked = sharedPrefs.getBoolean("DarkMode", isNightMode)
        updateThemeIcon(switchDarkMode.isChecked)
        
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("DarkMode", isChecked).apply()
            updateThemeIcon(isChecked)
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        // Logout
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, activity_login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Bottom Navigation Logic
        bottomNavigation.selectedItemId = R.id.nav_more
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, TransactionHistoryActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_more -> true
                else -> false
            }
        }

        fabAdd.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            val userId = auth.currentUser?.uid ?: "default"
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "profile_pic_$userId.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            val internalPath = Uri.fromFile(file).toString()
            updateProfilePicture(internalPath)
            
            // Update UI immediately using Bitmap to avoid URI caching issues
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ivProfileLarge.setImageBitmap(bitmap)
            ivProfileLarge.colorFilter = null
            Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image: ${e.message}")
            Toast.makeText(this, "Failed to save icon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateThemeIcon(isDark: Boolean) {
        ivDarkIcon.setImageResource(if (isDark) R.drawable.ic_moon else R.drawable.ic_sun)
    }

    private fun fetchUserData() {
        val user = auth.currentUser ?: return
        val userRef = database.getReference("users").child(user.uid).child("profile")
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing) return
                val username = snapshot.child("username").getValue(String::class.java)
                val profilePic = snapshot.child("profilePic").getValue(String::class.java)
                
                if (!username.isNullOrEmpty()) {
                    etUsername.setText(username)
                } else {
                    etUsername.setText(user.displayName ?: "")
                }
                
                if (!profilePic.isNullOrEmpty()) {
                    try {
                        val path = Uri.parse(profilePic).path
                        if (path != null && File(path).exists()) {
                            val bitmap = BitmapFactory.decodeFile(path)
                            ivProfileLarge.setImageBitmap(bitmap)
                            ivProfileLarge.colorFilter = null
                        } else {
                            ivProfileLarge.setImageResource(R.drawable.ic_profile)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading profile pic")
                        ivProfileLarge.setImageResource(R.drawable.ic_profile)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateUsername(username: String) {
        val user = auth.currentUser ?: return
        val userId = user.uid
        
        // Update Firebase Auth profile
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .build()
        user.updateProfile(profileUpdates)

        // Update Realtime Database
        database.getReference("users").child(userId).child("profile").child("username").setValue(username)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun updateProfilePicture(uriString: String) {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("profile").child("profilePic").setValue(uriString)
    }
}