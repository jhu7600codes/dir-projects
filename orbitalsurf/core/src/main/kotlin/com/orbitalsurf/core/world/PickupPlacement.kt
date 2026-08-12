package com.orbitalsurf.core.world

/** What collecting a given pickup gives the player. */
sealed class PickupKind {
    data object PlatesCoin : PickupKind()
    data class Powerup(val type: PowerupType) : PickupKind()
}

/** A single collectible placed in the world by `ChunkGenerator`. */
data class PickupPlacement(
    val id: String,
    val distance: Double,
    val lateral: Double,
    /** Height above the surface it floats at -- purely a rendering/collection-radius detail. */
    val height: Double,
    val kind: PickupKind,
)
