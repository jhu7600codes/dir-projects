package dev.jhu.crossi.generator

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrickTest {
    @Test
    fun `capital letter template only capitalizes the answer letter`() {
        val tricks = TrickSet.EN
        val rng = Rng(1)
        for (answer in tricks.answers) {
            val letter = tricks.letterFor(answer)!!
            repeat(20) {
                for (clue in tricks.allCluesFor(answer, rng)) {
                    val caps = clue.filter { it.isUpperCase() }.toSet()
                    // "the letter right after K" style clues name another letter on purpose
                    if (caps.size == 1 && "right" !in clue) assertEquals(setOf(letter), caps, clue)
                    if ("missing from" in clue) assertTrue('_' in clue && letter.lowercaseChar() !in clue.substringAfter("'"), clue)
                }
            }
        }
    }

    @Test
    fun `every trick answer has a clue and fits a grid`() {
        for (set in listOf(TrickSet.EN, TrickSet.RU)) for (answer in set.answers) {
            assertTrue(answer.length >= 3, answer)
            assertTrue(set.clueFor(answer, Rng(2)) != null, answer)
        }
    }
}
