package com.vaultgame.core.progression

import com.vaultgame.core.math.SeededRandom
import com.vaultgame.core.powerups.PowerupType

private data class DailyTemplate(
    val type: MissionTargetType,
    val targetRange: IntRange,
    val rewardPlates: Int,
    val describe: (target: Int, powerupType: PowerupType?) -> String,
)

/** Harder-scaled equivalents of [MissionPool]'s templates, plus a bigger plates reward. */
object DailyChallengePool {
    private val templates = listOf(
        DailyTemplate(MissionTargetType.COLLECT_COINS, 150..300, 600) { t, _ -> "Collect $t coins today" },
        DailyTemplate(MissionTargetType.RUN_DISTANCE_SINGLE_RUN, 1200..2000, 700) { t, _ ->
            "Run ${t}m in a single run"
        },
        DailyTemplate(MissionTargetType.USE_POWERUP, 6..10, 550) { t, p ->
            "Use ${p?.name?.lowercase()?.replace('_', ' ')} $t times today"
        },
        DailyTemplate(MissionTargetType.COLLECT_POWERUPS_TOTAL, 12..20, 550) { t, _ ->
            "Collect $t powerups today"
        },
        DailyTemplate(MissionTargetType.CLEAN_RUN_STREAK, 3..5, 650) { t, _ ->
            "Finish $t runs in a row without getting hit"
        },
    )

    fun rollForDay(dayKey: Long, rng: SeededRandom): DailyChallenge {
        val template = rng.pick(templates)
        val target = rng.nextInt(template.targetRange.first, template.targetRange.last + 1)
        val powerupType = if (template.type == MissionTargetType.USE_POWERUP) {
            rng.pick(PowerupType.entries.toList())
        } else null
        return DailyChallenge(
            dayKey = dayKey,
            description = template.describe(target, powerupType),
            targetType = template.type,
            targetValue = target,
            powerupType = powerupType,
            rewardPlates = template.rewardPlates,
        )
    }
}
