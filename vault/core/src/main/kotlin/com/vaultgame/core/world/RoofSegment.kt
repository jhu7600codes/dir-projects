package com.vaultgame.core.world

/**
 * One generated stretch of rooftop, [WorldConstants.SEGMENT_LENGTH] meters long, starting at
 * [startDistance]. [obstacles] and [pickups] carry absolute world distances (not offsets), so
 * the simulator and renderer never need to add [startDistance] back in themselves.
 */
data class RoofSegment(
    val index: Long,
    val theme: SegmentTheme,
    val startDistance: Double,
    val length: Double,
    val obstacles: List<Obstacle>,
    val pickups: List<Pickup>,
) {
    val endDistance: Double get() = startDistance + length
}
