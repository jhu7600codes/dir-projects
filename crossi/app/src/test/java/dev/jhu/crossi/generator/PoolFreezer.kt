package dev.jhu.crossi.generator

import dev.jhu.crossi.game.Difficulty
import dev.jhu.crossi.game.EASY_MODE_MAX_TIER
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Not a real test: pre-builds the russian 15x15 pool. Russian has too few short easy words
 * to fill a 15x15 quickly on a phone, so hard russian free roam and dailies pick from here.
 *
 *   CROSSI_POOL=150 ./gradlew :app:testDebugUnitTest --tests '*PoolFreezer*'
 */
class PoolFreezer {
    @Test
    fun freeze() {
        val count = System.getenv("CROSSI_POOL")?.toIntOrNull()
        assumeTrue(count != null)
        val dictionary = Fixtures.russian
        for ((file, maxTier) in listOf("pool_hard.json" to 5, "pool_hard_easy.json" to EASY_MODE_MAX_TIER)) {
            val puzzles = java.util.Collections.synchronizedList(ArrayList<Puzzle>())
            val nextSeed = AtomicLong(Rng.seedOf("crossi-pool-$file"))
            val threads = Runtime.getRuntime().availableProcessors()
            val pool = Executors.newFixedThreadPool(threads)
            repeat(threads) {
                pool.submit {
                    val generator = PuzzleGenerator(dictionary)
                    while (puzzles.size < count!!) {
                        val seed = nextSeed.getAndIncrement()
                        runCatching {
                            generator.generate(
                                GenConfig(Difficulty.HARD.size, seed, maxHardness = Difficulty.HARD.maxHardness, maxTier = maxTier),
                            )
                        }.getOrNull()?.let { p ->
                            check(PuzzleValidator.validate(p) { w -> dictionary.contains(w) }.isEmpty())
                            puzzles += p
                        }
                    }
                }
            }
            pool.shutdown()
            pool.awaitTermination(6, TimeUnit.HOURS)
            val list = puzzles.take(count!!).sortedBy { it.id }
            File("src/main/assets/ru/$file").writeText(Json.encodeToString(ListSerializer(Puzzle.serializer()), list))
            println("pooled ${list.size} into $file")
        }
    }
}
