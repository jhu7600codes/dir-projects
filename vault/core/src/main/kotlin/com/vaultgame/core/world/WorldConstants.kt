package com.vaultgame.core.world

/** Shared tunables for world generation and player movement, in meters/seconds throughout. */
object WorldConstants {
    /** Lateral distance between adjacent lanes, for the app's rendering projection. */
    const val LANE_WIDTH = 2.2

    /** Length of one generated rooftop segment along the run axis. */
    const val SEGMENT_LENGTH = 30.0

    /** How many segments ahead of the player stay generated/buffered at once. */
    const val SEGMENT_LOOKAHEAD = 4

    /** Minimum forward gap kept clear after any obstacle/obstacle cluster, so the next thing to
     * react to is never on top of the player at max speed. */
    const val MIN_OBSTACLE_SPACING = 6.0

    const val COIN_SPACING = 1.4
    const val COIN_ARC_LENGTH = 6

    /** Chance (0..1) that an eligible obstacle slot spawns a powerup instead of nothing. */
    const val POWERUP_SPAWN_CHANCE = 0.05

    const val COIN_VALUE = 1
}
