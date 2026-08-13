package com.vaultgame.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vaultgame.app.R
import com.vaultgame.app.VaultApp
import com.vaultgame.app.databinding.ActivityMainMenuBinding
import com.vaultgame.core.progression.DailyLoginSystem
import com.vaultgame.core.save.GameSave
import kotlinx.coroutines.launch

/** The launcher hub: Play/Skins/Shop/Daily Challenge/Achievements/Leaderboard, plus a glance at
 * best score, plates balance, any owned headstarts, and the daily login streak bonus. */
class MainMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playButton.setOnClickListener { startActivity(Intent(this, GameActivity::class.java)) }
        binding.skinsButton.setOnClickListener { startActivity(Intent(this, SkinSelectActivity::class.java)) }
        binding.shopButton.setOnClickListener { startActivity(Intent(this, ShopActivity::class.java)) }
        binding.missionsButton.setOnClickListener { startActivity(Intent(this, MissionsActivity::class.java)) }
        binding.dailyChallengeButton.setOnClickListener { startActivity(Intent(this, DailyChallengeActivity::class.java)) }
        binding.achievementsButton.setOnClickListener { startActivity(Intent(this, AchievementsActivity::class.java)) }
        binding.leaderboardButton.setOnClickListener { startActivity(Intent(this, LeaderboardActivity::class.java)) }
        binding.loginStreakButton.setOnClickListener { binding.loginStreakButton.visibility = View.GONE }
    }

    override fun onResume() {
        super.onResume()
        // Refreshed on every resume (not just onCreate) so returning from a run, the shop, or
        // an achievement unlock always shows current numbers.
        refreshHeader()
    }

    private fun refreshHeader() {
        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            var save = repository.load()

            val loginResult = DailyLoginSystem.claim(save.dailyLogin, System.currentTimeMillis())
            if (!loginResult.alreadyClaimedToday) {
                save = save.copy(dailyLogin = loginResult.newState, wallet = save.wallet.credit(loginResult.platesAwarded.toLong()))
                repository.save(save)
                binding.loginStreakButton.visibility = View.VISIBLE
                binding.loginStreakButton.text =
                    getString(R.string.menu_login_streak_format, loginResult.streakDay, loginResult.platesAwarded)
            } else {
                binding.loginStreakButton.visibility = View.GONE
            }

            binding.bestScoreText.text = getString(R.string.menu_best_score_format, save.playerStats.bestScore.toInt())
            binding.platesText.text = getString(R.string.menu_plates_format, save.wallet.plates.toInt())
            populateHeadstarts(save)
        }
    }

    private fun populateHeadstarts(save: GameSave) {
        binding.headstartContainer.removeAllViews()
        val checkpoints = save.inventory.unlockedHeadstarts.sorted()
        if (checkpoints.isEmpty()) {
            binding.headstartContainer.visibility = View.GONE
            return
        }
        binding.headstartContainer.visibility = View.VISIBLE
        for (distance in checkpoints) {
            val button = Button(this).apply {
                text = getString(R.string.menu_headstart_format, distance.toInt())
                textSize = 12f
                setOnClickListener { startRunWithHeadstart(distance) }
            }
            binding.headstartContainer.addView(button)
        }
    }

    private fun startRunWithHeadstart(distanceMeters: Double) {
        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            repository.save(save.copy(pendingHeadstartDistance = distanceMeters))
            startActivity(Intent(this@MainMenuActivity, GameActivity::class.java))
        }
    }
}
