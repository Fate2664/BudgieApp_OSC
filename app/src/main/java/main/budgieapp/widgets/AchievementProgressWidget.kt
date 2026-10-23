package main.budgieapp.widgets

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CancellationException
import main.budgieapp.AchievementsActivity
import main.budgieapp.R
import main.budgieapp.data.AchievementRepo

object AchievementProgressWidget {
    fun bindNavigation(root: View) {
        root.isClickable = true
        root.isFocusable = true
        root.setOnClickListener {
            root.context.startActivity(Intent(root.context, AchievementsActivity::class.java))
        }
    }

    fun showEarned(root: View, earned: Set<String>) {
        root.findViewById<TextView>(R.id.txtAchievementsEarned).text =
            root.resources.getQuantityString(
                R.plurals.achievements_earned_count,
                earned.size,
                earned.size
            )
    }

    suspend fun refresh(root: View, onEarnedLoaded: (Set<String>) -> Unit = {}) {
        val count = root.findViewById<TextView>(R.id.txtAchievementsEarned)
        val repo = AchievementRepo.forCurrentUser()
        val cached = repo.getCachedEarned()

        if (cached != null){
            showEarned(root, cached)
        } else {
            count.setText(R.string.achievements_loading)
        }

        try {
            val earned = repo.refreshAndGetEarned()
            showEarned(root, earned)
            onEarnedLoaded(earned)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(
                "AchievementProgressWidget",
                "Could not load achievements",
                error
            )

            if (cached == null) {
                count.setText(R.string.achievements_widget_error)
            }
        }
    }
}
