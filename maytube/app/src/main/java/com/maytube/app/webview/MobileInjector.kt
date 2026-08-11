package com.maytube.app.webview

import com.maytube.app.data.ServerConfig
import org.json.JSONObject

/**
 * Builds the CSS/JS that gets injected into every yt2009 page the WebView
 * loads.
 *
 * yt2009 reproduces the ~2009 YouTube desktop site verbatim: fixed 960px
 * layout, no viewport meta tag, table-based two column pages. None of that
 * is touched server-side -- instead we reflow it client-side after each
 * page load, the same way a userscript/extension would.
 *
 * This also carries the one thing that actually matters functionally: the
 * yt2009 backend enables SABR + MSE playback (see back/yt2009html.js,
 * back/yt2009sabr.js) purely by sniffing the raw `Cookie` header for the
 * substring "exp_sabr" -- there's no dedicated cookie name or endpoint, any
 * cookie containing that text flips the flag. We set that (and the sibling
 * hd_1080 flag) both up front via CookieManager *and* re-assert it here via
 * document.cookie so it survives even if a page clears cookies itself.
 */
object MobileInjector {

    /**
     * Cookie flags yt2009 looks for as raw substrings of the Cookie header.
     * See yt2009html.js ("flags.includes(...)") and yt2009_flags.htm for the
     * canonical list -- these are the ones relevant to playback.
     */
    fun flagCookieValue(config: ServerConfig): String {
        val flags = mutableListOf<String>()
        if (config.sabrEnabled) {
            flags += "exp_sabr"
            flags += "exp_sabr_audiotracks"
        }
        if (config.prefer1080p) {
            flags += "hd_1080"
        }
        // marker so we can tell at a glance (chrome://inspect etc.) that
        // maytube set this, and so the value is never empty
        flags += "maytube_client"
        return flags.joinToString(":")
    }

    /**
     * CSS injected on every page to reflow the fixed 960px desktop layout.
     *
     * Every selector below was taken from actually reading yt2009's own
     * CSS/templates (assets/site-assets/www-core-*.css, back/yt2009templates.js,
     * and the page .htm/.html files themselves), not guessed. The first cut
     * of this used plausible-sounding class names that don't exist in
     * yt2009's markup at all, which is why it visibly did nothing -- this
     * version instead targets the real containers responsible for the
     * overflow:
     *
     * - `#baseDiv` is the ONE wrapper every single page template uses
     *   (index.htm, watch.html, channelpage.htm, search-generic-page.htm,
     *   playlists.htm, my_videos.htm, videos.htm, channels.htm all open
     *   with `<div id="baseDiv">`) and it's hard-locked to `width: 960px`
     *   in www-core-*.css. Every other override here was fighting a losing
     *   battle against this one parent still being 960px wide.
     * - Homepage: `#homepage-main-content` (640px, float:left) /
     *   `#homepage-side-content` (300px, float:right), and inside those,
     *   the "Videos Being Watched Now" / featured modules use
     *   `.super-large-video` (fixed 229px, float:left) and
     *   `.normal-size-video` (60.5%, float:right) per feeditem.
     * - Watch page: `#watch-this-vid`/`#watch-this-vid-info` (640px,
     *   float:left) and `#watch-other-vids` (300px, float:right) are the
     *   *parents* of `#watch-player-div` -- fixing the player box alone
     *   wasn't enough while its float:left 640px-wide parent was still
     *   sitting inside the 960px #baseDiv.
     * - Channel pages: `.left-column`/`.right-column` (300px/640px floats,
     *   assets/site-assets/www-channel_new-*.css).
     * - Grid/list video listings (search results, /videos, channel videos,
     *   recommended feeds): cells are `.video-cell`/`.channel-cell`/etc,
     *   sized via `display:inline-block` + a CSS percentage (e.g. 24.9%)
     *   that's sometimes further overridden per-instance with an inline
     *   `style="width:19.5%"`/`"24.5%"` (back/yt2009templates.js) -- forced
     *   to `width: 100% !important` here, which also wins over those
     *   inline non-important styles per the CSS cascade. In list-view mode
     *   the text column next to the thumbnail (`.video-main-content` and
     *   siblings) is separately fixed at `width: 456px; float: left`,
     *   which needed its own override.
     * - `#search-options-container` (the sort/upload-date/type filter bar
     *   on search results) is a real `<table>` with side-by-side `<td>`s;
     *   collapsed to block display so it stacks instead of overflowing.
     *
     * Pages not read closely for this pass (playlists.htm, my_videos.htm,
     * inbox.htm, quicklist.htm, subscriptions.htm, comment threads, the
     * account/settings pages) get a non-destructive safety net at the very
     * end: nothing inside #baseDiv is allowed to be wider than the
     * viewport, full stop. It only clamps elements that would otherwise
     * overflow -- anything already narrower keeps its natural size -- so
     * it can't make a correctly-sized element worse, only stop an
     * undiscovered fixed-width element from pushing the page wider than
     * the screen the way #baseDiv itself was doing before this fix.
     */
    private val mobileCss = """
        html { -webkit-text-size-adjust: 100%; }
        body {
            width: 100% !important;
            min-width: 0 !important;
            overflow-x: hidden !important;
        }

        /* the one wrapper every page template shares, hard-locked to
           960px in yt2009's CSS -- see the class kdoc above */
        #baseDiv, #content, #alerts, #promos, #footer, #copyright,
        #masthead-container, #masthead, #search-section-header {
            width: 100% !important;
            max-width: 100% !important;
            min-width: 0 !important;
            margin-left: 0 !important;
            margin-right: 0 !important;
            box-sizing: border-box !important;
        }

        /* every fixed-width/percentage float-based two-column or
           multi-column layout found in yt2009's own CSS -- stack
           vertically instead of overflowing sideways. see the class kdoc
           above for exactly which file/selector each of these came from */
        #homepage-main-content, #homepage-side-content,
        #watch-this-vid, #watch-this-vid-info, #watch-other-vids,
        .watch-tabs,
        .left-column, .right-column,
        .super-large-video, .normal-size-video, .feeditem-compressed,
        .feedmodule-thumbnail,
        .feeditem-bigthumb .feedmodule-singleform-info,
        .feeditem-compressed .feedmodule-singleform-info,
        .video-main-content, .channel-main-content, .playlist-main-content,
        .movie-main-content, .trailer-main-content, .show-main-content,
        #logo, #masthead-search, #masthead-qr, #masthead-utility,
        #masthead-nav-main, #masthead-nav-user {
            float: none !important;
            display: block !important;
            width: 100% !important;
            max-width: 100% !important;
            margin-left: 0 !important;
            margin-right: 0 !important;
            box-sizing: border-box !important;
        }
        /* #masthead itself got squeezed from 960px down to mobile width,
           but #logo/#masthead-search (float:left) and #masthead-nav-user
           (float:right) -- all sized/positioned for the original 960px bar
           -- were untouched by that alone, so they collided/overlapped
           instead of reflowing. unfloating the whole masthead's direct
           children (above) fixes that, but #masthead-nav-main/-user pack
           several links onto one line via inline margins meant for a wide
           bar -- let those links wrap onto their own lines too */
        #masthead-nav-main a, #masthead-nav-user a {
            display: inline-block !important;
            margin: 2px 8px 2px 0 !important;
        }

        /* grid/list video listing cells: inline-block percentage widths
           (sometimes set inline per-instance) -- forced full width so they
           stack one per row instead of cramming down to illegible slivers */
        .video-cell, .channel-cell, .playlist-cell, .movie-cell,
        .show-cell, .trailer-cell {
            display: block !important;
            width: 100% !important;
            max-width: 100% !important;
            box-sizing: border-box !important;
        }

        /* the search results sort/filter bar is a real <table> with
           side-by-side <td>s */
        #search-options-container, #search-options-container tbody,
        #search-options-container tr, #search-options-container td {
            display: block !important;
            width: auto !important;
        }

        img {
            max-width: 100% !important;
            height: auto !important;
        }
        table { max-width: 100% !important; }

        /* the html5 <video> lives inside #watch-player-div; html5-player.js
           sets explicit pixel width/height on it via inline styles, these
           !important rules win over that per CSS cascade rules and keep it
           locked to a responsive 16:9 box regardless.

           IMPORTANT: only the actual video/object/embed element gets this
           treatment, never every direct div child of the container as a
           group. html5-player.js appends its own JS-managed overlay divs directly
           into #watch-player-div too (.annotations_container is literally
           `mainElement.appendChild(ac)` where mainElement IS
           #watch-player-div, positioned via precise pixel math + a
           transform:scale() the JS computes itself; there's very likely an
           end-screen/related-videos overlay doing the same). An earlier
           version of this rule forced *every* direct div child -- video
           element included or not -- to fill the frame at 100%/100%,
           which stomped those JS-computed positions and shoved whatever
           overlay div came last (looked like an unloaded thumbnail grid)
           full-size on top of the actual video. Their own JS reads the
           container's real (now-responsive) computed size dynamically, so
           leaving them alone here is enough for them to behave. */
        #watch-player-div {
            position: relative !important;
            width: 100% !important;
            max-width: 100% !important;
            height: 0 !important;
            padding-top: 56.25% !important;
            box-sizing: border-box !important;
            overflow: hidden !important;
        }
        #watch-player-div video,
        #watch-player-div object,
        #watch-player-div embed {
            position: absolute !important;
            top: 0 !important;
            left: 0 !important;
            width: 100% !important;
            height: 100% !important;
        }

        #masthead-search-term, .search-term, input[name="search_query"], input[name="q"] {
            width: 55vw !important;
            max-width: 340px !important;
            font-size: 16px !important;
        }
        input, textarea, select, button, .yt-uix-button, a.yt-uix-button {
            font-size: 15px !important;
        }
        a, button, .yt-uix-button, input[type="submit"], input[type="button"] {
            min-height: 30px;
        }

        /* non-destructive safety net for pages/elements not specifically
           targeted above -- see the class kdoc for why this is here.
           max-width only *clamps*, it never forces an element wider, so
           this can't make anything worse than it already was */
        #baseDiv * {
            max-width: 100vw !important;
        }
    """.trimIndent()

    /**
     * Returns the JS snippet to run (via evaluateJavascript) after every
     * page load. It is idempotent -- safe to run repeatedly on the same
     * document (e.g. re-run after an in-page navigation).
     */
    fun buildInjectionScript(config: ServerConfig): String {
        val cssJson = JSONObject.quote(mobileCss)
        val flagCookie = flagCookieValue(config)
        val cookieJson = JSONObject.quote("maytube_flags=$flagCookie; Path=/; Max-Age=63072000")
        return """
            (function() {
                try {
                    // re-assert the SABR/quality flag cookie from inside the
                    // page too, in case anything on this page rewrites
                    // document.cookie before making its own requests
                    document.cookie = $cookieJson;
                } catch (e) {}

                try {
                    var vp = document.querySelector('meta[name="viewport"]');
                    if (!vp) {
                        vp = document.createElement('meta');
                        vp.name = 'viewport';
                        document.head.appendChild(vp);
                    }
                    vp.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=5.0');
                } catch (e) {}

                try {
                    var STYLE_ID = 'maytube-mobile-css';
                    var existing = document.getElementById(STYLE_ID);
                    if (existing) existing.parentNode.removeChild(existing);
                    var style = document.createElement('style');
                    style.id = STYLE_ID;
                    style.type = 'text/css';
                    style.appendChild(document.createTextNode($cssJson));
                    document.head.appendChild(style);
                } catch (e) {}

                return true;
            })();
        """.trimIndent()
    }

    /**
     * Diagnostic-only hook for the SABR playback pipeline: patches
     * XMLHttpRequest.prototype.open so every request to /sabr_playback
     * (that's what html5-player.js's requestSabr() uses -- a raw
     * XMLHttpRequest, not fetch) logs its outcome via console.log, which
     * MaytubeWebChromeClient.onConsoleMessage then forwards to logcat
     * (debug builds only) under the tag "MaytubeWebConsole", prefixed
     * "[maytube-sabr]". This exists so SABR/MSE playback issues -- which
     * show up on-screen as nothing more informative than a stuck 0:00/0:00
     * -- can be diagnosed from the device alone:
     *
     *   su -c "logcat -s MaytubeWebConsole"
     *
     * No PC/chrome://inspect needed. Must run before html5-player.js's own
     * script executes, so this is injected via onPageStarted rather than
     * onPageFinished (see MaytubeWebViewClient) -- unlike the CSS/cookie
     * injection in buildInjectionScript, this doesn't touch the DOM at all
     * so it's safe to run before the document has a <head>/<body>.
     */
    fun buildSabrDiagnosticScript(): String {
        return """
            (function() {
                if (window.__maytubeSabrHooked) return;
                window.__maytubeSabrHooked = true;
                try {
                    var OrigOpen = XMLHttpRequest.prototype.open;
                    XMLHttpRequest.prototype.open = function(method, url) {
                        if (typeof url === 'string' && url.indexOf('sabr_playback') !== -1) {
                            var self = this;
                            var startedAt = Date.now();
                            this.addEventListener('loadend', function() {
                                try {
                                    console.log('[maytube-sabr] ' + method + ' ' + url
                                        + ' status=' + self.status
                                        + ' parts=' + self.getResponseHeader('x-part-count')
                                        + ' itag=' + self.getResponseHeader('x-yt2009-used-itag')
                                        + ' mime=' + self.getResponseHeader('x-yt2009-video-mime')
                                        + ' redirect=' + self.getResponseHeader('x-yt2009-got-internal-redirect')
                                        + ' bytes=' + (self.response && self.response.byteLength)
                                        + ' ' + (Date.now() - startedAt) + 'ms');
                                } catch (e) {
                                    console.log('[maytube-sabr] log error: ' + e);
                                }
                            });
                        }
                        return OrigOpen.apply(this, arguments);
                    };
                    console.log('[maytube-sabr] request/response logging active');
                } catch (e) {
                    console.log('[maytube-sabr] failed to hook XMLHttpRequest: ' + e);
                }
            })();
        """.trimIndent()
    }

    /**
     * Extracts a yt2009/YouTube style 11-char video id from a watch page
     * URL (?v=... on /watch, /mobile/watch, /embed/, etc). Returns null if
     * the current page isn't a watch page.
     */
    fun extractVideoId(url: String?): String? {
        if (url == null) return null
        val match = Regex("[?&]v=([a-zA-Z0-9_-]{6,20})").find(url)
            ?: Regex("/embed/([a-zA-Z0-9_-]{6,20})").find(url)
        val raw = match?.groupValues?.get(1) ?: return null
        return raw.take(11)
    }
}
