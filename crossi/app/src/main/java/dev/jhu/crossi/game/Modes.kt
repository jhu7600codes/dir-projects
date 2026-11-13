package dev.jhu.crossi.game

import dev.jhu.crossi.generator.Alphabet
import dev.jhu.crossi.generator.Rng
import kotlinx.serialization.Serializable

@Serializable
enum class Language(val code: String) {
    EN("en"),
    RU("ru");

    val alphabet: Alphabet get() = if (this == RU) Alphabet.RU else Alphabet.EN

    companion object {
        fun fromCode(code: String?): Language = entries.firstOrNull { it.code == code } ?: EN
    }
}

/** Only free roam and the dailies use this. Levels just get harder as you go. [maxHardness] is a word tier. */
@Serializable
enum class Difficulty(val size: Int, val maxHardness: Int, val scoreMultiplier: Int) {
    EASY(5, 2, 1),
    NORMAL(7, 3, 2),
    HARD(15, 4, 5),
}

@Serializable
enum class PowerUpType { REVEAL_LETTER, REVEAL_WORD, EXTRA_CHECK, SKIP_CLUE }

/**
 * Bump when words, clues or levels change. Saved games from an older version are
 * ignored, so everyone gets the regenerated puzzles instead of stale ones.
 */
const val CONTENT_VERSION = 3

/** Easy Mode (global) drops hard and super hard words everywhere. */
const val EASY_MODE_MAX_TIER = 3

@Serializable
sealed interface GameMode {
    /** Saved games are keyed by this, per language and content version. */
    fun key(language: Language): String

    @Serializable
    data class Level(val number: Int) : GameMode {
        override fun key(language: Language) = "v$CONTENT_VERSION:${language.code}:level:$number"
    }

    @Serializable
    data class FreeRoam(val difficulty: Difficulty) : GameMode {
        override fun key(language: Language) = "v$CONTENT_VERSION:${language.code}:free:${difficulty.name}"
    }

    /** [date] is ISO yyyy-MM-dd in the player's local time zone. */
    @Serializable
    data class Daily(val date: String, val difficulty: Difficulty) : GameMode {
        override fun key(language: Language) = "v$CONTENT_VERSION:${language.code}:daily:$date:${difficulty.name}"
    }
}

object Seeds {
    /** Same date + difficulty + language + easy mode + dictionary version = same puzzle for everyone. */
    fun daily(date: String, difficulty: Difficulty, language: Language, dictionaryVersion: Int, easyMode: Boolean = false): Long =
        Rng.seedOf("crossi-daily-$date-${difficulty.name}-${language.code}-v$dictionaryVersion" + if (easyMode) "-easy" else "")

    fun level(number: Int, language: Language): Long = Rng.seedOf("crossi-level-$number-${language.code}")
}
