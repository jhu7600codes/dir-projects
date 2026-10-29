package dev.jhu.crossi.data

import android.content.Context
import dev.jhu.crossi.game.Difficulty
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
    private val levels = HashMap<Language, List<Puzzle>>()

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

    suspend fun level(language: Language, number: Int): Puzzle = mutex.withLock {
        levels.getOrPut(language) {
            withContext(Dispatchers.IO) {
                Json.decodeFromString(ListSerializer(Puzzle.serializer()), asset("${language.code}/levels.json"))
            }
        }[number - 1]
    }

    private val prefetched = HashMap<Pair<Language, Difficulty>, Deferred<Puzzle>>()

    /** Generates a puzzle off the main thread. Big grids can take a moment. */
    suspend fun generate(language: Language, difficulty: Difficulty, seed: Long, trickFest: Boolean = false): Puzzle {
        val generator = generator(language)
        return withContext(Dispatchers.Default) {
            // a seed can (rarely) fail to fill, so step to the next one. still the same for everyone.
            var last: Exception? = null
            for (i in 0 until SEED_TRIES) {
                try {
                    return@withContext generator.generate(
                        GenConfig(size = difficulty.size, seed = seed + i, maxHardness = difficulty.maxHardness, trickFest = trickFest),
                    )
                } catch (e: dev.jhu.crossi.generator.GenerationException) {
                    last = e
                }
            }
            throw last!!
        }
    }

    /** Free roam: use the puzzle built in the background if there is one, then start building the next. */
    suspend fun freeRoamPuzzle(language: Language, difficulty: Difficulty): Puzzle {
        val key = language to difficulty
        val ready = synchronized(prefetched) { prefetched.remove(key) }
        val puzzle = ready?.let { runCatching { it.await() }.getOrNull() }
            ?: generate(language, difficulty, randomSeed())
        synchronized(prefetched) {
            prefetched[key] = scope.async { generate(language, difficulty, randomSeed()) }
        }
        return puzzle
    }

    private fun randomSeed(): Long = java.security.SecureRandom().nextLong()

    private fun asset(path: String): String = context.assets.open(path).bufferedReader().use { it.readText() }

    companion object {
        private const val SEED_TRIES = 6

        /** Bump when a words.tsv changes, so dailies stay in sync between players on the same version. */
        fun dictionaryVersion(language: Language): Int = when (language) {
            Language.EN -> 2
            Language.RU -> 2
        }
    }
}
