package app.venderian.course

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** assets/course.json, made by tools/make_course.py from course/s*.txt */
@Serializable
data class Course(val sections: List<Section>) {
    /** every lesson in order, the order they unlock in */
    val lessons: List<CourseLesson> by lazy { sections.flatMap { s -> s.modules.flatMap { it.lessons } } }
    private val byId by lazy { lessons.associateBy { it.id } }

    fun lesson(id: String) = byId.getValue(id)

    fun moduleOf(id: String): Module = sections.flatMap { it.modules }.first { m -> m.lessons.any { it.id == id } }

    /** the lessons a challenge / gold lesson reviews, or just the lesson itself */
    fun sources(lesson: CourseLesson): List<CourseLesson> =
        if (lesson.kind == Kind.LESSON) listOf(lesson) else lesson.from.map(::lesson)

    /** every word taught up to and including [id] */
    fun wordsUpTo(id: String): List<String> =
        lessons.takeWhile { it.id != id }.flatMap { it.words } + lesson(id).words

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String) = json.decodeFromString<Course>(text)
    }
}

@Serializable
data class Section(val id: Int, val title: String, val subtitle: String, val modules: List<Module>)

@Serializable
data class Module(val id: String, val title: String, val half: Boolean, val lessons: List<CourseLesson>)

@Serializable
data class Sentence(val v: String, val e: String)

object Kind {
    const val LESSON = "lesson"
    const val CHALLENGE = "challenge"
    const val GOLD = "gold"
    /** made up on the spot once the course is done: a mix of earlier lessons */
    const val PRACTICE = "practice"
    /** an english sounds lesson (assets/sounds.json) */
    const val SOUNDS = "sounds"
}

@Serializable
data class CourseLesson(
    val id: String,
    val kind: String,
    val title: String,
    val tip: String = "",
    val words: List<String> = emptyList(),
    val sentences: List<Sentence> = emptyList(),
    val from: List<String> = emptyList(),
) {
    /** challenges and gold lessons have hearts: three mistakes and you start over */
    val hasHearts get() = kind == Kind.CHALLENGE || kind == Kind.GOLD

    /** the section a section practice lesson is about, null for the whole course */
    val practiceSection: Int? get() = id.substringAfter("$PRACTICE_ID-s", "").toIntOrNull()

    companion object {
        const val PRACTICE_ID = "practice"
        val practice = CourseLesson(PRACTICE_ID, Kind.PRACTICE, "practice lesson")

        /** "practice" for the whole course, "practice-s2" for section 2 */
        fun practice(id: String) = CourseLesson(id, Kind.PRACTICE, "practice lesson")
        fun sectionPracticeId(section: Int) = "$PRACTICE_ID-s$section"
    }
}
