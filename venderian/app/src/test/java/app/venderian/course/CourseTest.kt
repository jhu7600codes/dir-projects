package app.venderian.course

import app.venderian.convert.Lexicon
import app.venderian.convert.Sounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class CourseTest {
    private val course = Course.parse(File("src/main/assets/course.json").readText())
    private val dictionary = Lexicon.parse(File("src/main/assets/dictionary.json").readText()).entries

    @Test
    fun `5 sections, 3 modules each, 20 lessons + challenge + gold per module`() {
        assertEquals(5, course.sections.size)
        for (section in course.sections) {
            assertEquals(6, section.modules.size) // N and N.5
            for ((full, half) in section.modules.chunked(2).map { it[0] to it[1] }) {
                assertEquals(List(10) { Kind.LESSON } + Kind.CHALLENGE, full.lessons.map { it.kind })
                assertEquals(List(10) { Kind.LESSON } + Kind.GOLD, half.lessons.map { it.kind })
                assertTrue(half.id == full.id + ".5")
            }
        }
        assertEquals(300, course.lessons.count { it.kind == Kind.LESSON })
    }

    @Test
    fun `every sentence can be written in the script`() {
        for (l in course.lessons) for (s in l.sentences) {
            val script = Sounds.scriptOfText(s.v)
            assertTrue(s.v, script.none { it in 'a'..'z' })
        }
    }

    @Test
    fun `every lesson makes sensible exercises`() {
        val maker = ExerciseMaker(course, dictionary, Random(1))
        for (lesson in course.lessons) {
            val exercises = maker.make(lesson)
            assertTrue("${lesson.id} has too few exercises", exercises.size >= 8)
            for (e in exercises) when (e) {
                is Exercise.PickMeaning -> assertEquals(e.word.meaning, e.options[e.answer])
                is Exercise.PickWord -> assertEquals(e.word.word, e.options[e.answer])
                is Exercise.BuildEnglish -> assertTrue(e.tiles.containsAll(e.answer))
                is Exercise.BuildVenderian -> assertTrue(e.tiles.containsAll(e.answer))
                is Exercise.Match -> assertTrue("${lesson.id} match", e.pairs.size >= 3 && e.pairs.map { it.second }.distinct().size == e.pairs.size)
                is Exercise.TypeRoman -> assertTrue(ExerciseMaker.sameText(e.venderian, e.venderian))
                is Exercise.NewWord, is Exercise.SpellLetters -> Unit
            }
            if (lesson.kind == Kind.LESSON) {
                val introduced = exercises.filterIsInstance<Exercise.NewWord>().map { it.word.word }
                assertEquals(lesson.id, lesson.words, introduced)
            }
        }
    }

    @Test
    fun `typed answers are forgiving about case, punctuation and how ë is typed`() {
        assertTrue(ExerciseMaker.sameText("Mi Bren Vola!", "mi bren vola."))
        assertTrue(ExerciseMaker.sameText("kite:n", "kitën"))
        assertTrue(ExerciseMaker.sameText("kitёn", "kitën"))
        assertTrue(!ExerciseMaker.sameText("kiten", "kitën"))
    }

    @Test
    fun `practice lessons mix lessons from all over the course`() {
        val maker = ExerciseMaker(course, dictionary, Random(7))
        val sources = listOf("m1-l2", "m4.5-l7", "m9-l3", "m7-l9").map(course::lesson)
        val exercises = maker.practice(sources)
        assertTrue(exercises.size >= 12)
        assertTrue(exercises.none { it is Exercise.NewWord })
        val sentences = sources.flatMap { it.sentences }.map { it.v }
        val used = exercises.filterIsInstance<Exercise.BuildEnglish>().map { it.venderian } +
            exercises.filterIsInstance<Exercise.BuildVenderian>().map { it.venderian }
        assertTrue(used.isNotEmpty() && used.all { it in sentences })
        assertTrue(!CourseLesson.practice.hasHearts)
    }

    @Test
    fun `sound lessons spell english words by sound`() {
        val sounds = SoundLessons.parse(File("src/main/assets/sounds.json").readText())
        val maker = SoundExerciseMaker(sounds, Random(2))
        assertTrue(sounds.lessons.size >= 15)
        for (lesson in sounds.lessons) {
            val exercises = maker.make(lesson)
            assertTrue(exercises.size >= 10)
            for (e in exercises) when (e) {
                is Exercise.SpellLetters -> {
                    assertEquals(Sounds.toRoman(e.answer), e.roman)
                    assertTrue(e.tiles.containsAll(e.answer))
                }
                is Exercise.PickWord -> { assertEquals(e.word.word, e.options[e.answer]); assertEquals(4, e.options.distinct().size) }
                is Exercise.PickMeaning -> assertEquals(e.word.meaning, e.options[e.answer])
                else -> Unit
            }
        }
        val pizza = sounds.lessons.flatMap { it.words }.first { it.en == "pizza" }
        assertEquals("pitsë", pizza.roman)
    }
}
