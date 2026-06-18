package com.example.budget_app.adapter

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.R
import com.example.budget_app.model.Achievement

class AchievementAdapter(private var items: List<Achievement>) :
    RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view.findViewById(R.id.tvAchievementEmoji)
        val tvName: TextView = view.findViewById(R.id.tvAchievementName)
        val tvDesc: TextView = view.findViewById(R.id.tvAchievementDesc)
        val ivCheck: ImageView = view.findViewById(R.id.ivCheckmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvEmoji.text = item.iconEmoji
        holder.tvName.text = item.name
        holder.tvDesc.text = item.description

        if (item.achieved) {
            holder.ivCheck.visibility = View.VISIBLE
            holder.itemView.alpha = 1.0f
            holder.tvEmoji.setLayerType(View.LAYER_TYPE_NONE, null)
        } else {
            holder.ivCheck.visibility = View.GONE
            holder.itemView.alpha = 0.5f
            
            // Grayscale filter for locked achievements
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            val filter = ColorMatrixColorFilter(matrix)
            val paint = Paint()
            paint.colorFilter = filter
            holder.tvEmoji.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<Achievement>) {
        items = newList
        notifyDataSetChanged()
    }
}
