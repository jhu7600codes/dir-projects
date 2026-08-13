package com.vaultgame.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vaultgame.app.R
import com.vaultgame.app.VaultApp
import com.vaultgame.app.databinding.ActivityDailyChallengeBinding
import com.vaultgame.core.progression.DailyChallengeSystem
import kotlinx.coroutines.launch

/** Today's single, harder-than-a-mission goal. Resets every UTC day (see
 * [DailyChallengeSystem]) and pays out a bigger plates reward than a mission set. */
class DailyChallengeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDailyChallengeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyChallengeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        refresh()
    }

    private fun refresh() {
        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            val now = System.currentTimeMillis()
            val challenge = DailyChallengeSystem.ensureCurrent(save.dailyChallenge, now, save.worldSeed)
            if (save.dailyChallenge != challenge) repository.save(save.copy(dailyChallenge = challenge))

            binding.descriptionText.text = challenge.description
            binding.progressText.text =
                getString(R.string.mission_progress_format, challenge.progress.coerceAtMost(challenge.targetValue), challenge.targetValue)
            binding.rewardText.text = getString(R.string.daily_reward_format, challenge.rewardPlates)

            binding.claimButton.isEnabled = challenge.isComplete && !challenge.claimed
            binding.claimButton.text = if (challenge.claimed) getString(R.string.daily_claimed) else getString(R.string.daily_claim)
            binding.claimButton.setOnClickListener { claim() }
        }
    }

    private fun claim() {
        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            val challenge = save.dailyChallenge ?: return@launch
            if (!challenge.isComplete || challenge.claimed) return@launch

            val updated = save.copy(
                dailyChallenge = challenge.copy(claimed = true),
                wallet = save.wallet.credit(challenge.rewardPlates.toLong()),
            )
            repository.save(updated)
            refresh()
        }
    }
}
