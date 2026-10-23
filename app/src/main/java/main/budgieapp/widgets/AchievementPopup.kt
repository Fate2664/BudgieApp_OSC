package main.budgieapp.widgets

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.delay
import main.budgieapp.R
import main.budgieapp.data.AchievementRepo

class AchievementPopup(private val root: ConstraintLayout) {
    private val context = root.context
    private var activeCard: View? = null

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
    suspend fun show(uid: String, earned: Set<String>) {
        val repo = AchievementRepo.forCurrentUser()

        for (id in main.budgieapp.data.AchievementRepo.ALL_IDS) {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != uid) return
            if (id !in earned) continue

            val (title, description) = when (id) {
                "first_step" -> R.string.achievement_first_step to R.string.achievement_first_step_description
                "halfway_there" ->
                    R.string.achievement_halfway to R.string.achievement_halfway_description

                "goal_achieved" ->
                    R.string.achievement_goal_achieved to R.string.achievement_goal_achieved_description

                "consistent_saver" ->
                    R.string.achievement_consistent_saver to R.string.achievement_consistent_saver_description

                "within_budget" ->
                    R.string.achievement_within_budget to R.string.achievement_within_budget_description

                else -> R.string.achievement_goal_explorer to R.string.achievement_goal_explorer_description
            }

            if (!repo.isNotificationPending(id)) continue
            showCard(
                title = context.getString(title),
                description = context.getString(description)
            )
            repo.markNotificationShown(id)

        }

    }

    private suspend fun showCard(
        title: String,
        description: String
    ) {
        val card = MaterialCardView(context).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(8).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.rgb(153, 222, 209)
            strokeWidth = dp(1)
            alpha = 0f
            layoutParams =
                ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    setMargins(dp(12), dp(8), dp(12), 0)
                }
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(6), dp(12))
        }

        val trophy = ImageView(context).apply {
            setImageResource(R.drawable.ic_achievement_trophy)
            setColorFilter(Color.rgb(189, 133, 0))
            setBackgroundResource(R.drawable.achievement_badge_circle)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.rgb(255, 228, 156))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        content.addView(trophy, LinearLayout.LayoutParams(dp(54), dp(54)))

        val labels = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun label(text: String, size: Float, color: Int, bold: Boolean) =
            TextView(context).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                if (bold) {
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
            }

        labels.addView(
            label(
                context.getString(R.string.achievement_popup_unlocked),
                12f,
                Color.rgb(8, 127, 104),
                false
            )
        )

        labels.addView(
            label(title, 17f, Color.rgb(23, 24, 49), true)
        )
        labels.addView(
            label(description, 13f, Color.rgb(85, 85, 99), false)
        )

        content.addView(
            labels,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
            }
        )

        var dismissed = false
        val close = TextView(context).apply {
            text = "\u00D7"
            textSize = 26f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.rgb(85, 85, 99))
            contentDescription = context.getString(R.string.achievement_popup_dismiss)
            isClickable = true
            isFocusable = true
            setOnClickListener { dismissed = true }
        }

        content.addView(close, LinearLayout.LayoutParams(dp(48), dp(48)))
        card.addView(content)
        activeCard = card
        root.addView(card)

        try {
            card.doOnLayout {
                if (activeCard === card) {
                    card.translationY = -card.height.toFloat() - dp(16)
                    card.animate().translationY(0f).alpha(1f).setDuration(250).start()
                    card.announceForAccessibility(
                        "${context.getString(R.string.achievement_popup_unlocked)}. $title. $description"
                    )
                }
            }
            repeat(40) {
                if (!dismissed) delay(100)
            }

            card.animate().translationY(-card.height.toFloat() - dp(16))
                .alpha(0f)
                .setDuration(200)
                .start()

            delay(200)
        } finally {
            card.animate().cancel()
            root.removeView(card)
            if (activeCard === card) activeCard = null
        }
    }
}
