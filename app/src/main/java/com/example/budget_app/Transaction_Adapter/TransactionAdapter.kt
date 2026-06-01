package com.example.budget_app.Transaction_Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.R
import com.example.budget_app.model.Transaction
import java.util.Locale

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
        val context = holder.itemView.context

        holder.tvTitle.text = transaction.transaction_name
        holder.tvCategory.text = transaction.category.category_name
        
        val amount = transaction.transaction_amount
        val amountStr = String.format(Locale.US, "%.2f", Math.abs(amount))
        
        if (amount >= 0) {
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.success_green))
            holder.tvAmount.text = context.getString(R.string.income_prefix, amountStr)
        } else {
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.red))
            holder.tvAmount.text = context.getString(R.string.expense_prefix, amountStr)
        }

        holder.tvTimeStamp.text = transaction.transaction_date
    }

    override fun getItemCount(): Int = transactions.size
}