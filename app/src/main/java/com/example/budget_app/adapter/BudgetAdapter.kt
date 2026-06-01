package com.example.budget_app.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.R
import com.example.budget_app.model.Budget
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Locale

class BudgetAdapter(
    private val budgetList: List<Budget>,
    private val spentMap: Map<String, Double>
) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    class BudgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBudgetName: TextView = view.findViewById(R.id.tvBudgetName)
        val tvBudgetPeriod: TextView = view.findViewById(R.id.tvBudgetPeriod)
        val tvBudgetStatus: TextView = view.findViewById(R.id.tvBudgetStatus)
        val budgetProgress: LinearProgressIndicator = view.findViewById(R.id.budgetProgress)
        val tvRemainingLabel: TextView = view.findViewById(R.id.tvRemainingLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.budget_tracker_item, parent, false)
        return BudgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val budget = budgetList[position]
        // Use budgetId as the primary key, fallback to name for older entries
        val spent = spentMap[budget.budgetId] ?: spentMap[budget.name] ?: 0.0
        val remaining = budget.targetAmount - spent
        val progress = if (budget.targetAmount > 0) ((spent / budget.targetAmount) * 100).toInt() else 0

        holder.tvBudgetName.text = budget.name
        holder.tvBudgetPeriod.text = budget.period
        holder.tvBudgetStatus.text = String.format(Locale.US, "R %.2f / R %.2f", spent, budget.targetAmount)
        
        holder.budgetProgress.progress = progress.coerceAtMost(100)
        
        if (progress >= 100) {
            holder.budgetProgress.setIndicatorColor(Color.RED)
            holder.tvRemainingLabel.text = String.format(Locale.US, "Over budget by R %.2f", spent - budget.targetAmount)
            holder.tvRemainingLabel.setTextColor(Color.RED)
        } else {
            holder.budgetProgress.setIndicatorColor(Color.parseColor("#0056D2"))
            holder.tvRemainingLabel.text = String.format(Locale.US, "R %.2f Left", remaining)
            holder.tvRemainingLabel.setTextColor(Color.parseColor("#0056D2"))
        }
    }

    override fun getItemCount() = budgetList.size
}
