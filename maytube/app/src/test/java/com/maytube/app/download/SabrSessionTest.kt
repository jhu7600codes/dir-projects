package com.maytube.app.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for parsing yt2009's `sabrExactRes` quality list --
 * embedded the same way as the `sabrBase` SABR session
 * (yt2009templates.js playerHDSabr(): `var sabrExactRes =
 * '${JSON.stringify(exactData)}';`, a JSON *string* the page's own JS
 * later JSON.parse()s, html5-player.js:3220), not a plain JS literal.
 */
class SabrSessionTest {

    @Test
    fun `parses itag, label and height from a real sabrExactRes payload`() {
        val html = """
            <script>
            var sabrBase = "/sabr_playback?pid=abc123";
            var sabrExactRes = '[[137,"1080p",1080,"h264","video/mp4",{"partList":[]}],[136,"720p",720,"h264","video/mp4",{"partList":[]}],[135,"480p",480,"h264","video/mp4",{"partList":[]}]]';
            </script>
        """.trimIndent()

        val qualities = SabrSession.parseQualities(html)
        assertEquals(3, qualities.size)
        assertEquals(SabrSession.QualityOption(137, "1080p", 1080), qualities[0])
        assertEquals(SabrSession.QualityOption(136, "720p", 720), qualities[1])
        assertEquals(SabrSession.QualityOption(135, "480p", 480), qualities[2])
    }

    @Test
    fun `returns an empty list when the page has no quality data at all`() {
        val html = """
            <script>
            var sabrBase = "/sabr_playback?pid=abc123";
            </script>
        """.trimIndent()
        assertTrue(SabrSession.parseQualities(html).isEmpty())
    }

    @Test
    fun `malformed quality json degrades to an empty list instead of throwing`() {
        // matches the capture regex (has a trailing "]") but isn't valid
        // JSON once parsed -- must not crash the whole session resolution
        // over a quality-picker nice-to-have
        val html = """var sabrExactRes = '[[137,]]';"""
        assertTrue(SabrSession.parseQualities(html).isEmpty())
    }
}
