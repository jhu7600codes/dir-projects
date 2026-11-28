package app.krala.core

/**
 * Splits a reply into its visible thinking and the answer. Reasoning models either stream a
 * separate reasoning field or put <think>...</think> at the start of the content.
 */
object Think {
    private val open = Regex("""^\s*<(think|thinking|reasoning)>""", RegexOption.IGNORE_CASE)

    data class Split(val thinking: String, val answer: String, val thinkingDone: Boolean)

    fun split(content: String): Split {
        val m = open.find(content) ?: return Split("", content, true)
        val tag = m.groupValues[1]
        val rest = content.substring(m.range.last + 1)
        val close = Regex("</$tag>", RegexOption.IGNORE_CASE).find(rest)
            ?: return Split(rest.trim(), "", false)
        return Split(rest.substring(0, close.range.first).trim(), rest.substring(close.range.last + 1).trimStart(), true)
    }

    fun answer(content: String) = split(content).answer
}
