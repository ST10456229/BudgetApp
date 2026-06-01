package com.example.budget_app.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.GoalDetailActivity
import com.example.budget_app.R
import com.example.budget_app.model.Goal
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Locale

class GoalAdapter(private val goals: List<Goal>) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivGoalImage: ImageView = view.findViewById(R.id.ivGoalImage)
        val tvGoalName: TextView = view.findViewById(R.id.tvGoalName)
        val tvGoalDate: TextView = view.findViewById(R.id.tvGoalDate)
        val progressGoal: LinearProgressIndicator = view.findViewById(R.id.progressGoal)
        val tvCurrentAmount: TextView = view.findViewById(R.id.tvCurrentAmount)
        val tvTargetAmount: TextView = view.findViewById(R.id.tvTargetAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.goal_item, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        holder.tvGoalName.text = goal.name
        holder.tvGoalDate.text = "By ${goal.targetDate}"
        
        if (goal.imageUrl.isNotEmpty()) {
            try {
                holder.ivGoalImage.setImageURI(null)
                holder.ivGoalImage.setImageURI(Uri.parse(goal.imageUrl))
            } catch (e: Exception) {
                holder.ivGoalImage.setImageResource(R.drawable.ic_goals)
            }
        } else {
            holder.ivGoalImage.setImageResource(R.drawable.ic_goals)
        }
        
        val progress = if (goal.targetAmount > 0) {
            ((goal.currentAmount / goal.targetAmount) * 100).toInt()
        } else 0
        
        holder.progressGoal.progress = progress.coerceAtMost(100)
        holder.tvCurrentAmount.text = String.format(Locale.US, "R %.2f", goal.currentAmount)
        holder.tvTargetAmount.text = String.format(Locale.US, "of R %.2f", goal.targetAmount)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, GoalDetailActivity::class.java)
            intent.putExtra("GOAL_ID", goal.goalId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = goals.size
}
