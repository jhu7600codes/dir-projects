package app.venderian.course

import app.venderian.convert.Entry
import kotlin.random.Random

sealed interface Exercise {
    /** a new word: just look at it */
    data class NewWord(val word: Entry) : Exercise

    /** venderian word -> pick the meaning */
    data class PickMeaning(val word: Entry, val options: List<String>, val answer: Int) : Exercise

    /** meaning -> pick the venderian word (options are romanized words, shown in script) */
    data class PickWord(val word: Entry, val options: List<String>, val answer: Int) : Exercise

    /** tap venderian words and their meanings in pairs */
    data class Match(val pairs: List<Pair<String, String>>) : Exercise

    /** read a venderian sentence, build the english from tiles */
    data class BuildEnglish(val venderian: String, val english: String, val tiles: List<String>, val answer: List<String>) : Exercise

    /** read an english sentence, build the venderian from tiles */
    data class BuildVenderian(val english: String, val venderian: String, val tiles: List<String>, val answer: List<String>) : Exercise

    /** read the script, type it in roman letters */
    data class TypeRoman(val venderian: String, val meaning: String?) : Exercise
}

/** makes the exercises for a lesson from its words and sentences */
class ExerciseMaker(
    private val course: Course,
    dictionary: List<Entry>,
    private val random: Random = Random.Default,
) {
    private val entries = dictionary.associateBy { it.word }

    fun make(lesson: CourseLesson): List<Exercise> {
        val sources = course.sources(lesson)
        val known = course.wordsUpTo(lesson.id).mapNotNull(entries::get).filter { it.quizzable }.distinctBy { it.word }
        return when (lesson.kind) {
            Kind.LESSON -> lessonExercises(lesson, known)
            Kind.CHALLENGE -> review(sources, known, builds = 6, types = 3, picks = 2, matches = 1)
            else -> review(sources, known, builds = 9, types = 4, picks = 3, matches = 2)
        }
    }

    /** a practice lesson: the words and sentences of [sources], from anywhere in the course */
    fun practice(sources: List<CourseLesson>): List<Exercise> {
        val known = course.lessons.flatMap { it.words }.mapNotNull(entries::get).filter { it.quizzable }.distinctBy { it.word }
        return review(sources, known, builds = 8, types = 4, picks = 3, matches = 1)
    }

    private fun lessonExercises(lesson: CourseLesson, known: List<Entry>): List<Exercise> {
        val newWords = lesson.words.mapNotNull(entries::get)
        val quizzable = newWords.filter { it.quizzable }
        val out = mutableListOf<Exercise>()
        // meet each word, then answer for it right away
        for (w in newWords) {
            out += Exercise.NewWord(w)
            if (w.quizzable) out += pickMeaning(w, known)
        }
        if (known.size >= 3) out += match(quizzable, known, 5)
        val rest = mutableListOf<Exercise>()
        lesson.sentences.forEachIndexed { i, s -> rest += if (i % 2 == 0) buildEnglish(s, known) else buildVenderian(s, known) }
        quizzable.shuffled(random).take(2).forEach { rest += pickWord(it, known) }
        // review lessons teach few or no words: practice recent ones instead
        if (quizzable.size < 3) {
            known.takeLast(20).shuffled(random).take(3).forEachIndexed { i, w ->
                rest += if (i == 0) pickWord(w, known) else pickMeaning(w, known)
            }
        }
        lesson.sentences.minByOrNull { it.v.length }?.let { rest += Exercise.TypeRoman(it.v, it.e) }
        return out + rest.shuffled(random)
    }

    private fun review(sources: List<CourseLesson>, known: List<Entry>, builds: Int, types: Int, picks: Int, matches: Int): List<Exercise> {
        val sentences = sources.flatMap { it.sentences }.shuffled(random)
        val words = sources.flatMap { it.words }.mapNotNull(entries::get).filter { it.quizzable }.distinctBy { it.word }.shuffled(random)
        val out = mutableListOf<Exercise>()
        sentences.take(builds).forEachIndexed { i, s -> out += if (i % 2 == 0) buildVenderian(s, known) else buildEnglish(s, known) }
        // typing whole sentences is the hard part: half of the typing is sentences
        sentences.drop(builds).take(types / 2).forEach { out += Exercise.TypeRoman(it.v, it.e) }
        words.take(types - types / 2).forEach { out += Exercise.TypeRoman(it.word, it.meaning) }
        words.drop(types).take(picks).forEach { out += if (random.nextBoolean()) pickWord(it, known) else pickMeaning(it, known) }
        repeat(matches) { n -> out += match(words.drop(n * 5).take(5), known, 5) }
        return out.shuffled(random)
    }

    private fun pickMeaning(w: Entry, known: List<Entry>): Exercise {
        val options = (others(w, known).map { it.meaning }.distinct().filter { it != w.meaning }.shuffled(random).take(3) + w.meaning).shuffled(random)
        return Exercise.PickMeaning(w, options, options.indexOf(w.meaning))
    }

    private fun pickWord(w: Entry, known: List<Entry>): Exercise {
        val options = (others(w, known).map { it.word }.shuffled(random).take(3) + w.word).shuffled(random)
        return Exercise.PickWord(w, options, options.indexOf(w.word))
    }

    /** distractors: other known words, the same part of speech first */
    private fun others(w: Entry, known: List<Entry>): List<Entry> {
        val pool = known.filter { it.word != w.word }.ifEmpty { entries.values.filter { it.quizzable && it.word != w.word } }
        val same = pool.filter { it.pos == w.pos }
        return if (same.size >= 3) same else pool
    }

    private fun match(first: List<Entry>, known: List<Entry>, size: Int): Exercise {
        val picked = (first + known.shuffled(random)).distinctBy { it.word }.distinctBy { it.meaning }.take(size)
        return Exercise.Match(picked.map { it.word to it.meaning })
    }

    private fun buildEnglish(s: Sentence, known: List<Entry>): Exercise {
        val answer = tokens(s.e)
        val extra = course.lessons.flatMap { it.sentences }.flatMap { tokens(it.e) }.distinct()
            .filter { it !in answer }.shuffled(random).take(3)
        return Exercise.BuildEnglish(s.v, s.e, (answer + extra).shuffled(random), answer)
    }

    private fun buildVenderian(s: Sentence, known: List<Entry>): Exercise {
        val answer = tokens(s.v)
        val extra = known.map { it.word }.filter { it !in answer }.shuffled(random).take(3)
        return Exercise.BuildVenderian(s.e, s.v, (answer + extra).shuffled(random), answer)
    }

    companion object {
        /** words of a sentence, lowercase, no punctuation */
        fun tokens(sentence: String): List<String> =
            sentence.lowercase().replace(Regex("[.,!?]"), " ").split(' ').filter { it.isNotBlank() }

        /** forgiving compare for typed answers: case, punctuation, spaces, and ë typed as e: or ё */
        fun sameText(a: String, b: String): Boolean = norm(a) == norm(b)

        private fun norm(s: String) = tokens(s.replace("e:", "ë").replace('ё', 'ë').replace("’", "'")).joinToString(" ")
    }
}

/** suffixes and names are taught, but not quizzed as words */
val Entry.quizzable get() = pos != "suffix" && pos != "name"
