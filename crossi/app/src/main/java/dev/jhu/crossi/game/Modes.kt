package dev.jhu.crossi.game

import kotlinx.serialization.Serializable

enum class Language { EN, RU }

/** Only free roam and the dailies use this. Levels just get harder as you go. */
enum class Difficulty(val size: Int) {
    EASY(5),
    NORMAL(7),
    HARD(15),
}

enum class PowerUpType { REVEAL_LETTER, REVEAL_WORD, EXTRA_CHECK, SKIP_CLUE }

@Serializable
sealed interface GameMode {
    @Serializable
    data class Level(val number: Int) : GameMode

    @Serializable
    data class FreeRoam(val difficulty: Difficulty) : GameMode

    /** [date] is ISO yyyy-MM-dd in the player's local time zone. */
    @Serializable
    data class Daily(val date: String, val difficulty: Difficulty) : GameMode
}
