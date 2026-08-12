package com.orbitalsurf.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.orbitalsurf.app.OrbitalSurfApp
import com.orbitalsurf.app.R
import com.orbitalsurf.app.data.SkinCatalog
import com.orbitalsurf.app.databinding.ActivityGameBinding
import com.orbitalsurf.app.input.GameInputState
import com.orbitalsurf.app.render.GameRenderer
import com.orbitalsurf.core.session.GameSession
import com.orbitalsurf.core.session.RunFrameResult
import com.orbitalsurf.core.session.RunResultApplier
import com.orbitalsurf.core.world.PowerupType
import kotlinx.coroutines.launch
import java.util.Random
import java.util.concurrent.TimeUnit

/**
 * Hosts the [com.orbitalsurf.app.view.GameSurfaceView] for one run. Owns the [GameSession] and
 * drives the HUD from [GameRenderer]'s per-frame callback -- which arrives on the GL thread, so
 * every HUD update here hops back to the main thread via [runOnUiThread].
 */
class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding
    private lateinit var gameSession: GameSession
    private lateinit var inputState: GameInputState

    private var voucherCount = 0
    private var surfaceStarted = false
    private var lastHudUpdateMillis = 0L

    @Volatile
    private var gameOverHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val startDistance = intent.getDoubleExtra(EXTRA_START_DISTANCE, 0.0)
        val repository = (application as OrbitalSurfApp).gameSaveRepository

        lifecycleScope.launch {
            val save = repository.load()
            voucherCount = save.inventory.missionSkipVouchers
            val skinVisual = SkinCatalog.forId(save.inventory.equippedSkinId)

            gameSession = GameSession(seed = Random().nextLong(), startDistance = startDistance)
            inputState = GameInputState()
            val renderer = GameRenderer(gameSession, inputState, skinVisual, ::onFrame)
            binding.gameSurfaceView.start(renderer, inputState)
            surfaceStarted = true
            // The Activity's own onResume() almost always fires *before* this coroutine gets
            // here (it has to suspend for the save to load first), so the guarded onResume()
            // override below never got a chance to forward the call -- GLSurfaceView's render
            // thread starts paused and only unpauses on an explicit onResume(), so without this
            // it would sit paused forever and never draw a single frame. Calling it here too
            // (in addition to the override, which still covers pause/resume *after* this point)
            // covers the case that actually happens on every launch.
            binding.gameSurfaceView.onResume()
        }
    }

    override fun onResume() {
        super.onResume()
        if (surfaceStarted) binding.gameSurfaceView.onResume()
    }

    override fun onPause() {
        if (surfaceStarted) binding.gameSurfaceView.onPause()
        super.onPause()
    }

    /**
     * Invoked from the GL thread every frame by [GameRenderer]. HUD text/`MissionsHudView`
     * rebinding is throttled to [HUD_UPDATE_INTERVAL_MS] -- posting a full HUD rebuild (which
     * re-inflates a row per mission) 60 times a second would be wasted work for a display that
     * doesn't need sub-frame precision. `lastHudUpdateMillis` is only ever touched here, on the
     * single GL thread, so no synchronization is needed for it.
     */
    private fun onFrame(frame: RunFrameResult) {
        val now = System.currentTimeMillis()
        if (now - lastHudUpdateMillis >= HUD_UPDATE_INTERVAL_MS) {
            lastHudUpdateMillis = now
            runOnUiThread {
                if (!isFinishing) updateHud(frame)
            }
        }
        if (frame.isGameOver && !gameOverHandled) {
            gameOverHandled = true
            lifecycleScope.launch { finishRun() }
        }
    }

    private fun updateHud(frame: RunFrameResult) {
        binding.scoreText.text = getString(R.string.hud_score_format, frame.score)
        binding.multiplierText.text = getString(R.string.hud_multiplier_format, frame.missionMultiplier)
        binding.missionsHud.update(frame.activeMissions, voucherCount) { missionId -> useVoucher(missionId) }
        binding.powerupsText.text = powerupsSummary(frame)
    }

    private fun powerupsSummary(frame: RunFrameResult): String {
        val parts = mutableListOf<String>()
        if (frame.isShielded) parts += "Shield ready"
        for (powerup in frame.activePowerups) {
            val label = when (val type = powerup.type) {
                is PowerupType.Magnet -> "Magnet"
                is PowerupType.Flight -> "Flight"
                is PowerupType.PlatesMultiplier -> "Plates x${type.factor}"
                is PowerupType.ScoreMultiplier -> "Score x${type.factor}"
                PowerupType.Shield -> "Shield"
            }
            parts += "$label %.1fs".format(powerup.remainingSeconds)
        }
        return parts.joinToString("   ")
    }

    /** Called on the UI thread (via [MissionsHudView]'s button callback, dispatched from [updateHud]). */
    private fun useVoucher(missionId: String) {
        if (voucherCount <= 0) return
        val used = gameSession.useMissionSkipVoucher(missionId)
        if (!used) return
        voucherCount -= 1

        val repository = (application as OrbitalSurfApp).gameSaveRepository
        lifecycleScope.launch {
            val save = repository.load()
            val newCount = (save.inventory.missionSkipVouchers - 1).coerceAtLeast(0)
            repository.save(save.copy(inventory = save.inventory.copy(missionSkipVouchers = newCount)))
        }
    }

    private suspend fun finishRun() {
        val repository = (application as OrbitalSurfApp).gameSaveRepository
        val summary = gameSession.buildRunSummary()
        val beforeSave = repository.load()
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        val afterSave = RunResultApplier.apply(summary, beforeSave, todayEpochDay)
        repository.save(afterSave)

        val platesEarnedThisRun = afterSave.wallet.plates - beforeSave.wallet.plates
        val isNewBest = summary.finalScore > beforeSave.bestScore

        val resultIntent = Intent(this@GameActivity, ResultsActivity::class.java).apply {
            putExtra(ResultsActivity.EXTRA_SCORE, summary.finalScore)
            putExtra(ResultsActivity.EXTRA_BEST_SCORE, afterSave.bestScore)
            putExtra(ResultsActivity.EXTRA_PLATES_EARNED, platesEarnedThisRun)
            putExtra(ResultsActivity.EXTRA_NEW_BEST, isNewBest)
        }
        startActivity(resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_START_DISTANCE = "start_distance"
        private const val HUD_UPDATE_INTERVAL_MS = 80L
    }
}
