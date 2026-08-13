package com.vaultgame.core.physics

import com.vaultgame.core.powerups.PowerupType
import com.vaultgame.core.world.ObstacleType

/** Something that happened during a [RunSimulator.step], for the session/HUD layer to react to. */
sealed class RunEvent {
    data class CoinCollected(val baseValue: Int) : RunEvent()
    data class PowerupCollected(val type: PowerupType) : RunEvent()
    data class PowerupExpired(val type: PowerupType) : RunEvent()
    data class ObstacleHit(val obstacleType: ObstacleType, val shieldAbsorbed: Boolean) : RunEvent()
    data class RunEnded(val finalDistance: Double) : RunEvent()
}
