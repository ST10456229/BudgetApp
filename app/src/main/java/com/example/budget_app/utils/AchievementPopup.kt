package com.example.budget_app.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.example.budget_app.R
import com.example.budget_app.model.Achievement

class AchievementPopup(private val context: Context) {

    fun show(achievement: Achievement) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.popup_achievement)
        
        dialog.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            it.setGravity(Gravity.TOP)
            it.setDimAmount(0.1f)
        }

        val tvTitle: TextView = dialog.findViewById(R.id.tvBadgeName)
        val tvDesc: TextView = dialog.findViewById(R.id.tvBadgeDesc)
        val tvXP: TextView = dialog.findViewById(R.id.tvXPReward)
        val tvEmoji: TextView = dialog.findViewById(R.id.tvBadgeEmoji)

        tvTitle.text = achievement.name
        tvDesc.text = achievement.description
        tvXP.text = "+${achievement.xpReward} XP"
        tvEmoji.text = achievement.iconEmoji

        dialog.show()

        // Auto dismiss after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }, 3000)
    }
}
