package com.vaultgame.core.economy

import com.vaultgame.core.powerups.PowerupType
import kotlinx.serialization.Serializable

@Serializable
data class Inventory(
    val ownedSkins: Set<String> = setOf(SkinCatalog.DEFAULT_SKIN_ID),
    val equippedSkin: String = SkinCatalog.DEFAULT_SKIN_ID,
    val powerupUpgradeLevels: Map<PowerupType, Int> = emptyMap(),
    val missionSkipVouchers: Int = 0,
    val unlockedHeadstarts: Set<Double> = emptySet(),
) {
    fun upgradeLevel(type: PowerupType): Int = powerupUpgradeLevels[type] ?: 0

    fun withSkinUnlocked(skinId: String): Inventory = copy(ownedSkins = ownedSkins + skinId)

    fun withSkinEquipped(skinId: String): Inventory =
        if (skinId in ownedSkins) copy(equippedSkin = skinId) else this
}
