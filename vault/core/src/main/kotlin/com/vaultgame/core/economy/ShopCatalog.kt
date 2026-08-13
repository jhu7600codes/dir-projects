package com.vaultgame.core.economy

import com.vaultgame.core.powerups.PowerupConfig
import com.vaultgame.core.powerups.PowerupType

/** Everything purchasable, generated from [SkinCatalog] plus fixed price tables for upgrades,
 * the mission-skip voucher, and headstart checkpoints. */
object ShopCatalog {
    /** Checkpoint distances a run headstart can be bought for, cheapest/closest first. */
    val HEADSTART_CHECKPOINTS: List<Double> = listOf(250.0, 500.0, 1_000.0, 2_000.0)
    private val HEADSTART_PRICES: List<Long> = listOf(400, 800, 1_600, 3_000)

    private val POWERUP_UPGRADE_PRICES: List<Long> = listOf(600, 1_200, 2_200) // levels 1..3

    fun skins(): List<ShopItem.Skin> = SkinCatalog.purchasable().map { ShopItem.Skin(it) }

    /** Shield has no duration/magnitude to scale (it's a binary absorb-one-hit), so it's the
     * only powerup with no purchasable upgrade tier. */
    private val UPGRADABLE_TYPES = PowerupType.entries.filter { it != PowerupType.SHIELD }

    fun powerupUpgrades(): List<ShopItem.PowerupUpgrade> =
        UPGRADABLE_TYPES.flatMap { type ->
            (1..PowerupConfig.MAX_UPGRADE_LEVEL).map { level ->
                ShopItem.PowerupUpgrade(type, level, POWERUP_UPGRADE_PRICES[level - 1])
            }
        }

    fun headstarts(): List<ShopItem.Headstart> =
        HEADSTART_CHECKPOINTS.mapIndexed { i, distance -> ShopItem.Headstart(distance, HEADSTART_PRICES[i]) }

    fun all(): List<ShopItem> = skins() + powerupUpgrades() + listOf(ShopItem.MissionSkipVoucher) + headstarts()
}
