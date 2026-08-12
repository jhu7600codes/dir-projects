package com.orbitalsurf.core.progression

/**
 * An unlockable, rewarding a cosmetic skin. Two flavors: automatic ones evaluated against
 * [PlayerStats] ([StatThresholdAchievement]), and honor-system ones the player marks visited
 * themselves after tapping through to an external link ([ExternalLinkAchievement]) --
 * generic and reusable rather than one-off hardcoded entries, so any future promo link is
 * just another catalog row.
 */
sealed class Achievement {
    abstract val id: String
    abstract val label: String
    abstract val rewardSkinId: String

    data class StatThresholdAchievement(
        override val id: String,
        override val label: String,
        override val rewardSkinId: String,
        val statKey: StatKey,
        val threshold: Long,
    ) : Achievement()

    data class ExternalLinkAchievement(
        override val id: String,
        override val label: String,
        override val rewardSkinId: String,
        val url: String,
    ) : Achievement()
}
