package com.vaultgame.core.economy

/** Validates and applies a purchase. Every branch is a pure function of (item, wallet,
 * inventory) -- no hidden state, so it's trivial to unit test every rejection path. */
object ShopService {
    sealed class PurchaseResult {
        data class Success(val wallet: Wallet, val inventory: Inventory) : PurchaseResult()
        data object InsufficientFunds : PurchaseResult()
        data object AlreadyOwned : PurchaseResult()
        /** Powerup upgrades must be bought in order: level 2 requires level 1 first. */
        data object SkipsPrerequisiteLevel : PurchaseResult()
    }

    fun purchase(item: ShopItem, wallet: Wallet, inventory: Inventory): PurchaseResult {
        when (item) {
            is ShopItem.Skin -> if (item.definition.id in inventory.ownedSkins) return PurchaseResult.AlreadyOwned
            is ShopItem.Headstart ->
                if (item.checkpointDistanceMeters in inventory.unlockedHeadstarts) return PurchaseResult.AlreadyOwned
            is ShopItem.PowerupUpgrade -> {
                val owned = inventory.upgradeLevel(item.powerupType)
                if (owned >= item.level) return PurchaseResult.AlreadyOwned
                if (owned != item.level - 1) return PurchaseResult.SkipsPrerequisiteLevel
            }
            ShopItem.MissionSkipVoucher -> Unit // always stackable, no ownership check
        }

        val newWallet = wallet.debit(item.price) ?: return PurchaseResult.InsufficientFunds
        val newInventory = when (item) {
            is ShopItem.Skin -> inventory.withSkinUnlocked(item.definition.id)
            is ShopItem.Headstart ->
                inventory.copy(unlockedHeadstarts = inventory.unlockedHeadstarts + item.checkpointDistanceMeters)
            is ShopItem.PowerupUpgrade ->
                inventory.copy(
                    powerupUpgradeLevels = inventory.powerupUpgradeLevels + (item.powerupType to item.level),
                )
            ShopItem.MissionSkipVoucher -> inventory.copy(missionSkipVouchers = inventory.missionSkipVouchers + 1)
        }
        return PurchaseResult.Success(newWallet, newInventory)
    }

    /** Spends one voucher (does nothing to the wallet -- vouchers were already paid for). Caller
     * is responsible for actually applying MissionSystem.skip when this succeeds. */
    fun redeemMissionSkipVoucher(inventory: Inventory): Inventory? =
        if (inventory.missionSkipVouchers > 0) inventory.copy(missionSkipVouchers = inventory.missionSkipVouchers - 1) else null
}
