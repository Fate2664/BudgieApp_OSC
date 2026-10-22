package main.budgieapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import main.budgieapp.data.AchievementRepo
import kotlin.coroutines.cancellation.CancellationException

class AchievementsActivity : AppCompatActivity() {
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_achievements)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.achievementsGrid).visibility = View.VISIBLE
        findViewById<TextView>(R.id.txtAchievementsCount).setText(R.string.achievements_loading)
    }

    override fun onStart() {
        super.onStart()

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        refreshAchievements()
    }

    override fun onStop() {
        refreshJob?.cancel()
        super.onStop()
    }

    private fun refreshAchievements() {
        refreshJob?.cancel()

        val count = findViewById<TextView>(R.id.txtAchievementsCount)
        val grid = findViewById<View>(R.id.achievementsGrid)

        count.setText(R.string.achievements_loading)
        grid.visibility = View.INVISIBLE

        refreshJob = lifecycleScope.launch {
            try {
                val earned = AchievementRepo.forCurrentUser().refreshAndGetEarned()
                renderAchievements(earned)

                count.text = getString(
                    R.string.achievements_count,
                    earned.size,
                    AchievementRepo.ALL_IDS.size
                )

                grid.visibility = View.VISIBLE
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(
                    "AchievementsActivity",
                    "Could not load achievements",
                    error
                )
                count.setText(R.string.achievements_load_error)
                grid.visibility = View.GONE
            }
        }
    }

    private fun renderAchievements(earned: Set<String>) {
        val cards = listOf(
            AchievementCardUi(
                AchievementRepo.FIRST_STEP,
                R.id.cardFirstStep, R.id.imgFirstStep,
                R.id.statusFirstStep, R.id.iconFirstStepStatus,
                R.id.txtFirstStepStatus,
                "#BFE8D5", "#087F68"
            ),
            AchievementCardUi(
                AchievementRepo.HALFWAY_THERE,
                R.id.cardHalfwayThere, R.id.imgHalfwayThere,
                R.id.statusHalfwayThere, R.id.iconHalfwayThereStatus,
                R.id.txtHalfwayThereStatus,
                "#FFE49C", "#BD8500"
            ),
            AchievementCardUi(
                AchievementRepo.GOAL_ACHIEVED,
                R.id.cardGoalAchieved, R.id.imgGoalAchieved,
                R.id.statusGoalAchieved, R.id.iconGoalAchievedStatus,
                R.id.txtGoalAchievedStatus,
                "#E7D2FA", "#7A28B9"
            ),
            AchievementCardUi(
                AchievementRepo.CONSISTENT_SAVER,
                R.id.cardConsistentSaver, R.id.imgConsistentSaver,
                R.id.statusConsistentSaver, R.id.iconConsistentSaverStatus,
                R.id.txtConsistentSaverStatus,
                "#E7D2FA", "#7A28B9"
            ),
            AchievementCardUi(
                AchievementRepo.WITHIN_BUDGET,
                R.id.cardWithinBudget, R.id.imgWithinBudget,
                R.id.statusWithinBudget, R.id.iconWithinBudgetStatus,
                R.id.txtWithinBudgetStatus,
                "#D5E6FA", "#387CB5"
            ),
            AchievementCardUi(
                AchievementRepo.GOAL_EXPLORER,
                R.id.cardGoalExplorer, R.id.imgGoalExplorer,
                R.id.statusGoalExplorer, R.id.iconGoalExplorerStatus,
                R.id.txtGoalExplorerStatus,
                "#FFE49C", "#BD8500"
            )
        )

        for (item in cards) {
            val isEarned = item.achievementId in earned
            val card = findViewById<MaterialCardView>(item.cardId)
            val badge = findViewById<ImageView>(item.badgeId)
            val status = findViewById<View>(item.statusId)
            val statusIcon = findViewById<ImageView>(item.statusIconId)
            val statusText = findViewById<TextView>(item.statusTextId)

            card.setCardBackgroundColor(
                Color.parseColor(
                    if (isEarned) "#FFFEFB" else "#FAFAF8"
                )
            )

            badge.backgroundTintList = ColorStateList.valueOf(
                Color.parseColor(
                    if (isEarned) item.badgeColor else "#E6E6E8"
                )
            )

            ImageViewCompat.setImageTintList(
                badge,
                ColorStateList.valueOf(
                    Color.parseColor(
                        if (isEarned) item.iconColor else "#7D7D87"
                    )
                )
            )

            status.backgroundTintList = ColorStateList.valueOf(
                Color.parseColor(
                    if (isEarned) "#EAF5E9" else "#EFEFF0"
                )
            )

            statusIcon.setImageResource(
                if (isEarned) {
                    R.drawable.ic_achievement_check_circle
                } else {
                    R.drawable.ic_achievement_lock
                }
            )

            ImageViewCompat.setImageTintList(
                statusIcon,
                ColorStateList.valueOf(
                    getColor(
                        if (isEarned) {
                            R.color.achievement_earned
                        } else {
                            R.color.achievement_locked
                        }
                    )
                )
            )

            statusText.setText(
                if (isEarned) {
                    R.string.achievement_earned
                } else {
                    R.string.achievement_locked
                }
            )

            statusText.setTextColor(
                getColor(
                    if (isEarned) {
                        R.color.achievement_earned
                    } else {
                        R.color.achievement_secondary_text
                    }
                )
            )
        }
    }

    private data class AchievementCardUi(
        val achievementId: String,
        val cardId: Int,
        val badgeId: Int,
        val statusId: Int,
        val statusIconId: Int,
        val statusTextId: Int,
        val badgeColor: String,
        val iconColor: String
    )
}