package com.orbitalsurf.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.orbitalsurf.app.R
import com.orbitalsurf.app.databinding.ActivityResultsBinding

/** Shown after [GameActivity] finishes a run -- score, best score, plates earned, Play Again / Menu. */
class ResultsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val score = intent.getLongExtra(EXTRA_SCORE, 0L)
        val bestScore = intent.getLongExtra(EXTRA_BEST_SCORE, 0L)
        val platesEarned = intent.getLongExtra(EXTRA_PLATES_EARNED, 0L)
        val isNewBest = intent.getBooleanExtra(EXTRA_NEW_BEST, false)

        binding.scoreText.text = getString(R.string.results_score_format, score)
        binding.bestScoreText.text = getString(R.string.results_best_score_format, bestScore)
        binding.platesEarnedText.text = getString(R.string.results_plates_earned_format, platesEarned)
        binding.newBestText.visibility = if (isNewBest) View.VISIBLE else View.GONE

        binding.playAgainButton.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
            finish()
        }
        binding.menuButton.setOnClickListener {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }

        // Back from the results screen should return to the menu, not re-show the finished run.
        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@ResultsActivity, MainMenuActivity::class.java))
            finish()
        }
    }

    companion object {
        const val EXTRA_SCORE = "score"
        const val EXTRA_BEST_SCORE = "best_score"
        const val EXTRA_PLATES_EARNED = "plates_earned"
        const val EXTRA_NEW_BEST = "new_best"
    }
}
