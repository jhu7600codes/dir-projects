package com.orbitalsurf.core.world

import kotlin.math.ceil

/**
 * Deterministic distance <-> checkpoint-number mapping. Checkpoint N is always at the same
 * distance in every run (there's only one seed-independent schedule), which is what lets a
 * purchased Headstart simply mean "start generating chunks from this distance" -- no need to
 * simulate or remember anything about a previous run.
 */
object CheckpointSchedule {
    /** The distance (meters) at which checkpoint [n] (n >= 1) sits. */
    fun checkpointDistance(n: Int): Double {
        require(n >= 1) { "checkpoint numbers start at 1, got $n" }
        return n * WorldConstants.CHECKPOINT_INTERVAL_DISTANCE
    }

    /** Which checkpoint number (if any) has its distance falling inside the given chunk's span. */
    fun checkpointAt(chunkIndex: Long): Int? {
        val start = chunkIndex * WorldConstants.CHUNK_LENGTH
        val end = start + WorldConstants.CHUNK_LENGTH
        // Smallest checkpoint number whose distance is >= start (ceil, not floor+1 -- when
        // start itself lands exactly on a checkpoint distance, that checkpoint IS the answer,
        // not the next one), floored at 1 since there is no checkpoint 0.
        val candidateN = maxOf(1L, ceil(start / WorldConstants.CHECKPOINT_INTERVAL_DISTANCE).toLong())
        val candidateDistance = candidateN * WorldConstants.CHECKPOINT_INTERVAL_DISTANCE
        return if (candidateDistance >= start && candidateDistance < end) candidateN.toInt() else null
    }
}
