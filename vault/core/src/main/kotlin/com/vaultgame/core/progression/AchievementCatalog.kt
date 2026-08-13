package com.vaultgame.core.progression

/** Static tiered achievement list. Thresholds compare against lifetime [PlayerStats] fields
 * (see [AchievementSystem]), never a single run's numbers. */
object AchievementCatalog {
    val all: List<Achievement> = listOf(
        // Distance milestones (lifetime meters run).
        Achievement("dist_1k", "Rookie Rooftopper", "Run a lifetime total of 1,000m", AchievementCategory.DISTANCE, 1_000),
        Achievement("dist_5k", "Skyline Regular", "Run a lifetime total of 5,000m", AchievementCategory.DISTANCE, 5_000),
        Achievement(
            "dist_25k", "Vault Legend", "Run a lifetime total of 25,000m",
            AchievementCategory.DISTANCE, 25_000, unlockSkinId = "skin_neon_legend",
        ),
        Achievement(
            "dist_100k", "Rooftop Ghost", "Run a lifetime total of 100,000m",
            AchievementCategory.DISTANCE, 100_000, unlockSkinId = "skin_ghost",
        ),

        // Coin milestones (lifetime coins collected).
        Achievement("coins_500", "Pocket Change", "Collect 500 coins total", AchievementCategory.COINS, 500),
        Achievement("coins_2500", "Coin Hoarder", "Collect 2,500 coins total", AchievementCategory.COINS, 2_500),
        Achievement(
            "coins_10000", "Vault Cracker", "Collect 10,000 coins total",
            AchievementCategory.COINS, 10_000, unlockSkinId = "skin_gold_jacket",
        ),
        Achievement(
            "coins_50000", "Plates for Days", "Collect 50,000 coins total",
            AchievementCategory.COINS, 50_000, unlockSkinId = "skin_platinum",
        ),

        // No-hit run streaks (consecutive clean runs).
        Achievement("streak_3", "Clean Getaway", "Finish 3 runs in a row without getting hit", AchievementCategory.NO_HIT_STREAK, 3),
        Achievement("streak_5", "Untouchable", "Finish 5 runs in a row without getting hit", AchievementCategory.NO_HIT_STREAK, 5),
        Achievement(
            "streak_10", "Ghost Protocol", "Finish 10 runs in a row without getting hit",
            AchievementCategory.NO_HIT_STREAK, 10, unlockSkinId = "skin_shadow",
        ),

        // Powerup usage (lifetime activations, any type).
        Achievement("powerup_25", "Power Player", "Use 25 powerups total", AchievementCategory.POWERUP_USAGE, 25),
        Achievement(
            "powerup_100", "Fully Loaded", "Use 100 powerups total",
            AchievementCategory.POWERUP_USAGE, 100, unlockSkinId = "skin_jetpack_kid",
        ),

        // Games played.
        Achievement("games_10", "Regular", "Play 10 runs", AchievementCategory.GAMES_PLAYED, 10),
        Achievement("games_100", "Rooftop Resident", "Play 100 runs", AchievementCategory.GAMES_PLAYED, 100),
    )
}
