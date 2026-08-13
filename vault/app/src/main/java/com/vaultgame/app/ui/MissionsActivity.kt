package com.vaultgame.app.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vaultgame.app.R
import com.vaultgame.app.VaultApp
import com.vaultgame.app.databinding.ActivityMissionsBinding
import com.vaultgame.core.math.SeededRandom
import com.vaultgame.core.progression.MissionSystem
import kotlinx.coroutines.launch

/** Shows the active rolling set of 3 mission goals and lets the player spend a mission-skip
 * voucher (bought in the shop) to instantly complete the set. */
class MissionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMissionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        refresh()
    }

    private fun refresh() {
        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            val missionState = save.missionState

            binding.rewardText.text = getString(R.string.missions_reward_format, missionState.currentSet.rewardMultiplier)
            binding.missionsContainer.removeAllViews()
            for (mission in missionState.currentSet.missions) {
                binding.missionsContainer.addView(buildMissionRow(mission.description, mission.progress, mission.targetValue))
            }

            if (missionState.pendingScoreMultiplier > 1.0) {
                binding.pendingMultiplierText.visibility = View.VISIBLE
                binding.pendingMultiplierText.text =
                    getString(R.string.missions_pending_multiplier_format, missionState.pendingScoreMultiplier)
            } else {
                binding.pendingMultiplierText.visibility = View.GONE
            }

            val vouchers = save.inventory.missionSkipVouchers
            binding.skipButton.isEnabled = vouchers > 0
            binding.skipButton.text =
                if (vouchers > 0) getString(R.string.missions_skip_format, vouchers) else getString(R.string.missions_skip_none)
            binding.skipButton.setOnClickListener { skipMissionSet() }
        }
    }

    private fun buildMissionRow(description: String, progress: Int, target: Int): View {
        val context = this
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 12)
        }
        val descriptionView = TextView(context).apply {
            text = description
            setTextColor(getColor(R.color.vault_on_surface))
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val progressView = TextView(context).apply {
            text = getString(R.string.mission_progress_format, progress.coerceAtMost(target), target)
            setTextColor(getColor(if (progress >= target) R.color.vault_success else R.color.vault_on_surface_muted))
            textSize = 15f
        }
        row.addView(descriptionView)
        row.addView(progressView)
        return row
    }

    private fun skipMissionSet() {
        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            if (save.inventory.missionSkipVouchers <= 0) return@launch

            val newInventory = com.vaultgame.core.economy.ShopService.redeemMissionSkipVoucher(save.inventory) ?: return@launch
            val rng = SeededRandom(save.worldSeed xor System.currentTimeMillis())
            val newMissionState = MissionSystem.skip(save.missionState, rng)

            repository.save(save.copy(inventory = newInventory, missionState = newMissionState))
            refresh()
        }
    }
}
