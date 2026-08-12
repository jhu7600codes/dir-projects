package com.orbitalsurf.core.progression

/**
 * The full, data-driven achievement list -- including the two honor-system Appteka entries
 * (see `Achievement.ExternalLinkAchievement`'s kdoc). Adding a new achievement is adding a row
 * here, nothing else.
 */
object AchievementCatalog {
    val all: List<Achievement> = listOf(
        Achievement.StatThresholdAchievement(
            id = "distance_1k",
            label = "Travel 1,000m total",
            rewardSkinId = "skin_bronze_roller",
            statKey = StatKey.TOTAL_DISTANCE,
            threshold = 1_000,
        ),
        Achievement.StatThresholdAchievement(
            id = "distance_10k",
            label = "Travel 10,000m total",
            rewardSkinId = "skin_silver_roller",
            statKey = StatKey.TOTAL_DISTANCE,
            threshold = 10_000,
        ),
        Achievement.StatThresholdAchievement(
            id = "distance_100k",
            label = "Travel 100,000m total",
            rewardSkinId = "skin_gold_roller",
            statKey = StatKey.TOTAL_DISTANCE,
            threshold = 100_000,
        ),
        Achievement.StatThresholdAchievement(
            id = "plates_1k",
            label = "Earn 1,000 plates total",
            rewardSkinId = "skin_plates_hoarder",
            statKey = StatKey.TOTAL_PLATES_EARNED,
            threshold = 1_000,
        ),
        Achievement.StatThresholdAchievement(
            id = "missions_25",
            label = "Complete 25 mission sets",
            rewardSkinId = "skin_mission_ace",
            statKey = StatKey.MISSIONS_COMPLETED,
            threshold = 25,
        ),
        Achievement.StatThresholdAchievement(
            id = "dailies_10",
            label = "Complete 10 daily challenges",
            rewardSkinId = "skin_daily_grinder",
            statKey = StatKey.DAILIES_COMPLETED,
            threshold = 10,
        ),
        Achievement.ExternalLinkAchievement(
            id = "appteka_follow",
            label = "Follow jhu codes bullshit on Appteka",
            rewardSkinId = "skin_appteka_follower",
            url = "https://appteka.store/profile/575675",
        ),
        Achievement.ExternalLinkAchievement(
            id = "appteka_subway_surfers_og",
            label = "Check out Subway Surfers OG on Appteka",
            rewardSkinId = "skin_appteka_supporter",
            url = "https://appteka.store/profile/575675",
        ),
    )
}
