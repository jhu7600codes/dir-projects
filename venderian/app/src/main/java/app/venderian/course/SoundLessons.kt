package app.venderian.course

import app.venderian.convert.Entry
import app.venderian.convert.Sounds
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/** assets/sounds.json, made by tools/make_sounds.py: english words spelled by sound */
@Serializable
data class SoundLessons(val lessons: List<SoundLesson>) {
    fun lesson(id: String) = lessons.first { it.id == id }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String) = json.decodeFromString<SoundLessons>(text)
        const val PREFIX = "snd-"
    }
}

@Serializable
data class SoundWord(val en: String, val roman: String)

@Serializable
data class SoundLesson(val id: String, val title: String, val tip: String, val words: List<SoundWord>) {
    /** so the lesson player can show it like any other lesson */
    val asLesson get() = CourseLesson(id, Kind.SOUNDS, title, tip)
}

/** exercises for a sound lesson: spell english words with letter tiles, read them back */
class SoundExerciseMaker(private val all: SoundLessons, private val random: Random = Random.Default) {
    fun make(lesson: SoundLesson): List<Exercise> {
        val pool = all.lessons.flatMap { it.words }
        val out = mutableListOf<Exercise>()
        for ((i, w) in lesson.words.withIndex()) {
            val entry = Entry(w.roman, w.en, "english word")
            out += when (i % 3) {
                // english word -> build it from letters
                0 -> spell(w)
                // script -> which english word is it?
                1 -> {
                    val options = (others(w, pool).map { it.en }.take(3) + w.en).shuffled(random)
                    Exercise.PickMeaning(entry, options, options.indexOf(w.en))
                }
                // english word -> which spelling is right?
                else -> {
                    val options = (lookalikes(w).take(3) + w.roman).shuffled(random)
                    Exercise.PickWord(entry, options, options.indexOf(w.roman))
                }
            }
        }
        // and every word spelled once more, mixed up
        lesson.words.shuffled(random).take(4).forEach { out += spell(it) }
        return out
    }

    private fun spell(w: SoundWord): Exercise {
        val answer = Sounds.parseRoman(w.roman)
        val extra = Sounds.all.filter { it !in answer }.shuffled(random).take(3)
        return Exercise.SpellLetters(w.en, w.roman, (answer + extra).shuffled(random), answer)
    }

    private fun others(w: SoundWord, pool: List<SoundWord>) = pool.filter { it.en != w.en }.shuffled(random)

    /** wrong spellings that look like what english spelling would suggest */
    private fun lookalikes(w: SoundWord): List<String> {
        val sounds = Sounds.parseRoman(w.roman)
        val swaps = mapOf("ë" to listOf("a", "e"), "a" to listOf("o", "e"), "i" to listOf("e", "ë"), "u" to listOf("o"),
            "o" to listOf("u", "a"), "e" to listOf("i", "a"), "k" to listOf("g"), "s" to listOf("z"), "ch" to listOf("sh"), "th" to listOf("t"))
        val out = linkedSetOf<String>()
        for (n in 0 until 40) {
            val i = random.nextInt(sounds.size)
            val options = swaps[sounds[i]] ?: continue
            val changed = sounds.toMutableList().also { it[i] = options.random(random) }
            val roman = Sounds.toRoman(changed)
            if (roman != w.roman) out += roman
            if (out.size >= 3) break
        }
        // short words might not have three: drop a letter or double one
        while (out.size < 3) out += Sounds.toRoman(sounds + sounds.last()).let { if (it in out) it + "ë" else it }
        return out.toList()
    }
}
