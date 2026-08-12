package com.orbitalsurf.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.orbitalsurf.app.OrbitalSurfApp
import com.orbitalsurf.app.R
import com.orbitalsurf.app.databinding.ActivityAchievementsBinding
import com.orbitalsurf.core.progression.Achievement
import com.orbitalsurf.core.progression.AchievementCatalog
import com.orbitalsurf.core.progression.AchievementSystem
import com.orbitalsurf.core.save.GameSave
import kotlinx.coroutines.launch

/**
 * Stat-threshold achievements unlock automatically (see `RunResultApplier`); the two Appteka
 * ones are honor-system -- tapping "Open" launches the link, and [onResume] marks *that
 * specific* achievement visited only because it was the one just tapped (tracked via
 * [pendingExternalLinkId], not "any resume unlocks it" -- an ordinary app-switch resume leaves
 * this null and does nothing).
 */
class AchievementsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAchievementsBinding
    private lateinit var adapter: AchievementsAdapter
    private var pendingExternalLinkId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AchievementsAdapter(
            onOpenLink = { achievement -> openLink(achievement) },
            onEquip = { skinId -> equipSkin(skinId) },
        )
        binding.achievementsList.layoutManager = LinearLayoutManager(this)
        binding.achievementsList.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val pendingId = pendingExternalLinkId
        if (pendingId != null) {
            pendingExternalLinkId = null
            markExternalLinkVisited(pendingId)
        } else {
            refresh()
        }
    }

    private fun refresh() {
        val repository = (application as OrbitalSurfApp).gameSaveRepository
        lifecycleScope.launch {
            adapter.submitList(computeRows(repository.load()))
        }
    }

    private fun computeRows(save: GameSave): List<AchievementRow> {
        val achievementSystem = AchievementSystem().apply { restoreVisited(save.visitedExternalLinkAchievementIds) }
        val unlockedStats = achievementSystem.unlockedStatThresholds(save.playerStats)

        return AchievementCatalog.all.map { achievement ->
            when (achievement) {
                is Achievement.StatThresholdAchievement -> {
                    val unlocked = achievement in unlockedStats
                    val current = save.playerStats.valueFor(achievement.statKey)
                    AchievementRow(
                        achievement = achievement,
                        unlocked = unlocked,
                        progressText = if (unlocked) {
                            getString(R.string.achievement_unlocked)
                        } else {
                            getString(R.string.achievement_progress_format, current, achievement.threshold)
                        },
                        actionText = equipActionText(achievement, unlocked, save),
                        actionEnabled = unlocked && save.inventory.equippedSkinId != achievement.rewardSkinId,
                        isExternalLink = false,
                    )
                }
                is Achievement.ExternalLinkAchievement -> {
                    val unlocked = achievementSystem.isExternalLinkVisited(achievement.id)
                    AchievementRow(
                        achievement = achievement,
                        unlocked = unlocked,
                        progressText = if (unlocked) getString(R.string.achievement_unlocked) else getString(R.string.achievement_locked),
                        actionText = if (!unlocked) getString(R.string.achievement_visit_link) else equipActionText(achievement, true, save),
                        actionEnabled = !unlocked || save.inventory.equippedSkinId != achievement.rewardSkinId,
                        isExternalLink = true,
                    )
                }
            }
        }
    }

    private fun equipActionText(achievement: Achievement, unlocked: Boolean, save: GameSave): String =
        if (!unlocked) {
            ""
        } else if (save.inventory.equippedSkinId == achievement.rewardSkinId) {
            "Equipped"
        } else {
            "Equip"
        }

    private fun openLink(achievement: Achievement.ExternalLinkAchievement) {
        pendingExternalLinkId = achievement.id
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(achievement.url)))
    }

    private fun markExternalLinkVisited(achievementId: String) {
        val repository = (application as OrbitalSurfApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            val achievement = AchievementCatalog.all
                .filterIsInstance<Achievement.ExternalLinkAchievement>()
                .firstOrNull { it.id == achievementId }

            val newVisited = save.visitedExternalLinkAchievementIds + achievementId
            val newOwnedSkins = if (achievement != null) {
                save.inventory.ownedSkinIds + achievement.rewardSkinId
            } else {
                save.inventory.ownedSkinIds
            }
            repository.save(
                save.copy(
                    visitedExternalLinkAchievementIds = newVisited,
                    inventory = save.inventory.copy(ownedSkinIds = newOwnedSkins),
                ),
            )
            refresh()
        }
    }

    private fun equipSkin(skinId: String) {
        val repository = (application as OrbitalSurfApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            if (skinId !in save.inventory.ownedSkinIds) return@launch
            repository.save(save.copy(inventory = save.inventory.copy(equippedSkinId = skinId)))
            refresh()
        }
    }
}
