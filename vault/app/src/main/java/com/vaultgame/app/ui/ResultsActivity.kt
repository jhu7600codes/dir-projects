package com.vaultgame.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vaultgame.app.R
import com.vaultgame.app.databinding.ActivityResultsBinding

/** Run-end summary: score, coins/distance/plates earned, and any achievements unlocked this run. */
class ResultsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val score = intent.getLongExtra(EXTRA_SCORE, 0L)
        val bestScore = intent.getLongExtra(EXTRA_BEST_SCORE, 0L)
        val coins = intent.getIntExtra(EXTRA_COINS, 0)
        val distance = intent.getDoubleExtra(EXTRA_DISTANCE, 0.0)
        val platesEarned = intent.getLongExtra(EXTRA_PLATES_EARNED, 0L)
        val newAchievements = intent.getStringArrayExtra(EXTRA_NEW_ACHIEVEMENTS).orEmpty()

        binding.scoreText.text = getString(R.string.results_score_format, score.toInt())
        binding.bestScoreText.text = getString(R.string.results_best_score_format, bestScore.toInt())
        binding.distanceText.text = getString(R.string.results_distance_format, distance.toInt())
        binding.coinsText.text = getString(R.string.results_coins_format, coins)
        binding.platesEarnedText.text = getString(R.string.results_plates_earned_format, platesEarned.toInt())
        binding.newBestText.visibility = if (score >= bestScore && score > 0) View.VISIBLE else View.GONE

        if (newAchievements.isNotEmpty()) {
            binding.newAchievementsTitle.visibility = View.VISIBLE
            binding.newAchievementsList.visibility = View.VISIBLE
            binding.newAchievementsList.text = newAchievements.joinToString("\n")
        } else {
            binding.newAchievementsTitle.visibility = View.GONE
            binding.newAchievementsList.visibility = View.GONE
        }

        binding.playAgainButton.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
            finish()
        }
        binding.mainMenuButton.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }
    }

    companion object {
        const val EXTRA_SCORE = "score"
        const val EXTRA_BEST_SCORE = "best_score"
        const val EXTRA_COINS = "coins"
        const val EXTRA_DISTANCE = "distance"
        const val EXTRA_PLATES_EARNED = "plates_earned"
        const val EXTRA_NEW_ACHIEVEMENTS = "new_achievements"
    }
}
