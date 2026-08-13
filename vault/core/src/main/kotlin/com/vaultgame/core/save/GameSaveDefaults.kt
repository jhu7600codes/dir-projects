package com.vaultgame.core.save

import com.vaultgame.core.math.SeededRandom
import com.vaultgame.core.progression.MissionSystem

object GameSaveDefaults {
    fun new(worldSeed: Long): GameSave =
        GameSave(worldSeed = worldSeed, missionState = MissionSystem.freshState(SeededRandom(worldSeed)))
}
