package app.venderian.data

import app.venderian.convert.Entry
import app.venderian.srs.Grade
import app.venderian.srs.Srs
import app.venderian.srs.Streaks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

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
) {
    /** 100 xp a level, each level a little longer than the last */
    val level: Int get() = generateSequence(1) { it + 1 }.first { xpFor(it + 1) > totalXp }
    val levelProgress: Float get() = (totalXp - xpFor(level)).toFloat() / (xpFor(level + 1) - xpFor(level))

    companion object {
        fun xpFor(level: Int) = (level - 1) * 100 + (level - 1) * (level - 2) * 10
    }
}

class Repository(private val db: VenderianDb, val alphabet: Alphabet, val words: List<Entry>) {

    /** words that can be flashcards / quiz questions (not suffixes) */
    val cardWords: List<Entry> = words.filter { it.pos != "suffix" }
    private val wordsByRoman = cardWords.associateBy { it.word }

    fun word(roman: String) = wordsByRoman[roman]

    val lessonsDone: Flow<Set<Int>> = db.progress().lessons().map { list -> list.map { it.lesson }.toSet() }

    fun dueCount(now: Long = System.currentTimeMillis()) = db.cards().dueCount(now)

    val progress: Flow<Progress> = combine(db.progress().days(), db.cards().all(), lessonsDone) { days, cards, lessons ->
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

    /**
     * the cards to practice now: everything due, plus up to [NEW_WORDS_PER_DAY] new words a day,
     * taken from the dictionary in order (it starts with the most useful words).
     */
    suspend fun practiceDeck(limit: Int = 20): List<CardEntity> {
        val now = System.currentTimeMillis()
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val newToday = db.cards().introducedSince(CardEntity.WORD, startOfDay)
        val room = (NEW_WORDS_PER_DAY - newToday).coerceAtLeast(0)
        if (room > 0) {
            val known = db.cards().ids().toSet()
            val fresh = cardWords.asSequence()
                .map { "${CardEntity.WORD}:${it.word}" }
                .filter { it !in known }
                .take(room)
                .map { CardEntity(it, CardEntity.WORD, dueAt = now, introducedAt = now) }
                .toList()
            db.cards().insertAll(fresh)
        }
        return db.cards().due(now, limit)
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
        const val NEW_WORDS_PER_DAY = 8

        fun today() = LocalDate.now().toEpochDay()
    }
}
