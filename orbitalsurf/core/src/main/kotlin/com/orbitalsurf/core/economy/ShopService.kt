package com.orbitalsurf.core.economy

data class PurchaseResult(val wallet: Wallet, val inventory: Inventory, val success: Boolean)

/**
 * Purchases are atomic: either the wallet is debited and the inventory updated together, or
 * neither changes at all (the returned `wallet`/`inventory` are the same instances passed in
 * on failure) -- there's no partial-purchase state to clean up.
 */
object ShopService {
    fun purchase(
        item: ShopItem,
        wallet: Wallet,
        inventory: Inventory,
        unlockedCheckpoints: Set<Int> = emptySet(),
    ): PurchaseResult {
        if (item is ShopItem.HeadstartItem && item.checkpointIndex !in unlockedCheckpoints) {
            return PurchaseResult(wallet, inventory, success = false)
        }
        if (item is ShopItem.CosmeticSkinItem && item.skinId in inventory.ownedSkinIds) {
            // Idempotent: already owned, no charge, nothing to grant a second time.
            return PurchaseResult(wallet, inventory, success = false)
        }

        val debited = wallet.debit(item.price) ?: return PurchaseResult(wallet, inventory, success = false)
        val updatedInventory = when (item) {
            is ShopItem.MissionSkipVoucherItem -> inventory.copy(missionSkipVouchers = inventory.missionSkipVouchers + 1)
            is ShopItem.HeadstartItem -> inventory.copy(
                headstartTickets = inventory.headstartTickets +
                    (item.checkpointIndex to ((inventory.headstartTickets[item.checkpointIndex] ?: 0) + 1)),
            )
            is ShopItem.CosmeticSkinItem -> inventory.copy(ownedSkinIds = inventory.ownedSkinIds + item.skinId)
        }
        return PurchaseResult(debited, updatedInventory, success = true)
    }
}
