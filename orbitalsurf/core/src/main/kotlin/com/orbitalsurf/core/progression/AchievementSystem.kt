package com.orbitalsurf.core.progression

/**
 * Evaluates [AchievementCatalog] against live state. Stat-threshold achievements are computed
 * fresh every call (cheap, stateless, always correct); external-link ones need a bit of
 * memory ("has this id been marked visited"), which is the only state this class actually
 * owns -- and it's exactly what a save file needs to persist ([visitedIds]/[restoreVisited]).
 */
class AchievementSystem {
    private val externalLinkVisited = mutableSetOf<String>()

    fun unlockedStatThresholds(stats: PlayerStats): Set<Achievement.StatThresholdAchievement> =
        AchievementCatalog.all
            .filterIsInstance<Achievement.StatThresholdAchievement>()
            .filter { stats.valueFor(it.statKey) >= it.threshold }
            .toSet()

    /** Idempotent: marking an already-visited link again changes nothing. */
    fun markExternalLinkVisited(id: String) {
        externalLinkVisited += id
    }

    fun isExternalLinkVisited(id: String): Boolean = id in externalLinkVisited

    fun unlockedExternalLinks(): Set<Achievement.ExternalLinkAchievement> =
        AchievementCatalog.all
            .filterIsInstance<Achievement.ExternalLinkAchievement>()
            .filter { it.id in externalLinkVisited }
            .toSet()

    fun allUnlocked(stats: PlayerStats): Set<Achievement> = unlockedStatThresholds(stats) + unlockedExternalLinks()

    fun visitedIds(): Set<String> = externalLinkVisited.toSet()

    fun restoreVisited(ids: Set<String>) {
        externalLinkVisited.clear()
        externalLinkVisited += ids
    }
}
