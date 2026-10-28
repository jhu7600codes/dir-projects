package dev.jhu.crossi.generator

import dev.jhu.crossi.game.BossTwist
import dev.jhu.crossi.game.Language
import dev.jhu.crossi.game.LevelCatalog
import dev.jhu.crossi.game.Seeds
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Not a real test: regenerates assets/<lang>/levels.json. Levels are frozen so a
 * dictionary change never changes a level someone already played.
 *
 *   CROSSI_FREEZE=1 ./gradlew :app:testDebugUnitTest --tests '*LevelFreezer*'   (or =en / =ru)
 */
class LevelFreezer {
    @Test
    fun freeze() {
        // CROSSI_FREEZE=1 does both languages, CROSSI_FREEZE=en or =ru just one
        val which = System.getenv("CROSSI_FREEZE")
        assumeTrue(which != null)
        val all = listOf(Language.EN to Fixtures.english, Language.RU to Fixtures.russian)
        for ((language, dictionary) in all.filter { which == "1" || it.first.code == which }) {
            val generator = PuzzleGenerator(dictionary)
            val levels = (1..LevelCatalog.COUNT).map { n ->
                val spec = LevelCatalog.spec(n)
                val base = Seeds.level(n, language)
                (0L until 20L).firstNotNullOf { i ->
                    runCatching {
                        generator.generate(
                            GenConfig(
                                size = spec.size,
                                seed = base + i,
                                maxHardness = spec.maxHardness,
                                trickFest = BossTwist.TRICK_FEST in spec.twists,
                            ),
                        ).copy(id = "${language.code}-level-$n")
                    }.getOrNull()
                }.also { check(PuzzleValidator.validate(it) { w -> dictionary.contains(w) }.isEmpty()) }
            }
            File("src/main/assets/${language.code}/levels.json")
                .writeText(Json.encodeToString(ListSerializer(Puzzle.serializer()), levels))
            println("froze ${levels.size} ${language.code} levels")
        }
    }
}
