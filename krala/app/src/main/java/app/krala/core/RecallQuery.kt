package app.krala.core

/** Turns free text into an fts5 query. Pure, so it is unit tested. */
object RecallQuery {
    private val stop = setOf(
        // en
        "the", "and", "for", "are", "but", "not", "you", "your", "with", "this", "that", "have", "has", "was", "were",
        "what", "when", "where", "which", "who", "why", "how", "can", "could", "would", "should", "will", "just", "like",
        "about", "from", "they", "them", "then", "than", "there", "their", "here", "been", "being", "into", "also",
        "some", "any", "all", "out", "get", "got", "did", "does", "doing", "its", "it's", "i'm", "im", "dont", "don't",
        "yeah", "yes", "okay", "lol", "hey", "hi", "hello", "please", "thanks", "thank", "really", "very", "much", "more",
        "know", "think", "want", "need", "make", "one", "now", "too", "our", "she", "her", "him", "his", "krala", "remember",
        // ru
        "что", "как", "это", "так", "вот", "был", "была", "было", "были", "для", "или", "его", "она", "они", "оно", "мне",
        "меня", "тебя", "тебе", "мой", "моя", "мое", "твой", "твоя", "уже", "еще", "ещё", "нет", "да", "все", "всё", "там",
        "тут", "где", "когда", "чем", "кто", "почему", "зачем", "если", "тоже", "очень", "просто", "можно", "надо", "нужно",
        "привет", "пока", "спасибо", "ладно", "короче", "типа", "вообще", "кстати", "помнишь", "крала",
    )

    fun terms(text: String, max: Int = 8): List<String> {
        val words = Regex("""[\p{L}\p{N}][\p{L}\p{N}'_\-]*""").findAll(text.lowercase()).map { it.value.trim('\'', '-', '_') }
        return words.filter { it.length >= 3 && it !in stop && !it.all(Char::isDigit) }
            .distinct()
            .sortedByDescending { it.length }
            .take(max)
            .toList()
    }

    /** prefix match on longer words, exact on short ones, all OR'ed */
    fun fts(terms: List<String>): String? {
        if (terms.isEmpty()) return null
        return terms.joinToString(" OR ") { t ->
            val clean = t.replace("\"", "")
            if (clean.length >= 5) "\"${clean.dropLast(1)}\"*" else "\"$clean\""
        }
    }

    /** fts query for the /recall command: every word must appear (prefix match) */
    fun ftsAll(text: String): String? {
        val words = Regex("""[\p{L}\p{N}]+""").findAll(text.lowercase()).map { it.value }.filter { it.length >= 2 }.toList()
        if (words.isEmpty()) return null
        return words.joinToString(" ") { "\"$it\"*" }
    }

    const val HL_START = '\u0002'
    const val HL_END = '\u0003'

    fun stripHighlights(s: String) = s.replace(HL_START.toString(), "").replace(HL_END.toString(), "")
}
