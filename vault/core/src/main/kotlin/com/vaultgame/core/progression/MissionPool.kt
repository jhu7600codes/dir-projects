package com.vaultgame.core.progression

import com.vaultgame.core.math.SeededRandom
import com.vaultgame.core.powerups.PowerupType

/** A generator template: how to roll one concrete [Mission] from a template kind. */
private data class MissionTemplate(
    val idPrefix: String,
    val type: MissionTargetType,
    val targetRange: IntRange,
    val describe: (target: Int, powerupType: PowerupType?) -> String,
)

/** Catalog of mission templates and the roller that builds a fresh 3-goal [MissionSet]. */
object MissionPool {
    private val templates = listOf(
        MissionTemplate("collect_coins", MissionTargetType.COLLECT_COINS, 30..80) { t, _ ->
            "Collect $t coins"
        },
        MissionTemplate("run_distance", MissionTargetType.RUN_DISTANCE_SINGLE_RUN, 300..800) { t, _ ->
            "Run ${t}m in one run"
        },
        MissionTemplate("use_powerup", MissionTargetType.USE_POWERUP, 2..5) { t, p ->
            "Use ${p?.name?.lowercase()?.replace('_', ' ')} $t times"
        },
        MissionTemplate("collect_powerups", MissionTargetType.COLLECT_POWERUPS_TOTAL, 5..12) { t, _ ->
            "Collect $t powerups"
        },
        MissionTemplate("clean_streak", MissionTargetType.CLEAN_RUN_STREAK, 1..3) { t, _ ->
            "Finish $t run${if (t > 1) "s" else ""} in a row without getting hit"
        },
    )

    fun rollSet(rng: SeededRandom, setIndex: Long): MissionSet {
        val chosen = rng.shuffle(templates).take(3)
        val missions = chosen.mapIndexed { i, template ->
            val target = rng.nextInt(template.targetRange.first, template.targetRange.last + 1)
            val powerupType = if (template.type == MissionTargetType.USE_POWERUP) {
                rng.pick(PowerupType.entries.toList())
            } else null
            Mission(
                id = "${template.idPrefix}_${setIndex}_$i",
                description = template.describe(target, powerupType),
                targetType = template.type,
                targetValue = target,
                powerupType = powerupType,
            )
        }
        return MissionSet(missions)
    }
}
