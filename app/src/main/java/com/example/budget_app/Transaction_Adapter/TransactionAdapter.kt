package com.example.budget_app.Transaction_Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.R
import com.example.budget_app.model.Transaction

class TransactionAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvTimeStamp: TextView = itemView.findViewById(R.id.tvTimeStamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.tvTitle.text = transaction.transaction_name
        holder.tvCategory.text = transaction.category.category_name
        
        val amount = transaction.transaction_amount
        val isIncome = transaction.transaction_type == "Income"

        if (isIncome) {
            // Income: Plus sign and Green text
            holder.tvAmount.text = "+ ZAR ${String.format("%.2f", Math.abs(amount))}"
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50")) // Green
        } else {
            // Expense: Minus sign and Red text
            holder.tvAmount.text = "- ZAR ${String.format("%.2f", Math.abs(amount))}"
            holder.tvAmount.setTextColor(Color.parseColor("#F44336")) // Red
        }

        holder.tvTimeStamp.text = transaction.transaction_date
    }

    override fun getItemCount(): Int = transactions.size
}
