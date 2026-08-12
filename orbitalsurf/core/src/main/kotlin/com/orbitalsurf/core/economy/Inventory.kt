package com.orbitalsurf.core.economy

import kotlinx.serialization.Serializable

/** Owned consumables and cosmetics. Immutable, same as [Wallet]. */
@Serializable
data class Inventory(
    val missionSkipVouchers: Int = 0,
    /** checkpointIndex -> how many unused Headstart tickets for it. */
    val headstartTickets: Map<Int, Int> = emptyMap(),
    val ownedSkinIds: Set<String> = emptySet(),
    val equippedSkinId: String = DEFAULT_SKIN_ID,
) {
    companion object {
        const val DEFAULT_SKIN_ID = "skin_default"
    }
}
