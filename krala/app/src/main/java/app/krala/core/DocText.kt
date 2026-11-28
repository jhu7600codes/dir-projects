package app.krala.core

/** text out of office xml, pure so it can be tested */
object DocText {
    /** word/document.xml → text with paragraphs */
    fun docxXml(xml: String): String {
        val t = xml.replace(Regex("""</w:p>"""), "\n").replace(Regex("""<w:tab/>"""), "\t").replace(Regex("""<w:br/>"""), "\n")
        return Web.decodeEntities(t.replace(Regex("""<[^>]+>"""), "")).lines().joinToString("\n") { it.trimEnd() }.replace(Regex("""\n{3,}"""), "\n\n").trim()
    }

    /** ppt/slides/slideN.xml → its text runs */
    fun slideXml(xml: String): String =
        Regex("""<a:t>(.*?)</a:t>""").findAll(xml).map { Web.decodeEntities(it.groupValues[1]) }.joinToString(" ").trim()

    /** heuristic: is this byte content readable text? */
    fun looksLikeText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        val sample = bytes.take(4096)
        if (sample.any { it == 0.toByte() }) return false
        val s = String(sample.toByteArray(), Charsets.UTF_8)
        val bad = s.count { it == '�' || (it.isISOControl() && it !in "\n\r\t") }
        return bad * 50 < s.length
    }

    fun truncate(text: String, maxChars: Int): String =
        if (text.length <= maxChars) text else text.take(maxChars) + "\n[… cut, ${text.length - maxChars} more characters]"
}
