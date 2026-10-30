package app.venderian.convert

/**
 * finds venderian words: what a word in a sentence is (taking endings off: "golta" is gol +
 * -ta), and dictionary search by english, romanization or script.
 */
class WordLookup(entries: List<Entry>) {
    data class Hit(val entry: Entry, val endings: List<Entry>)

    private val byWord = entries.filter { it.pos != "suffix" }.associateBy { it.word }
    private val suffixes = entries.filter { it.pos == "suffix" }.associateBy { it.word.removePrefix("-") }
    private val all = entries

    /** a word as it appears in a sentence, or null if it isn't venderian */
    fun analyze(token: String): Hit? {
        val word = token.lowercase().trim { !it.isLetter() && it != 'ë' }
        if (word.isEmpty()) return null
        return analyze(word, 0)
    }

    private fun analyze(word: String, depth: Int): Hit? {
        byWord[word]?.let { return Hit(it, emptyList()) }
        if (depth >= 3) return null
        for ((ending, entry) in suffixes.entries.sortedByDescending { it.key.length }) {
            if (!word.endsWith(ending) || word.length <= ending.length) continue
            val stem = word.dropLast(ending.length)
            if (!fits(ending, stem)) continue
            val hit = analyze(stem, depth + 1) ?: continue
            return hit.copy(endings = hit.endings + entry)
        }
        return null
    }

    /** -n / -r / -sh go after vowels, -an / -or / -esh / -i after consonants */
    private fun fits(ending: String, stem: String): Boolean {
        val last = try {
            Sounds.parseRoman(stem).lastOrNull() ?: return false
        } catch (e: IllegalArgumentException) {
            return false
        }
        val vowel = Sounds.isVowel(last)
        return when (ending) {
            "n", "r", "sh", "ri" -> vowel
            "an", "or", "esh", "i" -> !vowel
            else -> true
        }
    }

    /** dictionary search: best matches first. script input is read as romanization */
    fun search(query: String): List<Entry> {
        val q = Sounds.scriptToRoman(query).lowercase().trim()
        if (q.isEmpty()) return all.sortedBy { it.word.removePrefix("-") }
        val base = analyze(q)?.entry
        return all.mapNotNull { e ->
            val words = e.meaning.lowercase().split(Regex("[^a-z']+")).filter { it.isNotEmpty() }
            val score = when {
                e.word == q || e.word == "-$q" -> 0
                e == base -> 1
                e.word.startsWith(q) -> 2
                q in words || q.removePrefix("to ") in words -> 3
                e.meaning.lowercase().contains(q) -> 4
                e.word.contains(q) -> 5
                else -> return@mapNotNull null
            }
            e to score
        }.sortedWith(compareBy({ it.second }, { it.first.word.length }, { it.first.word })).map { it.first }
    }
}
