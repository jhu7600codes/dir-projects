package com.vaultgame.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vaultgame.app.R
import com.vaultgame.app.VaultApp
import com.vaultgame.app.data.SkinVisuals
import com.vaultgame.app.databinding.ActivityGameBinding
import com.vaultgame.app.render.GameRenderer
import com.vaultgame.core.powerups.PowerupType
import com.vaultgame.core.progression.MissionSystem
import com.vaultgame.core.progression.ScoreSystem
import com.vaultgame.core.session.GameSession
import com.vaultgame.core.session.RunResultApplier
import kotlinx.coroutines.launch

/**
 * Hosts [com.vaultgame.app.view.GameSurfaceView] for one run. The surface view's Choreographer
 * loop already runs on the main thread (see GameSurfaceView), so [onFrame] can touch view state
 * directly with no thread hop.
 */
class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding
    private lateinit var gameSession: GameSession
    private var scoreMultiplier = 1.0

    @Volatile
    private var gameOverHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            val (session, saveAfterHeadstartConsumed) = RunResultApplier.beginRun(save)
            // Persist immediately so a crash mid-run doesn't let the same headstart be spent twice.
            repository.save(saveAfterHeadstartConsumed)

            gameSession = session
            scoreMultiplier = MissionSystem.consumePendingMultiplier(saveAfterHeadstartConsumed.missionState).first
            val skinVisual = SkinVisuals.forId(saveAfterHeadstartConsumed.inventory.equippedSkin)
            binding.platesText.text = getString(R.string.hud_plates_format, saveAfterHeadstartConsumed.wallet.plates.toInt())

            binding.gameSurfaceView.start(gameSession, GameRenderer(skinVisual), ::onFrame)
            binding.gameSurfaceView.resumeLoop()
        }

        binding.pauseButton.setOnClickListener { showPause() }
        binding.resumeButton.setOnClickListener { hidePause() }
        binding.quitButton.setOnClickListener { finishRun() }
    }

    override fun onPause() {
        if (::gameSession.isInitialized) binding.gameSurfaceView.pauseLoop()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::gameSession.isInitialized && binding.pauseOverlay.visibility != android.view.View.VISIBLE) {
            binding.gameSurfaceView.resumeLoop()
        }
    }

    private fun showPause() {
        binding.pauseOverlay.visibility = android.view.View.VISIBLE
        binding.gameSurfaceView.pauseLoop()
    }

    private fun hidePause() {
        binding.pauseOverlay.visibility = android.view.View.GONE
        binding.gameSurfaceView.resumeLoop()
    }

    /** Fired once per simulation tick, on the main thread, right after physics. */
    private fun onFrame(session: GameSession) {
        val score = ScoreSystem.computeScore(session.playerState.distance, session.coinsCollected, scoreMultiplier)
        binding.scoreText.text = getString(R.string.hud_score_format, score.toInt())
        binding.powerupsText.text = powerupsSummary(session)

        if (session.runEnded && !gameOverHandled) {
            gameOverHandled = true
            binding.gameSurfaceView.pauseLoop()
            finishRun()
        }
    }

    private fun powerupsSummary(session: GameSession): String {
        val powerups = session.activePowerupsSnapshot()
        val parts = mutableListOf<String>()
        if (powerups.isActive(PowerupType.SHIELD)) parts += "Shield ready"
        for (type in PowerupType.entries) {
            if (type == PowerupType.SHIELD || !powerups.isActive(type)) continue
            val label = type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
            parts += "$label %.1fs".format(powerups.remainingSeconds(type))
        }
        return parts.joinToString("   ")
    }

    private fun finishRun() {
        val repository = (application as VaultApp).gameSaveRepository
        lifecycleScope.launch {
            val beforeSave = repository.load()
            val result = RunResultApplier.apply(beforeSave, gameSession, nowEpochMillis = System.currentTimeMillis())
            repository.save(result.updatedSave)

            val intent = Intent(this@GameActivity, ResultsActivity::class.java).apply {
                putExtra(ResultsActivity.EXTRA_SCORE, result.summary.score)
                putExtra(ResultsActivity.EXTRA_BEST_SCORE, result.updatedSave.playerStats.bestScore)
                putExtra(ResultsActivity.EXTRA_COINS, result.summary.coinsCollected)
                putExtra(ResultsActivity.EXTRA_DISTANCE, result.summary.distanceMeters)
                putExtra(ResultsActivity.EXTRA_PLATES_EARNED, result.platesEarned)
                putExtra(ResultsActivity.EXTRA_NEW_ACHIEVEMENTS, result.newlyUnlockedAchievements.map { it.name }.toTypedArray())
            }
            startActivity(intent)
            finish()
        }
    }
}
