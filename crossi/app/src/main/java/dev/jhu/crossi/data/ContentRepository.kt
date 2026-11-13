package dev.jhu.crossi.data

import android.content.Context
import dev.jhu.crossi.game.Difficulty
import dev.jhu.crossi.game.EASY_MODE_MAX_TIER
import dev.jhu.crossi.game.Language
import dev.jhu.crossi.generator.GenConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import dev.jhu.crossi.generator.Dictionary
import dev.jhu.crossi.generator.DictionaryParser
import dev.jhu.crossi.generator.Puzzle
import dev.jhu.crossi.generator.PuzzleGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Bundled content: dictionaries, the generator built on them, frozen levels. Loaded lazily, cached. */
class ContentRepository(private val context: Context, private val scope: CoroutineScope) {
    private val mutex = Mutex()
    private val dictionaries = HashMap<Language, Dictionary>()
    private val generators = HashMap<Language, PuzzleGenerator>()
    private val levels = HashMap<Pair<Language, Boolean>, List<Puzzle>>()

    suspend fun dictionary(language: Language): Dictionary = mutex.withLock {
        dictionaries.getOrPut(language) {
            withContext(Dispatchers.IO) {
                Dictionary(
                    alphabet = language.alphabet,
                    words = DictionaryParser.parse(asset("${language.code}/words.tsv")),
                    longWords = DictionaryParser.parse(asset("${language.code}/longwords.tsv")),
                    version = dictionaryVersion(language),
                )
            }
        }
    }

    suspend fun generator(language: Language): PuzzleGenerator {
        val dictionary = dictionary(language)
        return mutex.withLock { generators.getOrPut(language) { PuzzleGenerator(dictionary) } }
    }

    /** Easy Mode has its own frozen set, built without hard and super hard words. */
    suspend fun level(language: Language, number: Int, easyMode: Boolean): Puzzle = mutex.withLock {
        levels.getOrPut(language to easyMode) {
            withContext(Dispatchers.IO) {
                val file = if (easyMode) "levels_easy.json" else "levels.json"
                Json.decodeFromString(ListSerializer(Puzzle.serializer()), asset("${language.code}/$file"))
            }
        }[number - 1]
    }

    private data class PrefetchKey(val language: Language, val difficulty: Difficulty, val easyMode: Boolean)

    private val prefetched = HashMap<PrefetchKey, Deferred<Puzzle>>()

    private val pools = HashMap<Boolean, List<Puzzle>>()

    /**
     * Russian has too few short easy words to fill a 15x15 fast on a phone, so russian hard
     * puzzles come from a big pool built ahead of time (one set per Easy Mode setting).
     */
    private fun usesPool(language: Language, difficulty: Difficulty) =
        language == Language.RU && difficulty == Difficulty.HARD

    private suspend fun pool(easyMode: Boolean): List<Puzzle> = mutex.withLock {
        pools.getOrPut(easyMode) {
            withContext(Dispatchers.IO) {
                val file = if (easyMode) "pool_hard_easy.json" else "pool_hard.json"
                Json.decodeFromString(ListSerializer(Puzzle.serializer()), asset("ru/$file"))
            }
        }
    }

    /** Next pool puzzle in a fixed shuffled order, so nothing repeats until the whole pool was played. */
    private suspend fun nextFromPool(easyMode: Boolean): Puzzle {
        val list = pool(easyMode)
        val key = if (easyMode) "pool_easy_next" else "pool_next"
        val n = poolPrefs.getInt(key, 0)
        poolPrefs.edit().putInt(key, n + 1).apply()
        val order = list.indices.toMutableList().also(dev.jhu.crossi.generator.Rng(POOL_ORDER_SEED)::shuffle)
        return list[order[n % list.size]]
    }

    private val poolPrefs by lazy { context.getSharedPreferences("crossi_pool", Context.MODE_PRIVATE) }

    /** Generates a puzzle off the main thread. Big grids can take a moment. */
    suspend fun generate(
        language: Language,
        difficulty: Difficulty,
        seed: Long,
        easyMode: Boolean,
        trickFest: Boolean = false,
    ): Puzzle {
        if (usesPool(language, difficulty)) {
            val list = pool(easyMode)
            return list[Math.floorMod(seed, list.size.toLong()).toInt()]
        }
        val generator = generator(language)
        return withContext(Dispatchers.Default) {
            // a seed can (rarely) fail to fill, so step to the next one. still the same for everyone.
            var last: Exception? = null
            for (i in 0 until SEED_TRIES) {
                try {
                    return@withContext generator.generate(
                        GenConfig(
                            size = difficulty.size,
                            seed = seed + i,
                            maxHardness = difficulty.maxHardness,
                            maxTier = if (easyMode) EASY_MODE_MAX_TIER else 5,
                            trickFest = trickFest,
                        ),
                    )
                } catch (e: dev.jhu.crossi.generator.GenerationException) {
                    last = e
                }
            }
            throw last!!
        }
    }

    /** Free roam: use the puzzle built in the background if there is one, then start building the next. */
    suspend fun freeRoamPuzzle(language: Language, difficulty: Difficulty, easyMode: Boolean): Puzzle {
        if (usesPool(language, difficulty)) return nextFromPool(easyMode)
        val key = PrefetchKey(language, difficulty, easyMode)
        val ready = synchronized(prefetched) { prefetched.remove(key) }
        val puzzle = ready?.let { runCatching { it.await() }.getOrNull() }
            ?: generate(language, difficulty, randomSeed(), easyMode)
        synchronized(prefetched) {
            prefetched[key] = scope.async { generate(language, difficulty, randomSeed(), easyMode) }
        }
        return puzzle
    }

    private fun randomSeed(): Long = java.security.SecureRandom().nextLong()

    private fun asset(path: String): String = context.assets.open(path).bufferedReader().use { it.readText() }

    companion object {
        private const val SEED_TRIES = 6
        private const val POOL_ORDER_SEED = 20261113L

        /** Bump when a words.tsv changes, so dailies stay in sync between players on the same version. */
        fun dictionaryVersion(language: Language): Int = when (language) {
            Language.EN -> 3
            Language.RU -> 3
        }
    }
}
