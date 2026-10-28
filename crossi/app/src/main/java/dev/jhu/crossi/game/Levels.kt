package dev.jhu.crossi.game

import dev.jhu.crossi.generator.Rng
import kotlinx.serialization.Serializable

@Serializable
enum class BossTwist {
    TIMER,
    LOCKED_CELLS,
    SCRAMBLED_CLUES,
    LIMITED_MISTAKES,
    FOG,
    TRICK_FEST,
    SCRAMBLED_KEYBOARD,
    ONE_SHOT,
}

data class LevelSpec(
    val number: Int,
    val size: Int,
    val maxHardness: Int,
    val twists: List<BossTwist>,
    /** entry index -> power up you get for solving that entry. Picked at runtime from the level seed. */
    val pickupCount: Int,
) {
    val isBoss: Boolean get() = twists.isNotEmpty()
}

/** The fixed level list: gets harder the further you go, a boss every 10. */
object LevelCatalog {
    const val COUNT = 100
    const val BOSS_EVERY = 10

    private val bosses = mapOf(
        10 to listOf(BossTwist.TIMER),
        20 to listOf(BossTwist.LOCKED_CELLS),
        30 to listOf(BossTwist.SCRAMBLED_CLUES),
        40 to listOf(BossTwist.LIMITED_MISTAKES),
        50 to listOf(BossTwist.FOG),
        60 to listOf(BossTwist.TRICK_FEST),
        70 to listOf(BossTwist.TIMER, BossTwist.LOCKED_CELLS),
        80 to listOf(BossTwist.SCRAMBLED_CLUES, BossTwist.LIMITED_MISTAKES),
        90 to listOf(BossTwist.FOG, BossTwist.SCRAMBLED_KEYBOARD),
        100 to listOf(BossTwist.ONE_SHOT, BossTwist.TIMER, BossTwist.TRICK_FEST),
    )

    fun spec(number: Int): LevelSpec {
        require(number in 1..COUNT) { "no level $number" }
        val size = when (number) {
            in 1..15 -> 5
            in 16..35 -> 7
            in 36..55 -> 9
            in 56..70 -> 11
            in 71..85 -> 13
            else -> 15
        }
        val hardness = when (number) {
            in 1..35 -> 1
            in 36..70 -> 2
            else -> 3
        }
        val twists = bosses[number].orEmpty()
        val pickups = when {
            twists.isNotEmpty() -> 0
            number % 9 == 0 -> 2
            number % 3 == 0 -> 1
            else -> 0
        }
        return LevelSpec(number, size, hardness, twists, pickups)
    }

    /** Which entries carry a pickup and what it gives. Deterministic per level. */
    fun pickups(spec: LevelSpec, entryCount: Int): Map<Int, PowerUpType> {
        if (spec.pickupCount == 0 || entryCount == 0) return emptyMap()
        val rng = Rng(Rng.seedOf("crossi-pickups-${spec.number}"))
        val entries = (0 until entryCount).toMutableList().also(rng::shuffle)
        return entries.take(spec.pickupCount).associateWith { PowerUpType.entries[rng.nextInt(PowerUpType.entries.size)] }
    }
}
