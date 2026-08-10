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

    /** CSS injected on every page to reflow the fixed 960px desktop layout. */
    private val mobileCss = """
        html { -webkit-text-size-adjust: 100%; }
        body {
            width: 100% !important;
            min-width: 0 !important;
            overflow-x: hidden !important;
        }
        #page, #content, #masthead-container, #masthead, #body-column,
        #homepage-main-content, #homepage-side-content, #footer,
        .watch-video-info, #browseMain, .yt-uix-line-wrap-container {
            width: 100% !important;
            max-width: 100% !important;
            min-width: 0 !important;
            box-sizing: border-box !important;
        }
        /* two-column desktop layouts (homepage, channel, watch sidebar)
           stack vertically instead of overflowing sideways */
        #homepage-main-content, #homepage-side-content,
        #main-channel-left, #main-channel-right,
        #watch-sidebar, #watch7-sidebar, .watch-sidebar {
            float: none !important;
            display: block !important;
            width: 100% !important;
        }
        img {
            max-width: 100% !important;
            height: auto !important;
        }
        table { max-width: 100% !important; }
        /* horizontally scroll anything we didn't explicitly reflow instead
           of letting it force the whole page wider than the screen */
        #content > table, #content > div {
            max-width: 100vw;
        }
        /* the html5 <video> lives inside #watch-player-div; html5-player.js
           sets explicit pixel width/height on it via inline styles, these
           !important rules win over that per CSS cascade rules and keep it
           locked to a responsive 16:9 box regardless */
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
        #watch-player-div embed,
        #watch-player-div > div {
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
        /* thumbnails/listings: let flex-ish rows wrap instead of clipping */
        .video-list, .yt-lockup, .browse-list, #list-view, #list-pane {
            width: 100% !important;
            max-width: 100% !important;
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
