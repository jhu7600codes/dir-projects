package app.krala.core

/**
 * A small markdown parser tuned for chat replies. Robust against half streamed input: an
 * unclosed code fence becomes a code block that runs to the end.
 */
object Markdown {
    sealed interface Block {
        data class Heading(val level: Int, val text: String) : Block
        data class Paragraph(val text: String) : Block
        data class Code(val language: String, val code: String, val closed: Boolean) : Block
        data class ListItem(val text: String, val ordered: Boolean, val number: Int, val depth: Int, val checked: Boolean?) : Block
        data class Quote(val text: String) : Block
        data class Table(val header: List<String>, val rows: List<List<String>>) : Block
        data object Rule : Block
    }

    private val fence = Regex("""^\s{0,3}(```+|~~~+)\s*([\w+#.\-]*)\s*$""")
    private val heading = Regex("""^\s{0,3}(#{1,6})\s+(.*?)\s*#*\s*$""")
    private val bullet = Regex("""^(\s*)([-*+])\s+(.*)$""")
    private val ordered = Regex("""^(\s*)(\d{1,9})[.)]\s+(.*)$""")
    private val rule = Regex("""^\s{0,3}([-*_])(\s*\1){2,}\s*$""")
    private val task = Regex("""^\[([ xX])]\s+(.*)$""")
    private val tableSep = Regex("""^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$""")

    fun parse(src: String): List<Block> {
        val lines = src.replace("\r\n", "\n").split('\n')
        val out = mutableListOf<Block>()
        val para = StringBuilder()
        fun flushPara() {
            if (para.isNotBlank()) out += Block.Paragraph(para.toString().trim())
            para.clear()
        }
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val f = fence.matchEntire(line)
            if (f != null) {
                flushPara()
                val marker = f.groupValues[1]
                val lang = f.groupValues[2]
                val code = StringBuilder()
                var j = i + 1
                var closed = false
                while (j < lines.size) {
                    val l = lines[j]
                    if (l.trim().startsWith(marker.take(3)) && l.trim().trimStart(marker[0]).isBlank() && l.trim().length >= marker.length) {
                        closed = true
                        break
                    }
                    if (code.isNotEmpty() || j > i + 1) code.append('\n')
                    code.append(l)
                    j++
                }
                out += Block.Code(lang, code.toString().trimEnd('\n'), closed)
                i = if (closed) j + 1 else j
                continue
            }
            if (line.isBlank()) {
                flushPara(); i++; continue
            }
            val h = heading.matchEntire(line)
            if (h != null) {
                flushPara()
                out += Block.Heading(h.groupValues[1].length, h.groupValues[2])
                i++
                continue
            }
            if (rule.matches(line)) {
                flushPara(); out += Block.Rule; i++; continue
            }
            // tables: header row, separator row, body rows
            if (line.contains('|') && i + 1 < lines.size && tableSep.matches(lines[i + 1])) {
                flushPara()
                val header = cells(line)
                val rows = mutableListOf<List<String>>()
                var j = i + 2
                while (j < lines.size && lines[j].contains('|') && lines[j].isNotBlank()) {
                    rows += cells(lines[j]).let { r -> if (r.size < header.size) r + List(header.size - r.size) { "" } else r.take(header.size) }
                    j++
                }
                out += Block.Table(header, rows)
                i = j
                continue
            }
            if (line.trimStart().startsWith(">")) {
                flushPara()
                val q = StringBuilder()
                var j = i
                while (j < lines.size && lines[j].trimStart().startsWith(">")) {
                    if (q.isNotEmpty()) q.append('\n')
                    q.append(lines[j].trimStart().removePrefix(">").removePrefix(" "))
                    j++
                }
                out += Block.Quote(q.toString())
                i = j
                continue
            }
            val b = bullet.matchEntire(line)
            val o = if (b == null) ordered.matchEntire(line) else null
            if (b != null || o != null) {
                flushPara()
                val m = b ?: o!!
                val depth = m.groupValues[1].replace("\t", "    ").length / 2
                var text = m.groupValues[3]
                var checked: Boolean? = null
                task.matchEntire(text)?.let { t ->
                    checked = t.groupValues[1].isNotBlank()
                    text = t.groupValues[2]
                }
                // lazy continuation lines belong to the item
                var j = i + 1
                while (j < lines.size && lines[j].isNotBlank() && lines[j].startsWith("  ") &&
                    bullet.matchEntire(lines[j]) == null && ordered.matchEntire(lines[j]) == null && fence.matchEntire(lines[j]) == null
                ) {
                    text += "\n" + lines[j].trim()
                    j++
                }
                out += Block.ListItem(
                    text = text,
                    ordered = o != null,
                    number = o?.groupValues?.get(2)?.toIntOrNull() ?: 0,
                    depth = depth.coerceAtMost(4),
                    checked = checked,
                )
                i = j
                continue
            }
            if (para.isNotEmpty()) para.append('\n')
            para.append(line)
            i++
        }
        flushPara()
        return out
    }

    private fun cells(row: String): List<String> {
        var r = row.trim()
        if (r.startsWith("|")) r = r.substring(1)
        if (r.endsWith("|") && !r.endsWith("\\|")) r = r.dropLast(1)
        return r.split(Regex("""(?<!\\)\|""")).map { it.trim().replace("\\|", "|") }
    }

    // ---- inline ----

    sealed interface Span {
        data class Text(val text: String) : Span
        data class Styled(val children: List<Span>, val bold: Boolean = false, val italic: Boolean = false, val strike: Boolean = false) : Span
        data class InlineCode(val code: String) : Span
        data class Link(val children: List<Span>, val url: String) : Span
    }

    private val autolink = Regex("""https?://[^\s<>()\[\]]+[^\s<>()\[\].,;:!?'"]""")

    fun inline(src: String): List<Span> = InlineParser(src).parse()

    private class InlineParser(val s: String) {
        var i = 0

        fun parse(stop: String? = null): List<Span> {
            val out = mutableListOf<Span>()
            val buf = StringBuilder()
            fun flush() {
                if (buf.isNotEmpty()) {
                    out += Span.Text(buf.toString()); buf.clear()
                }
            }
            while (i < s.length) {
                if (stop != null && s.startsWith(stop, i)) {
                    flush()
                    return out
                }
                val c = s[i]
                when {
                    c == '\\' && i + 1 < s.length && s[i + 1] in "\\`*_~[]()#+-.!|>" -> {
                        buf.append(s[i + 1]); i += 2
                    }
                    c == '`' -> {
                        val ticks = s.substring(i).takeWhile { it == '`' }
                        val end = s.indexOf(ticks, i + ticks.length)
                        if (end > 0) {
                            flush()
                            out += Span.InlineCode(s.substring(i + ticks.length, end).trim())
                            i = end + ticks.length
                        } else {
                            buf.append(ticks); i += ticks.length
                        }
                    }
                    s.startsWith("**", i) || s.startsWith("__", i) -> {
                        val mark = s.substring(i, i + 2)
                        val close = s.indexOf(mark, i + 2)
                        if (close > i + 2 && !(mark == "__" && wordBefore(i))) {
                            flush()
                            i += 2
                            val kids = InlineParser(s.substring(i, close)).parse()
                            out += Span.Styled(kids, bold = true)
                            i = close + 2
                        } else {
                            buf.append(mark); i += 2
                        }
                    }
                    s.startsWith("~~", i) -> {
                        val close = s.indexOf("~~", i + 2)
                        if (close > i + 2) {
                            flush()
                            out += Span.Styled(InlineParser(s.substring(i + 2, close)).parse(), strike = true)
                            i = close + 2
                        } else {
                            buf.append("~~"); i += 2
                        }
                    }
                    (c == '*' || c == '_') && i + 1 < s.length && !s[i + 1].isWhitespace() && !(c == '_' && wordBefore(i)) -> {
                        val close = findSingle(c, i + 1)
                        if (close > 0) {
                            flush()
                            out += Span.Styled(InlineParser(s.substring(i + 1, close)).parse(), italic = true)
                            i = close + 1
                        } else {
                            buf.append(c); i++
                        }
                    }
                    c == '[' -> {
                        val closeText = matching(i, '[', ']')
                        if (closeText > 0 && closeText + 1 < s.length && s[closeText + 1] == '(') {
                            val closeUrl = s.indexOf(')', closeText + 2)
                            if (closeUrl > 0) {
                                flush()
                                val kids = InlineParser(s.substring(i + 1, closeText)).parse()
                                val url = s.substring(closeText + 2, closeUrl).trim().substringBefore(' ')
                                out += Span.Link(kids, url)
                                i = closeUrl + 1
                                continue
                            }
                        }
                        buf.append(c); i++
                    }
                    (c == 'h') && (s.startsWith("http://", i) || s.startsWith("https://", i)) && !wordBefore(i) -> {
                        val m = autolink.find(s, i)
                        if (m != null && m.range.first == i) {
                            flush()
                            out += Span.Link(listOf(Span.Text(m.value)), m.value)
                            i = m.range.last + 1
                        } else {
                            buf.append(c); i++
                        }
                    }
                    else -> {
                        buf.append(c); i++
                    }
                }
            }
            flush()
            return out
        }

        private fun wordBefore(pos: Int) = pos > 0 && s[pos - 1].isLetterOrDigit()

        private fun findSingle(c: Char, from: Int): Int {
            var j = from
            while (j < s.length) {
                if (s[j] == '\\') { j += 2; continue }
                if (s[j] == '`') {
                    val e = s.indexOf('`', j + 1); if (e < 0) return -1; j = e + 1; continue
                }
                if (s[j] == c && !s[j - 1].isWhitespace() && !(j + 1 < s.length && s[j + 1] == c)) {
                    if (c == '_' && j + 1 < s.length && s[j + 1].isLetterOrDigit()) { j++; continue }
                    return j
                }
                if (s[j] == c && j + 1 < s.length && s[j + 1] == c) { j += 2; continue }
                j++
            }
            return -1
        }

        private fun matching(open: Int, o: Char, c: Char): Int {
            var depth = 0
            var j = open
            while (j < s.length) {
                when (s[j]) {
                    '\\' -> j++
                    o -> depth++
                    c -> { depth--; if (depth == 0) return j }
                }
                j++
            }
            return -1
        }
    }

    /** plain text version (for copy, titles, search) */
    fun toPlain(src: String): String = parse(src).joinToString("\n\n") { b ->
        when (b) {
            is Block.Heading -> plain(inline(b.text))
            is Block.Paragraph -> plain(inline(b.text))
            is Block.Code -> b.code
            is Block.ListItem -> "  ".repeat(b.depth) + (if (b.ordered) "${b.number}. " else "• ") + plain(inline(b.text))
            is Block.Quote -> b.text
            is Block.Table -> (listOf(b.header) + b.rows).joinToString("\n") { it.joinToString("  ") }
            Block.Rule -> "—"
        }
    }

    fun plain(spans: List<Span>): String = spans.joinToString("") { sp ->
        when (sp) {
            is Span.Text -> sp.text
            is Span.InlineCode -> sp.code
            is Span.Styled -> plain(sp.children)
            is Span.Link -> plain(sp.children)
        }
    }
}
