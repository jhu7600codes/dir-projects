package com.orbitalsurf.core.economy

/** The full, data-driven shop listing. */
object ShopCatalog {
    val missionSkipVoucher = ShopItem.MissionSkipVoucherItem(id = "shop_mission_skip_voucher", price = 80)

    val cosmeticSkins: List<ShopItem.CosmeticSkinItem> = listOf(
        ShopItem.CosmeticSkinItem(skinId = "skin_neon_ball", id = "shop_skin_neon_ball", price = 500),
        ShopItem.CosmeticSkinItem(skinId = "skin_checker_ball", id = "shop_skin_checker_ball", price = 350),
        ShopItem.CosmeticSkinItem(skinId = "skin_lava_ball", id = "shop_skin_lava_ball", price = 750),
    )

    private const val HEADSTART_PRICE_PER_TIER = 100L

    /** Headstart price grows with checkpoint tier -- skipping further ahead is worth more plates. */
    fun headstartFor(checkpointIndex: Int): ShopItem.HeadstartItem {
        require(checkpointIndex >= 1) { "checkpoint numbers start at 1, got $checkpointIndex" }
        return ShopItem.HeadstartItem(
            checkpointIndex = checkpointIndex,
            id = "shop_headstart_$checkpointIndex",
            price = HEADSTART_PRICE_PER_TIER * checkpointIndex,
        )
    }

    /** The Headstart section only ever lists checkpoints the player has actually unlocked. */
    fun all(unlockedCheckpoints: Set<Int>): List<ShopItem> =
        listOf(missionSkipVoucher) + cosmeticSkins + unlockedCheckpoints.sorted().map { headstartFor(it) }
}
