package com.vaultgame.core.world

/**
 * A single obstacle placed at [distance] (world-forward meters from run start). [lane] is
 * meaningless when [ObstacleType.spansAllLanes] is true -- the generator always fills it with
 * [Lane.CENTER] in that case, but every lane is affected.
 */
data class Obstacle(
    val distance: Double,
    val lane: Lane,
    val type: ObstacleType,
    var resolved: Boolean = false,
) {
    fun blocksLane(target: Lane): Boolean = type.spansAllLanes || lane == target
}
