package com.jhulian.android.youtube.classic.playback

import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import com.jhulian.android.youtube.classic.network.SponsorSegment

/**
 * Watches playback position against a fetched list of [SponsorSegment]s and
 * either auto-skips or surfaces a manual "Skip" affordance, mirroring the
 * SponsorBlock browser extension's own behaviour in the player.
 *
 * Polls on a 250ms handler tick rather than reacting to ExoPlayer position
 * events, because Media3 doesn't fire a callback on every progressed
 * second - only on discontinuities/state changes - and a segment boundary
 * can land anywhere between those.
 */
class SponsorBlockController(
    private val player: Player,
    private val autoSkip: Boolean,
    private val onAutoSkipped: (SponsorSegment) -> Unit,
    private val onManualSkipAvailable: (SponsorSegment?) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var segments: List<SponsorSegment> = emptyList()
    private var lastManualSegment: SponsorSegment? = null
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            checkPosition()
            if (running) handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    fun setSegments(segments: List<SponsorSegment>) {
        this.segments = segments
    }

    fun start() {
        if (running) return
        running = true
        handler.post(tick)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
    }

    private fun checkPosition() {
        if (segments.isEmpty()) return
        val positionSeconds = player.currentPosition / 1000.0
        val activeSegment = segments.firstOrNull {
            positionSeconds >= it.startSeconds && positionSeconds < it.endSeconds - MIN_REMAINING_SECONDS
        }

        if (activeSegment == null) {
            if (lastManualSegment != null) {
                lastManualSegment = null
                onManualSkipAvailable(null)
            }
            return
        }

        if (autoSkip) {
            player.seekTo((activeSegment.endSeconds * 1000).toLong())
            onAutoSkipped(activeSegment)
        } else if (lastManualSegment != activeSegment) {
            lastManualSegment = activeSegment
            onManualSkipAvailable(activeSegment)
        }
    }

    fun skipManually(segment: SponsorSegment) {
        player.seekTo((segment.endSeconds * 1000).toLong())
        lastManualSegment = null
        onManualSkipAvailable(null)
    }

    companion object {
        private const val POLL_INTERVAL_MS = 250L
        private const val MIN_REMAINING_SECONDS = 0.3
    }
}
