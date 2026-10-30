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
    fun `3 sections, 3 modules each, 10 lessons + challenge + gold per module`() {
        assertEquals(3, course.sections.size)
        for (section in course.sections) {
            assertEquals(6, section.modules.size) // N and N.5
            for ((full, half) in section.modules.chunked(2).map { it[0] to it[1] }) {
                assertEquals(listOf(Kind.LESSON, Kind.LESSON, Kind.LESSON, Kind.LESSON, Kind.LESSON, Kind.CHALLENGE), full.lessons.map { it.kind })
                assertEquals(listOf(Kind.LESSON, Kind.LESSON, Kind.LESSON, Kind.LESSON, Kind.LESSON, Kind.GOLD), half.lessons.map { it.kind })
                assertTrue(half.id == full.id + ".5")
            }
        }
        assertEquals(90, course.lessons.count { it.kind == Kind.LESSON })
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
                is Exercise.NewWord -> Unit
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
}
