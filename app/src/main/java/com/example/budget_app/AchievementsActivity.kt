package com.example.budget_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.adapter.AchievementAdapter
import com.example.budget_app.utils.GamificationManager
import com.example.budget_app.utils.NavigationHelper

class AchievementsActivity : AppCompatActivity() {
    private lateinit var adapter: AchievementAdapter
    private lateinit var gamificationManager: GamificationManager
    private lateinit var rvAchievements: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        gamificationManager = GamificationManager.getInstance(this)
        rvAchievements = findViewById(R.id.rvAchievements)
        
        rvAchievements.layoutManager = LinearLayoutManager(this)
        adapter = AchievementAdapter(gamificationManager.getAchievements())
        rvAchievements.adapter = adapter

        NavigationHelper.setupNavigation(this)
    }
}
