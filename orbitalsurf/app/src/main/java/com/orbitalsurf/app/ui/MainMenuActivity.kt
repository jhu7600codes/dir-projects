package com.orbitalsurf.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.orbitalsurf.app.OrbitalSurfApp
import com.orbitalsurf.app.R
import com.orbitalsurf.app.databinding.ActivityMainMenuBinding
import com.orbitalsurf.core.world.CheckpointSchedule
import kotlinx.coroutines.launch

/** The launcher hub: Play/Shop/Achievements/Dailies, plus a glance at best score, plates balance, and any owned Headstarts. */
class MainMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playButton.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
        binding.shopButton.setOnClickListener {
            startActivity(Intent(this, ShopActivity::class.java))
        }
        binding.achievementsButton.setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }
        binding.dailyChallengesButton.setOnClickListener {
            startActivity(Intent(this, DailyChallengesActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Refreshed on every resume (not just onCreate) so returning from a run, the shop, or
        // an achievement unlock always shows current numbers.
        refreshHeader()
    }

    private fun refreshHeader() {
        val repository = (application as OrbitalSurfApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            binding.bestScoreText.text = getString(R.string.menu_best_score_format, save.bestScore)
            binding.platesText.text = getString(R.string.menu_plates_format, save.wallet.plates)
            populateHeadstarts(save.inventory.headstartTickets)
        }
    }

    private fun populateHeadstarts(headstartTickets: Map<Int, Int>) {
        binding.headstartContainer.removeAllViews()
        val owned = headstartTickets.filterValues { it > 0 }.toSortedMap()
        if (owned.isEmpty()) {
            binding.headstartContainer.visibility = View.GONE
            return
        }
        binding.headstartContainer.visibility = View.VISIBLE
        for ((checkpointIndex, count) in owned) {
            val button = Button(this).apply {
                text = getString(R.string.shop_headstart_item_format, checkpointIndex) + " x$count"
                textSize = 12f
                setOnClickListener { startRunWithHeadstart(checkpointIndex) }
            }
            binding.headstartContainer.addView(button)
        }
    }

    private fun startRunWithHeadstart(checkpointIndex: Int) {
        val repository = (application as OrbitalSurfApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            val currentCount = save.inventory.headstartTickets[checkpointIndex] ?: 0
            if (currentCount <= 0) return@launch

            val newTickets = save.inventory.headstartTickets.toMutableMap()
            if (currentCount - 1 <= 0) newTickets.remove(checkpointIndex) else newTickets[checkpointIndex] = currentCount - 1
            repository.save(save.copy(inventory = save.inventory.copy(headstartTickets = newTickets)))

            val startDistance = CheckpointSchedule.checkpointDistance(checkpointIndex)
            val intent = Intent(this@MainMenuActivity, GameActivity::class.java)
                .putExtra(GameActivity.EXTRA_START_DISTANCE, startDistance)
            startActivity(intent)
        }
    }
}
