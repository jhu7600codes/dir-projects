package com.orbitalsurf.core.world

/**
 * Tuning shared across `ChunkGenerator`, `CheckpointSchedule`, and `WorldStreamer` so they
 * can never disagree about chunk length or checkpoint spacing -- both of those are baked
 * into every distance-to-chunk-index calculation in this package.
 */
object WorldConstants {
    /** Distance (meters) each generated chunk spans along the path. */
    const val CHUNK_LENGTH = 40.0

    /** Distance (meters) between successive in-building checkpoints (checkpoint 1, 2, 3, ...). */
    const val CHECKPOINT_INTERVAL_DISTANCE = 500.0
}
