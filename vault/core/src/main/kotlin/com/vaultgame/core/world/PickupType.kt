package com.vaultgame.core.world

import com.vaultgame.core.powerups.PowerupType

/** Anything sitting in a lane the player collects on contact (as opposed to obstacles they avoid). */
sealed class PickupType {
    data object Coin : PickupType()
    data class Powerup(val type: PowerupType) : PickupType()
}
