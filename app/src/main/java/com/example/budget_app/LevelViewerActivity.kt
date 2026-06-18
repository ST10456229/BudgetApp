package com.example.budget_app

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.adapter.LevelAdapter
import com.example.budget_app.model.LevelConfig
import com.example.budget_app.utils.GamificationManager

class LevelViewerActivity : AppCompatActivity() {
    private lateinit var gamificationManager: GamificationManager
    private lateinit var rvLevels: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_viewer)

        gamificationManager = GamificationManager.getInstance(this)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            finish()
        }

        rvLevels = findViewById(R.id.rvLevels)
        rvLevels.layoutManager = LinearLayoutManager(this)

        val stats = gamificationManager.getStats()
        // Accessing the levels property directly from LevelConfig object
        val adapter = LevelAdapter(LevelConfig.levels, stats.level)
        rvLevels.adapter = adapter
    }
}
