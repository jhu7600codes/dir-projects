package com.orbitalsurf.core.world

/** One generated slice of the infinite city, spanning `[startDistance, endDistance)`. */
data class Chunk(
    val index: Long,
    val startDistance: Double,
    val endDistance: Double,
    val segments: List<SurfaceSegment>,
    val obstacles: List<Obstacle>,
    val pickups: List<PickupPlacement>,
    /** Non-null if this chunk contains checkpoint N's interior room. */
    val checkpointIndex: Int? = null,
)
