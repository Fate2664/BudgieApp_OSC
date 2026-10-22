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

    suspend fun refresh(root: View) {
        val count = root.findViewById<TextView>(R.id.txtAchievementsEarned)

        count.setText(R.string.achievements_loading)

        try {
            val earned = AchievementRepo.forCurrentUser().refreshAndGetEarned()

            count.text = root.resources.getQuantityString(
                R.plurals.achievements_earned_count,
                earned.size,
                earned.size
            )
        } catch (cancelled: CancellationException){
            throw cancelled
        } catch (error: Exception){
            Log.e(
                "AchievementProgressWidget",
                "Could not load achievements",
                error
            )
            count.setText(R.string.achievements_widget_error)
        }
    }
}