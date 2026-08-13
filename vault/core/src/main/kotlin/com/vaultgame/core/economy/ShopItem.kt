package com.vaultgame.core.economy

import com.vaultgame.core.powerups.PowerupType

/** Everything the shop sells. No item here affects run difficulty or obstacle survivability --
 * only cosmetics ([Skin]) and convenience (longer/stronger powerups, skipping a mission set,
 * starting a run further in). */
sealed class ShopItem(
    open val id: String,
    open val name: String,
    open val description: String,
    open val price: Long,
) {
    data class Skin(val definition: SkinDefinition) : ShopItem(
        id = definition.id,
        name = definition.displayName,
        description = definition.description,
        price = (definition.priceInShop ?: 0).toLong(),
    )

    /** Buying this raises [powerupType]'s duration/magnitude to [level] (see PowerupConfig).
     * Levels must be purchased in order: level 2 requires level 1 already owned. */
    data class PowerupUpgrade(
        val powerupType: PowerupType,
        val level: Int,
        override val price: Long,
    ) : ShopItem(
        id = "upgrade_${powerupType.name.lowercase()}_l$level",
        name = "${powerupType.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }} Upgrade $level",
        description = "Increases ${powerupType.name.lowercase().replace('_', ' ')} duration and strength.",
        price = price,
    )

    /** Consumable: instantly completes the active mission set. Stackable -- buying more than
     * one just banks more vouchers in Inventory.missionSkipVouchers. */
    data object MissionSkipVoucher : ShopItem(
        id = "mission_skip_voucher",
        name = "Mission Skip Voucher",
        description = "Instantly completes your current mission set.",
        price = 350,
    )

    /** One-time unlock per checkpoint distance: future runs can start there instead of at 0m. */
    data class Headstart(val checkpointDistanceMeters: Double, override val price: Long) : ShopItem(
        id = "headstart_${checkpointDistanceMeters.toInt()}",
        name = "Headstart: ${checkpointDistanceMeters.toInt()}m",
        description = "Start your next run already ${checkpointDistanceMeters.toInt()}m in.",
        price = price,
    )
}
