package com.example.budget_app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.R
import com.example.budget_app.model.LevelData
import com.google.android.material.card.MaterialCardView

class LevelAdapter(
    private val levels: List<LevelData>,
    private val currentLevel: Int
) : RecyclerView.Adapter<LevelAdapter.LevelViewHolder>() {

    class LevelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardLevel: MaterialCardView = view.findViewById(R.id.cardLevel)
        val tvLevelNumber: TextView = view.findViewById(R.id.tvLevelNumber)
        val tvLevelTitle: TextView = view.findViewById(R.id.tvLevelTitle)
        val tvLevelXp: TextView = view.findViewById(R.id.tvLevelXp)
        val ivLock: ImageView = view.findViewById(R.id.ivLock)
        val tvCurrentLabel: TextView = view.findViewById(R.id.tvCurrentLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_level, parent, false)
        return LevelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        val levelData = levels[position]
        val isUnlocked = levelData.level <= currentLevel
        val isCurrent = levelData.level == currentLevel

        holder.tvLevelNumber.text = levelData.level.toString()
        holder.tvLevelTitle.text = levelData.title
        holder.tvLevelXp.text = if (levelData.level == 1) "Starting Level" else "${levelData.xpRequired} XP to reach"

        if (isUnlocked) {
            holder.ivLock.visibility = View.GONE
            holder.cardLevel.alpha = 1.0f
            holder.tvLevelNumber.backgroundTintList = ContextCompat.getColorStateList(holder.itemView.context, R.color.primary_blue)
        } else {
            holder.ivLock.visibility = View.VISIBLE
            holder.cardLevel.alpha = 0.5f
            holder.tvLevelNumber.backgroundTintList = ContextCompat.getColorStateList(holder.itemView.context, android.R.color.darker_gray)
        }

        if (isCurrent) {
            holder.tvCurrentLabel.visibility = View.VISIBLE
            holder.cardLevel.strokeWidth = 4
            holder.cardLevel.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.primary_blue)
        } else {
            holder.tvCurrentLabel.visibility = View.GONE
            holder.cardLevel.strokeWidth = 1
            holder.cardLevel.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.primary_blue) // Default or variant
        }
    }

    override fun getItemCount() = levels.size
}
