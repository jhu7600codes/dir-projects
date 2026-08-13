package com.vaultgame.core.world

data class Pickup(
    val distance: Double,
    val lane: Lane,
    val type: PickupType,
    var collected: Boolean = false,
)
