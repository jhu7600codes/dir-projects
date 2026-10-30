package app.venderian.data

import app.venderian.convert.Entry
import app.venderian.course.Course
import app.venderian.course.CourseLesson
import app.venderian.course.Kind
import app.venderian.srs.Grade
import app.venderian.srs.Srs
import app.venderian.srs.Streaks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class Progress(
    val totalXp: Int = 0,
    val todayXp: Int = 0,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    /** xp for the last 7 days, oldest first */
    val week: List<Int> = List(7) { 0 },
    val lettersLearned: Int = 0,
    val wordsSeen: Int = 0,
    /** cards whose interval has grown to 3 weeks or more */
    val mastered: Int = 0,
    val lessonsDone: Set<Int> = emptySet(),
    val courseDone: Int = 0,
    val golds: Int = 0,
) {
    /** 100 xp a level, each level a little longer than the last */
    val level: Int get() = generateSequence(1) { it + 1 }.first { xpFor(it + 1) > totalXp }
    val levelProgress: Float get() = (totalXp - xpFor(level)).toFloat() / (xpFor(level + 1) - xpFor(level))

    companion object {
        fun xpFor(level: Int) = (level - 1) * 100 + (level - 1) * (level - 2) * 10
    }
}

class Repository(private val db: VenderianDb, val alphabet: Alphabet, val words: List<Entry>, val course: Course) {

    /** words that can be flashcards / quiz questions (not suffixes) */
    val cardWords: List<Entry> = words.filter { it.pos != "suffix" }
    private val wordsByRoman = cardWords.associateBy { it.word }

    fun word(roman: String) = wordsByRoman[roman]

    val lessonsDone: Flow<Set<Int>> = db.progress().lessons().map { list -> list.map { it.lesson }.toSet() }

    /** ids of finished course lessons */
    val courseDone: Flow<Set<String>> = db.progress().course().map { list -> list.map { it.id }.toSet() }

    /** the course needs the whole script first */
    val scriptDone: Flow<Boolean> = lessonsDone.map { done -> alphabet.lessons.all { it.index in done } }

    fun dueCount(now: Long = System.currentTimeMillis()) = db.cards().dueCount(now)

    val progress: Flow<Progress> = combine(db.progress().days(), db.cards().all(), lessonsDone, courseDone) { days, cards, lessons, course ->
        val today = today()
        val active = days.filter { it.xp > 0 }.map { it.epochDay }.toSet()
        val byDay = days.associate { it.epochDay to it.xp }
        Progress(
            totalXp = days.sumOf { it.xp },
            todayXp = byDay[today] ?: 0,
            streak = Streaks.current(active, today),
            longestStreak = Streaks.longest(active),
            week = (6 downTo 0).map { byDay[today - it] ?: 0 },
            lettersLearned = cards.count { it.kind == CardEntity.GLYPH },
            wordsSeen = cards.count { it.kind == CardEntity.WORD },
            mastered = cards.count { it.intervalDays >= 21 },
            lessonsDone = lessons,
            courseDone = course.count { it.startsWith("m") },
            golds = course.count { it.endsWith("-gold") },
        )
    }

    suspend fun addXp(xp: Int) {
        val day = today()
        db.progress().ensureDay(day)
        db.progress().addToDay(day, xp)
    }

    suspend fun completeLesson(lesson: Lesson) {
        val now = System.currentTimeMillis()
        db.progress().completeLesson(LessonEntity(lesson.index, now))
        db.cards().insertAll(lesson.ids.map { CardEntity("${CardEntity.GLYPH}:$it", CardEntity.GLYPH, dueAt = now, introducedAt = now) })
        addXp(XP_LESSON)
    }

    /** the cards to practice now: everything that's due. words get here by finishing course lessons. */
    suspend fun practiceDeck(limit: Int = 20): List<CardEntity> = db.cards().due(System.currentTimeMillis(), limit)

    /** words in the practice deck */
    val deckWords: Flow<Set<String>> = db.cards().all().map { cards ->
        cards.filter { it.kind == CardEntity.WORD }.map { it.key }.toSet()
    }

    /** puts a word in the practice deck (from the dictionary) */
    suspend fun addWordCard(word: String) {
        val now = System.currentTimeMillis()
        db.cards().insertAll(listOf(CardEntity("${CardEntity.WORD}:$word", CardEntity.WORD, dueAt = now, introducedAt = now)))
    }

    /** the first lesson that teaches a word */
    fun lessonFor(word: String): Pair<String, CourseLesson>? {
        for (section in course.sections) for (module in section.modules) for (lesson in module.lessons) {
            if (word in lesson.words) return "section ${section.id} · module ${module.id} ${module.title}" to lesson
        }
        return null
    }

    /** words from finished course lessons, for the quiz */
    suspend fun learnedWords(): List<Entry> =
        db.cards().ids().filter { it.startsWith("${CardEntity.WORD}:") }.mapNotNull { word(it.substringAfter(':')) }

    /**
     * the lessons a practice lesson mixes: [count] finished lessons from the whole course (or one [section]),
     * picked at random but leaning towards ones you made mistakes in or haven't seen in a while.
     */
    suspend fun practiceSources(
        section: Int? = null,
        count: Int = 4,
        random: kotlin.random.Random = kotlin.random.Random.Default,
    ): List<CourseLesson> {
        val now = System.currentTimeMillis()
        val inSection = section?.let { n ->
            course.sections.first { it.id == n }.modules.flatMap { m -> m.lessons.map { it.id } }.toSet()
        }
        val done = db.progress().courseNow().filter { it.id.contains("-l") && (inSection == null || it.id in inSection) }
        val pool = done.map { e ->
            val weeks = (now - e.completedAt) / (7 * Srs.DAY_MS).toDouble()
            e to 1.0 + 2.0 * e.mistakes + weeks
        }.toMutableList()
        val picked = mutableListOf<CourseLesson>()
        while (picked.size < count && pool.isNotEmpty()) {
            var r = random.nextDouble() * pool.sumOf { it.second }
            val i = pool.indexOfFirst { r -= it.second; r <= 0 }.let { if (it < 0) pool.lastIndex else it }
            picked += course.lesson(pool.removeAt(i).first.id)
        }
        return picked
    }

    /** a practice lesson is done: xp, and its lessons count as freshly reviewed */
    suspend fun completePractice(sources: List<CourseLesson>, mistakes: Int) {
        val now = System.currentTimeMillis()
        val current = db.progress().courseNow().associateBy { it.id }
        for (l in sources) {
            // a clean review forgets old mistakes, a messy one keeps the lesson coming back
            val before = current[l.id]?.mistakes ?: 0
            db.progress().completeCourse(CourseEntity(l.id, now, if (mistakes == 0) 0 else before))
        }
        addXp(XP_PRACTICE)
    }

    /** an english sounds lesson is done (stored with the course, by its snd- id) */
    suspend fun completeSoundLesson(id: String, mistakes: Int) {
        db.progress().completeCourse(CourseEntity(id, System.currentTimeMillis(), mistakes))
        addXp(XP_COURSE_LESSON)
    }

    suspend fun completeCourseLesson(lesson: CourseLesson, mistakes: Int) {
        val now = System.currentTimeMillis()
        db.progress().completeCourse(CourseEntity(lesson.id, now, mistakes))
        val newWords = lesson.words.filter { word(it) != null }
        db.cards().insertAll(newWords.map { CardEntity("${CardEntity.WORD}:$it", CardEntity.WORD, dueAt = now, introducedAt = now) })
        addXp(when (lesson.kind) {
            Kind.CHALLENGE -> XP_CHALLENGE
            Kind.GOLD -> XP_GOLD
            else -> XP_COURSE_LESSON
        })
    }

    suspend fun review(card: CardEntity, grade: Grade): CardEntity {
        val now = System.currentTimeMillis()
        val s = Srs.review(card.srs, grade)
        val updated = card.copy(
            ease = s.ease, intervalDays = s.intervalDays, reps = s.reps, lapses = s.lapses, dueAt = Srs.dueAt(now, s),
        )
        db.cards().update(updated)
        addXp(if (grade == Grade.AGAIN) XP_REVIEW_MISS else XP_REVIEW)
        return updated
    }

    companion object {
        const val XP_LESSON = 20
        const val XP_REVIEW = 2
        const val XP_REVIEW_MISS = 1
        const val XP_QUIZ_RIGHT = 5
        const val XP_COURSE_LESSON = 15
        const val XP_CHALLENGE = 30
        const val XP_GOLD = 50
        const val XP_PRACTICE = 25

        fun today() = LocalDate.now().toEpochDay()
    }
}
