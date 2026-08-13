package com.vaultgame.core.economy

/** [priceInShop] null means it's not for sale -- either the default free skin or one that only
 * unlocks via an [com.vaultgame.core.progression.Achievement]'s `unlockSkinId`. */
data class SkinDefinition(
    val id: String,
    val displayName: String,
    val description: String,
    val priceInShop: Int?,
)

object SkinCatalog {
    const val DEFAULT_SKIN_ID = "skin_default"

    val all: List<SkinDefinition> = listOf(
        SkinDefinition(DEFAULT_SKIN_ID, "Street Default", "Beanie, jacket, scrappy streetwear -- the classic Vault look.", priceInShop = null),
        SkinDefinition("skin_red_beanie", "Red Beanie Crew", "Cherry-red beanie and a black bomber jacket.", priceInShop = 500),
        SkinDefinition("skin_windbreaker", "Windbreaker", "Bright windbreaker that catches the sunset light.", priceInShop = 900),
        SkinDefinition("skin_neon_runner", "Neon Runner", "Reflective neon trim, built for the downtown night runs.", priceInShop = 1_200),
        SkinDefinition("skin_neon_legend", "Neon Legend", "Unlocked at 25,000m lifetime distance.", priceInShop = null),
        SkinDefinition("skin_ghost", "Rooftop Ghost", "Unlocked at 100,000m lifetime distance.", priceInShop = null),
        SkinDefinition("skin_gold_jacket", "Gold Jacket", "Unlocked at 10,000 lifetime coins.", priceInShop = null),
        SkinDefinition("skin_platinum", "Platinum Plates", "Unlocked at 50,000 lifetime coins.", priceInShop = null),
        SkinDefinition("skin_shadow", "Shadow Runner", "Unlocked at a 10-run no-hit streak.", priceInShop = null),
        SkinDefinition("skin_jetpack_kid", "Jetpack Kid", "Unlocked at 100 lifetime powerup uses.", priceInShop = null),
    )

    fun purchasable(): List<SkinDefinition> = all.filter { it.priceInShop != null }

    fun byId(id: String): SkinDefinition? = all.firstOrNull { it.id == id }
}
