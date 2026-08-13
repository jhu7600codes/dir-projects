package com.vaultgame.core.progression

enum class AchievementCategory {
    DISTANCE,
    COINS,
    NO_HIT_STREAK,
    POWERUP_USAGE,
    GAMES_PLAYED,
}

/** [unlockSkinId] is null for achievements that are just a badge with no cosmetic reward. */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val category: AchievementCategory,
    val threshold: Long,
    val unlockSkinId: String? = null,
)
