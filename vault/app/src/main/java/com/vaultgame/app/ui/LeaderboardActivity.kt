package com.vaultgame.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vaultgame.app.R
import com.vaultgame.app.VaultApp
import com.vaultgame.app.databinding.ActivityLeaderboardBinding
import kotlinx.coroutines.launch

/** Local top runs by score -- see [com.vaultgame.core.leaderboard.LeaderboardService]. No
 * backend; entries live in the on-device save only. */
class LeaderboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLeaderboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            if (save.leaderboard.isEmpty()) {
                binding.entriesText.text = getString(R.string.leaderboard_empty)
                return@launch
            }
            binding.entriesText.text = save.leaderboard
                .sortedByDescending { it.score }
                .mapIndexed { index, entry ->
                    getString(
                        R.string.leaderboard_entry_format,
                        index + 1, entry.score.toInt(), entry.distanceMeters.toInt(), entry.coinsCollected,
                    )
                }
                .joinToString("\n")
        }
    }
}
