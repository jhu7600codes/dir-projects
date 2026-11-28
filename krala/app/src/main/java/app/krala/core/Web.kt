package app.krala.core

import java.net.URLDecoder

/** html helpers for web search: result parsing and readable page text. pure, unit tested. */
object Web {
    data class Result(val title: String, val url: String, val snippet: String)

    private val ddgLink = Regex("""<a[^>]*class="[^"]*result__a[^"]*"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
    private val ddgSnippet = Regex("""<a[^>]*class="[^"]*result__snippet[^"]*"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)

    /** results from html.duckduckgo.com, ads and duplicates removed */
    fun parseDuckDuckGo(html: String, max: Int = 8): List<Result> {
        val links = ddgLink.findAll(html).toList()
        val snippets = ddgSnippet.findAll(html).map { text(it.groupValues[1]) }.toList()
        return links.mapIndexedNotNull { i, m ->
            val url = unwrapDdg(m.groupValues[1]) ?: return@mapIndexedNotNull null
            if ("duckduckgo.com/y.js" in url || "ad_domain" in url) return@mapIndexedNotNull null
            Result(text(m.groupValues[2]), url, snippets.getOrElse(i) { "" })
        }.distinctBy { it.url }.take(max)
    }

    fun unwrapDdg(href: String): String? {
        val h = decodeEntities(href)
        val uddg = Regex("""[?&]uddg=([^&]+)""").find(h)?.groupValues?.get(1)
        val url = if (uddg != null) URLDecoder.decode(uddg, "UTF-8") else if (h.startsWith("//")) "https:$h" else h
        return url.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }

    /** the readable text of a page: scripts, styles, menus and markup dropped, paragraphs kept */
    fun readable(html: String, maxChars: Int = 4_000): String {
        var h = html
        h = h.replace(Regex("""(?is)<(script|style|noscript|svg|nav|footer|header|form|aside|iframe|template)[^>]*>.*?</\1>"""), " ")
        h = h.replace(Regex("""(?is)<!--.*?-->"""), " ")
        val body = Regex("""(?is)<(article|main)[^>]*>(.*?)</\1>""").find(h)?.groupValues?.get(2) ?: h
        var t = body.replace(Regex("""(?i)<(br|/p|/div|/li|/h[1-6]|/tr|/section)[^>]*>"""), "\n")
        t = text(t)
        val lines = t.lines().map { it.trim() }.filter { it.length > 1 }
        // keep lines that look like prose, so cookie banners and link lists fall away
        val prose = lines.filter { it.length >= 40 || it.endsWith(".") || it.endsWith(":") }
        return (prose.ifEmpty { lines }).joinToString("\n").take(maxChars)
    }

    fun title(html: String): String? =
        Regex("""(?is)<title[^>]*>(.*?)</title>""").find(html)?.groupValues?.get(1)?.let { text(it) }?.takeIf { it.isNotBlank() }

    fun text(htmlFragment: String): String =
        decodeEntities(htmlFragment.replace(Regex("""<[^>]+>"""), " ")).replace(Regex("""[ \t ]+"""), " ").replace(Regex("""\n\s*\n+"""), "\n").trim()

    fun decodeEntities(s: String): String {
        var out = s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
            .replace("&#39;", "'").replace("&#x27;", "'").replace("&apos;", "'").replace("&nbsp;", " ")
        out = Regex("""&#(\d+);""").replace(out) { m -> m.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value }
        out = Regex("""&#x([0-9a-fA-F]+);""").replace(out) { m -> m.groupValues[1].toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value }
        return out
    }

    fun domain(url: String): String = url.substringAfter("://").substringBefore('/').removePrefix("www.")

    /** a search query from a chat message: short, no filler */
    fun query(message: String): String {
        val t = message.replace(Regex("""\s+"""), " ").trim()
        val cleaned = t.replace(Regex("""(?i)^((hey|hi|yo|krala|крала|привет)[,!.\s]+)+"""), "")
            .replace(Regex("""(?i)^(can you |could you |please |pls |search( the web)?( for)? |google |look up |найди |поищи |загугли )+"""), "")
        return cleaned.take(200).trim().ifBlank { t.take(200) }
    }

    /** the --web-- section text */
    fun section(query: String, date: String, pages: List<Pair<Result, String>>): String = buildString {
        append("web search for \"").append(query).append("\" (").append(date).append("). ")
        append("use these if they help and name the sources (site names) you used. say so if they don't answer it.\n")
        pages.forEachIndexed { i, (r, body) ->
            append("[").append(i + 1).append("] ").append(r.title).append(" · ").append(r.url).append('\n')
            append(body.ifBlank { r.snippet }.trim()).append("\n\n")
        }
    }.trim()
}
