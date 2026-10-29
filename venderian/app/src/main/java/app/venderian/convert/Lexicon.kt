package app.venderian.convert

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Entry(val word: String, val meaning: String, val pos: String)

/**
 * the starter dictionary (assets/dictionary.json), indexed by english meaning for translate
 * mode. knows just enough english to undo plurals, -ed, -ing, a few irregular verbs and
 * "will", and applies the venderian plural / past / future endings instead.
 */
class Lexicon(val entries: List<Entry>) {
    private val byEnglish: Map<String, Entry>
    /** longest english phrase in the index, in words */
    val longestPhrase: Int

    init {
        val index = LinkedHashMap<String, Entry>()
        for (e in entries) {
            if (e.pos == "suffix") continue
            for (part in e.meaning.split(',')) {
                val key = part.replace(Regex("\\(.*?\\)"), "").trim().lowercase().removePrefix("to ").trim()
                if (key.isNotEmpty()) index.putIfAbsent(key, e)
            }
        }
        byEnglish = index
        longestPhrase = index.keys.maxOf { it.split(' ').size }
    }

    fun entry(english: String): Entry? = byEnglish[english.lowercase()]

    /**
     * venderian for one english word (or phrase), already inflected, possibly several words
     * ("my" -> "da mi"). empty string means "drop it" (articles). null means unknown.
     */
    fun translate(english: String): String? {
        val w = english.lowercase()
        if (w in DROPPED) return ""
        SPECIAL[w]?.let { return it }
        entry(w)?.let { return it.word }
        IRREGULAR_PAST[w]?.let { base -> entry(base)?.takeIf { it.pos == "verb" }?.let { return past(it.word) } }
        IRREGULAR_PLURAL[w]?.let { base -> entry(base)?.takeIf { it.pos == "noun" }?.let { return plural(it.word) } }
        for (stem in stems(w, listOf("ies" to "y", "es" to "", "s" to ""))) {
            val e = entry(stem) ?: continue
            if (e.pos == "noun") return plural(e.word)
            if (e.pos == "verb") return e.word
        }
        for (stem in stems(w, listOf("ied" to "y", "ed" to "", "ed" to "e"))) {
            entry(stem)?.takeIf { it.pos == "verb" }?.let { return past(it.word) }
        }
        for (stem in stems(w, listOf("ing" to "", "ing" to "e"))) {
            entry(stem)?.takeIf { it.pos == "verb" }?.let { return it.word }
        }
        return null
    }

    fun isVerb(english: String): Boolean {
        val w = english.lowercase()
        return entry(w)?.pos == "verb" || w in IRREGULAR_PAST
    }

    /** english suffix rules, plus the doubled-consonant versions (stopped -> stop) */
    private fun stems(w: String, rules: List<Pair<String, String>>): List<String> = buildList {
        for ((suffix, replacement) in rules) {
            if (w.length <= suffix.length + 1 || !w.endsWith(suffix)) continue
            val stem = w.dropLast(suffix.length)
            add(stem + replacement)
            if (replacement.isEmpty() && stem.length > 2 && stem.last() == stem[stem.length - 2]) add(stem.dropLast(1))
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String) = Lexicon(json.decodeFromString<List<Entry>>(text))

        /** venderian plural: -n after a vowel, -an after a consonant */
        fun plural(word: String): String {
            val last = Sounds.parseRoman(word).last()
            return word + if (Sounds.isVowel(last)) "n" else "an"
        }

        fun past(word: String) = word + "ta"
        fun future(word: String) = word + "va"

        private val DROPPED = setOf("a", "an", "the", "will", "shall", "do", "does", "did")
        private val SPECIAL = mapOf(
            "am" to "es", "is" to "es", "are" to "es", "be" to "es", "was" to "esta", "were" to "esta",
            "my" to "da mi", "your" to "da tu", "his" to "da sa", "her" to "da sa", "its" to "da sa",
            "our" to "da min", "their" to "da san", "him" to "sa", "us" to "min", "them" to "san",
            "don't" to "ne", "doesn't" to "ne", "didn't" to "ne", "isn't" to "ne es", "aren't" to "ne es",
            "can't" to "ne", "won't" to "ne", "hi" to "vela", "bye" to "dano", "thanks" to "shevan",
        )
        private val IRREGULAR_PAST = mapOf(
            "ate" to "eat", "went" to "go", "saw" to "see", "came" to "come", "had" to "have",
            "gave" to "give", "took" to "take", "made" to "make", "knew" to "know", "spoke" to "speak",
            "drank" to "drink", "slept" to "sleep", "ran" to "run", "sat" to "sit", "stood" to "stand",
            "found" to "find", "thought" to "think", "felt" to "feel", "wrote" to "write",
            "taught" to "teach", "bought" to "buy", "sang" to "sing", "heard" to "hear", "did" to "do",
        )
        private val IRREGULAR_PLURAL = mapOf(
            "people" to "person", "children" to "child", "men" to "man", "women" to "woman",
            "leaves" to "leaf", "feet" to "foot", "teeth" to "tooth",
        )
    }
}
