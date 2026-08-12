package com.orbitalsurf.core.economy

sealed class ShopItem {
    abstract val id: String
    abstract val price: Long

    /** Consumable, stackable -- spending one instantly finishes one active mission (`MissionSystem.forceComplete`). */
    data class MissionSkipVoucherItem(override val id: String, override val price: Long) : ShopItem()

    /** Consumable ticket for a checkpoint the player has already reached at least once -- lets a future run start there. */
    data class HeadstartItem(val checkpointIndex: Int, override val id: String, override val price: Long) : ShopItem()

    /** A permanent, one-time cosmetic unlock. */
    data class CosmeticSkinItem(val skinId: String, override val id: String, override val price: Long) : ShopItem()
}
