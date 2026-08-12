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
    fun `player letterboxes instead of cropping non-16by9 video`() {
        // regression test: forcing width/height:100% with no object-fit
        // stretches/crops video whose real aspect ratio isn't exactly the
        // box's 16:9 (object-fit's initial value is "fill"). contain keeps
        // the whole frame visible with black bars instead.
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains("object-fit: contain"))
        assertTrue(script.contains("background: #000"))
    }

    @Test
    fun `dark mode class is toggled based on config`() {
        val darkOn = MobileInjector.buildInjectionScript(config.copy(darkMode = true))
        assertTrue(darkOn.contains("classList.toggle('maytube-dark', true)"))

        val darkOff = MobileInjector.buildInjectionScript(config.copy(darkMode = false))
        assertTrue(darkOff.contains("classList.toggle('maytube-dark', false)"))
    }

    @Test
    fun `css covers the watch-longform-buttons float and the unstyled ab-title`() {
        // regression test: #watch-longform-buttons (float:right, holds the
        // resize/popout icon buttons) and .watch-vid-ab-title (no CSS rule
        // anywhere in yt2009 itself, always rendered as a raw oversized h1)
        // both visibly collided with the masthead nav once reflowed into a
        // single mobile column.
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains("#watch-longform-buttons"))
        assertTrue(script.contains(".watch-vid-ab-title"))
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
    fun `native player mode pauses and blanks the webview's own video`() {
        // regression test: Settings > native player plays through
        // PlayerActivity's own SABR fetch instead -- the WebView's copy of
        // the video must not also be left running, or it silently
        // double-fetches the same video for a player nobody can see.
        val script = MobileInjector.buildInjectionScript(config.copy(nativePlayer = true))
        assertTrue(script.contains("if (true) {"))
        assertTrue(script.contains("pv.pause()"))
        assertTrue(script.contains("maytube-native-hint"))
    }

    @Test
    fun `native player mode off leaves the webview's own video alone`() {
        val script = MobileInjector.buildInjectionScript(config.copy(nativePlayer = false))
        assertTrue(script.contains("if (false) {"))
    }

    @Test
    fun `forces yt2009's own CSS fullscreen fallback instead of real native fullscreen`() {
        // regression test: real WebView fullscreen (onShowCustomView) drops
        // html5-player.js's own sibling control divs, rendering bare video
        // with no play/seek/HD controls -- confirmed side-by-side against
        // Fennec, which shows those controls. Forcing #watch-player-div's
        // requestFullscreen() to throw synchronously is what triggers
        // html5-player.js's own try/catch fallback into its existing
        // CSS-driven "fullscreen-unsupported" mode (nbedit_style.css),
        // which keeps everything intact.
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains("getElementById('watch-player-div')"))
        assertTrue(script.contains("playerDiv.requestFullscreen ="))
        assertTrue(script.contains("NotSupportedError"))
    }

    @Test
    fun `patches document exitFullscreen to throw when nothing is really fullscreen`() {
        // regression test: html5-player.js's own exit path only runs its
        // cleanup (which un-does the fake-fullscreen CSS classes) inside a
        // catch block, expecting document.exitFullscreen() to throw
        // synchronously when there's nothing real to exit. Real WebView
        // instead resolves/rejects a Promise, invisibly to a plain
        // synchronous try/catch -- without this patch, the page gets stuck
        // showing the fake-fullscreen CSS with no way out via its own button.
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains("document.exitFullscreen = function()"))
        assertTrue(script.contains("InvalidStateError"))
    }

    @Test
    fun `relays yt2009's fullscreen-unsupported class toggle to native code`() {
        // regression test: without this, "fullscreen" would just be a
        // same-size CSS overlay with the Android status bar/app toolbar
        // still on top of it -- MaytubeFullscreenBridge is what lets
        // MaytubeWebChromeClient still apply the same system-chrome-hiding/
        // landscape-lock treatment real native fullscreen would have gotten.
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains("fullscreen-unsupported"))
        assertTrue(script.contains("MutationObserver"))
        assertTrue(script.contains("window.MaytubeFullscreen.onEnter()"))
        assertTrue(script.contains("window.MaytubeFullscreen.onExit()"))
    }

    @Test
    fun `fullscreen-unsupported override beats the plain player id selector on specificity`() {
        // regression test: #watch-player-div (bare ID, specificity 100) and
        // nbedit_style.css's own .fullscreen-unsupported (bare class,
        // specificity 10) both use !important -- the higher-specificity ID
        // rule silently wins by default, keeping the player locked to its
        // normal in-page aspect-ratio box even once html5-player.js adds
        // that class. #watch-player-div.fullscreen-unsupported (ID+class,
        // specificity 110) is what actually overrides it; this is the
        // actual reason fullscreen visibly did nothing, not a WebView
        // platform limitation.
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains("#watch-player-div.fullscreen-unsupported"))
        assertTrue(script.contains("position: fixed !important"))
        // must out-rank the sticky masthead's own z-index, or the header
        // renders on top of the now-correctly-positioned fullscreen video
        assertTrue(script.contains("z-index: 999999 !important"))
    }

    @Test
    fun `hides the masthead outright during fake fullscreen instead of relying on z-index alone`() {
        // regression test: reported directly from a real device -- the
        // video correctly went full-viewport (the specificity fix worked),
        // but #masthead-container still rendered on top of it, because it's
        // a sibling under #baseDiv, not a descendant of #watch-player-div,
        // so its z-index:1000 wasn't reliably losing to the player's
        // z-index:999999 once ancestor stacking contexts got involved.
        // Hiding it outright sidesteps that fight entirely.
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains("classList.toggle('maytube-pseudo-fullscreen'"))
        assertTrue(script.contains("html.maytube-pseudo-fullscreen #masthead-container"))
    }

    @Test
    fun `forces the title's inline style too, not just the class rule`() {
        // regression test: reported directly from a real device rendering
        // the title enormous (one word per line, full viewport width)
        // despite the existing !important class rule -- some yt2009
        // deployments' own CSS apparently sizes #watch-vid-title h1 with an
        // ID+tag rule of its own that isn't guaranteed to lose to a plain
        // class selector on every instance. An inline style set with
        // 'important' priority outranks any external stylesheet rule
        // regardless of selector, so this is correct no matter what a given
        // instance's CSS actually does.
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains("querySelectorAll('.watch-vid-ab-title')"))
        assertTrue(script.contains("setProperty('font-size', '17px', 'important')"))
    }

    @Test
    fun `hides the desktop-only popout-player buttons`() {
        // regression test: #watch-longform-buttons (yt2009's own
        // float:right "change player size"/"popout" icon buttons) rendered
        // displaced up near the masthead once unfloated -- and neither
        // button does anything useful in a mobile WebView shell anyway.
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains("#watch-longform-buttons"))
    }

    @Test
    fun `constrains the URL and embed code fields to the viewport`() {
        // regression test: reported directly from a real device -- these
        // plain <input type="text"> fields (full watch URL / <object> embed
        // snippet) had no width constraint at all, overflowing the mobile
        // viewport with the majority of their content unreachable (no
        // horizontal scroll affordance on a plain input).
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains("#watch-url-field, #embed_code"))
    }

    @Test
    fun `applies a real font stack and page background instead of yt2009's bare defaults`() {
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains("-apple-system"))
        assertTrue(script.contains("background: #f1f1f1"))
    }

    @Test
    fun `styles the related-videos and more-from section headers`() {
        // .yt-uix-expander-head is the real class yt2009 uses for both
        // panels (watch.html), which itself sets no font-size/weight at
        // all -- always rendered as a raw browser-default h2.
        val script = MobileInjector.buildInjectionScript(config)
        assertTrue(script.contains(".yt-uix-expander-head"))
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
