package com.vaultgame.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaultgame.app.VaultApp
import com.vaultgame.app.databinding.ActivityAchievementsBinding
import com.vaultgame.core.progression.AchievementCatalog
import kotlinx.coroutines.launch

/** Distance milestones, coin milestones, and no-hit run streaks -- all lifetime totals, some
 * unlocking a cosmetic skin (see [AchievementCatalog]). */
class AchievementsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAchievementsBinding
    private val adapter = AchievementsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.entriesList.layoutManager = LinearLayoutManager(this)
        binding.entriesList.adapter = adapter

        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            adapter.submit(
                AchievementCatalog.all.map { achievement ->
                    AchievementEntry(
                        name = achievement.name,
                        description = achievement.description,
                        unlocked = achievement.id in save.achievementsUnlockedIds,
                    )
                },
            )
        }
    }
}
