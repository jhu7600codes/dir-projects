package app.krala.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebAndDocsTest {
    @Test
    fun parsesDuckDuckGoHtml() {
        val html = """
            <div class="result"><a rel="nofollow" class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fpage&amp;rut=x">Example &amp; Co</a>
            <a class="result__snippet" href="#">a <b>snippet</b> here</a></div>
            <div class="result"><a class="result__a" href="https://duckduckgo.com/y.js?ad_domain=x">ad</a><a class="result__snippet">ad</a></div>
            <div class="result"><a class="result__a" href="https://second.org/">Second</a><a class="result__snippet">two</a></div>
        """
        val r = Web.parseDuckDuckGo(html)
        assertEquals(2, r.size)
        assertEquals(Web.Result("Example & Co", "https://example.com/page", "a snippet here"), r[0])
        assertEquals("https://second.org/", r[1].url)
    }

    @Test
    fun readableTextDropsScriptsAndMenus() {
        val html = "<html><head><title>T</title><script>var x=1</script></head><body><nav>home about</nav>" +
            "<article><h1>Big news</h1><p>This is the first real paragraph of the article, long enough to keep.</p></article></body></html>"
        val t = Web.readable(html)
        assertTrue(t.contains("first real paragraph"))
        assertFalse(t.contains("var x") || t.contains("home about"))
        assertEquals("T", Web.title(html))
    }

    @Test
    fun queriesLoseFiller() {
        assertEquals("weather in kazan", Web.query("hey krala, can you search for weather in kazan"))
        assertEquals("курс доллара", Web.query("найди курс доллара"))
        assertEquals("example.com", Web.domain("https://www.example.com/a/b"))
    }

    @Test
    fun entities() {
        assertEquals("a & b ' é", Web.decodeEntities("a &amp; b &#39; &#233;"))
    }

    @Test
    fun docxAndSlides() {
        val xml = "<w:document><w:body><w:p><w:r><w:t>hello</w:t></w:r></w:p><w:p><w:r><w:t>world &amp; co</w:t></w:r></w:p></w:body></w:document>"
        assertEquals("hello\nworld & co", DocText.docxXml(xml))
        assertEquals("title here more", DocText.slideXml("<p:sp><a:t>title here</a:t></p:sp><a:t>more</a:t>"))
    }

    @Test
    fun textDetectionAndTruncation() {
        assertTrue(DocText.looksLikeText("fun main() = println(\"hi\")\n".toByteArray()))
        assertFalse(DocText.looksLikeText(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0, 0, 1, 2)))
        assertEquals("abc\n[… cut, 3 more characters]", DocText.truncate("abcdef", 3))
    }

    @Test
    fun sectionsGoInsideTheBlock() {
        val first = KralaTags.compose(KralaTags.build(KralaTags.Block("", "p", "")), "q")
        val out = KralaTags.withSection(first, KralaTags.WEB, "[1] result")
        assertEquals(1, Regex("--krala tags--").findAll(out).count())
        assertTrue(out.contains("--web--\n[1] result\n--krala tags end--"))
        assertEquals("q", KralaTags.strip(out))
        val later = KralaTags.withSection("later", KralaTags.FILES, "[a.txt]\nbody")
        assertEquals("--krala tags--\n--files--\n[a.txt]\nbody\n--krala tags end--\n\nlater", later)
        assertNull(KralaTags.parse("later"))
    }

    @Test
    fun keywordTriggers() {
        assertTrue(Web.autoTrigger("can you search for flights to kazan"))
        assertTrue(Web.autoTrigger("look up the score"))
        assertTrue(Web.autoTrigger("Check the weather"))
        assertTrue(Web.autoTrigger("поищи рецепт борща"))
        assertFalse(Web.autoTrigger("i researched it already"))
        assertFalse(Web.autoTrigger("how are you"))
    }

    @Test
    fun webTag() {
        assertEquals("kazan weather", WebTag.find("<web search>kazan weather</web search>"))
        assertEquals("", WebTag.find("<Web Search></web search>"))
        assertEquals(null, WebTag.find("just text"))
        assertEquals("", WebTag.strip("<web search>kazan weather</web search>"))
        assertEquals("hmm", WebTag.strip("hmm <web se"))
        assertEquals("a < b", WebTag.strip("a < b"))
    }
}
