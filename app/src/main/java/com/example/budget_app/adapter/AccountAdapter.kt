package com.example.budget_app.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.R
import com.example.budget_app.model.Account
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class AccountAdapter(
    private var accounts: List<Account>,
    private val onAccountClick: (Account) -> Unit
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    inner class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvAccountName)
        val tvType: TextView = view.findViewById(R.id.tvAccountType)
        val tvBalance: TextView = view.findViewById(R.id.tvAccountBalance)
        val ivIcon: ImageView = view.findViewById(R.id.ivAccountIcon)
        val cvIcon: MaterialCardView = view.findViewById(R.id.cvAccountIcon)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAccountClick(accounts[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.account_item, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = accounts[position]
        holder.tvName.text = account.name
        holder.tvType.text = account.type
        holder.tvBalance.text = String.format(Locale.US, "R %.2f", account.balance)

        val context = holder.itemView.context
        when (account.type.lowercase()) {
            "cash" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_transactions)
                holder.cvIcon.setCardBackgroundColor(ContextCompat.getColor(context, R.color.success_green))
            }
            "credit card" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_reports) // Use appropriate icon
                holder.cvIcon.setCardBackgroundColor(ContextCompat.getColor(context, R.color.red))
            }
            else -> {
                holder.ivIcon.setImageResource(R.drawable.ic_categories)
                holder.cvIcon.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_blue))
            }
        }
        holder.ivIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
    }

    override fun getItemCount() = accounts.size

    fun updateList(newAccounts: List<Account>) {
        accounts = newAccounts
        notifyDataSetChanged()
    }
}
