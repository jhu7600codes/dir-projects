package com.maytube.app.webview

import com.maytube.app.data.ServerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural sanity checks for the injected CSS/JS. This can't render the
 * result in an actual WebView from a JVM unit test, but it can catch the
 * class of mistake that shipped last time undetected: selectors for
 * containers that don't exist in yt2009's real markup, and basic
 * braces/quoting mistakes in the generated script.
 */
class MobileInjectorTest {

    private val config = ServerConfig(host = "example.test", port = 3000)

    @Test
    fun `generated script has balanced braces and parens`() {
        val script = MobileInjector.buildInjectionScript(config)
        assertEquals("unbalanced curly braces", script.count { it == '{' }, script.count { it == '}' })
        assertEquals("unbalanced parens", script.count { it == '(' }, script.count { it == ')' })
    }

    @Test
    fun `generated script sets viewport and re-asserts the flag cookie`() {
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains("name = 'viewport'"))
        assertTrue(script.contains("document.cookie ="))
        assertTrue(script.contains("exp_sabr"))
    }

    @Test
    fun `css targets the real yt2009 container ids, not invented ones`() {
        val script = MobileInjector.buildInjectionScript(config)
        // #baseDiv is the one wrapper every yt2009 page template shares
        // (index.htm, watch.html, channelpage.htm, search-generic-page.htm,
        // playlists.htm, my_videos.htm, videos.htm, channels.htm) and is
        // hard-locked to 960px in yt2009's own CSS -- this is the selector
        // that was missing in the version that shipped broken.
        assertTrue(script.contains("#baseDiv"))
        assertTrue(script.contains("#watch-this-vid"))
        assertTrue(script.contains("#watch-other-vids"))
        assertTrue(script.contains(".left-column"))
        assertTrue(script.contains(".video-cell"))
        assertTrue(script.contains("#search-options-container"))

        // selectors from the first draft that don't exist anywhere in
        // yt2009's actual markup/CSS -- make sure they don't creep back in
        assertFalse(script.contains(".yt-lockup"))
        assertFalse(script.contains("#watch-sidebar"))
        assertFalse(script.contains("#watch7-sidebar"))
    }

    @Test
    fun `css unfloats the masthead's own children, not just the masthead itself`() {
        // regression test: #masthead was squeezed to mobile width but its
        // children (#logo/#masthead-search float:left,
        // #masthead-nav-user float:right -- all sized for the original
        // 960px bar) were left untouched, so they overlapped each other
        // instead of reflowing.
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains("#logo"))
        assertTrue(script.contains("#masthead-search"))
        assertTrue(script.contains("#masthead-nav-user"))
        assertTrue(script.contains("#masthead-nav-main"))
    }

    @Test
    fun `logo never gets forced to full width`() {
        // regression test: #logo is a .master-sprite button -- a fixed
        // 110px crop window onto a shared sprite sheet tiled with
        // "background: ... repeat-x". An earlier fix for the masthead
        // overlap forced every masthead child (#logo included) to
        // width:100%, which widened that crop window and revealed the
        // sprite tiling itself: multiple repeated copies of the YouTube
        // wordmark side by side. #logo may be unfloated like everything
        // else, but must never be forced to 100% width.
        val script = MobileInjector.buildInjectionScript(config)
        assertFalse(script.contains("#logo, #masthead-search"))
        assertTrue(script.contains("#logo, #masthead-qr"))
    }

    @Test
    fun `sabr diagnostic script has balanced braces and hooks sabr_playback only`() {
        val script = MobileInjector.buildSabrDiagnosticScript()
        assertEquals("unbalanced curly braces", script.count { it == '{' }, script.count { it == '}' })
        assertEquals("unbalanced parens", script.count { it == '(' }, script.count { it == ')' })
        assertTrue(script.contains("sabr_playback"))
        assertTrue(script.contains("XMLHttpRequest.prototype.open"))
        // must guard against re-wrapping XMLHttpRequest.prototype.open on
        // every navigation, since addDocumentStartJavaScript re-runs this
        // on every page load
        assertTrue(script.contains("__maytubeSabrHooked"))
    }

    @Test
    fun `player css never blanket-resizes every div inside watch-player-div`() {
        // regression test: html5-player.js appends its own JS-managed
        // overlay divs (.annotations_container, an end-screen/related
        // grid) directly into #watch-player-div, positioned via pixel math
        // the JS computes itself. "#watch-player-div > div { width:100%;
        // height:100% }" stomps that positioning and forces whatever
        // overlay div is present to fill the whole frame -- reported as an
        // unloaded-looking grid of tiles sitting where the video should be,
        // with the actual <video> element hidden underneath. Only
        // video/object/embed may get the full-size treatment.
        val script = MobileInjector.buildInjectionScript(config)
        assertFalse(script.contains("#watch-player-div > div"))
    }

    @Test
    fun `flag cookie carries exp_sabr when sabr is enabled`() {
        val value = MobileInjector.flagCookieValue(config.copy(sabrEnabled = true))
        assertTrue(value.contains("exp_sabr"))
    }

    @Test
    fun `flag cookie omits exp_sabr when sabr is disabled`() {
        val value = MobileInjector.flagCookieValue(config.copy(sabrEnabled = false))
        assertFalse(value.contains("exp_sabr"))
    }

    @Test
    fun `flag cookie carries hd_1080 only when prefer1080p is set`() {
        assertTrue(MobileInjector.flagCookieValue(config.copy(prefer1080p = true)).contains("hd_1080"))
        assertFalse(MobileInjector.flagCookieValue(config.copy(prefer1080p = false)).contains("hd_1080"))
    }

    @Test
    fun `extracts video id from a watch url`() {
        assertEquals("dQw4w9WgXcQ", MobileInjector.extractVideoId("http://host:3000/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `extracts video id from an embed url`() {
        assertEquals("dQw4w9WgXcQ", MobileInjector.extractVideoId("http://host:3000/embed/dQw4w9WgXcQ"))
    }

    @Test
    fun `returns null for a non-watch page`() {
        assertEquals(null, MobileInjector.extractVideoId("http://host:3000/"))
    }
}
