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
            /* light neutral instead of yt2009's own plain white -- lets the
               white cards (.video-cell etc, and #watch-channel-vids-div
               below) actually read as distinct cards instead of blending
               into an identically-white page. Dark mode's invert filter
               (bottom of this file) handles this automatically, same as
               everything else -- no separate dark-mode value needed. */
            background: #f1f1f1 !important;
        }
        /* yt2009's own font stack (www-core-feather.css:
           `body,input,textarea {font: 12px Arial, sans-serif}`) is Arial --
           the same default system font most devices already render, so
           swapping in the OS's own UI font stack reads as "app", not
           "unstyled webpage", for free. */
        body, input, textarea, select, button {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif !important;
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

        /* page-wide gutter: yt2009 has zero side padding anywhere, so
           reflowed content sits flush against the screen edges -- cramped
           and distinctly "raw HTML", not "app". #baseDiv specifically (not
           #masthead-container, which stays full-bleed as its own header
           bar) gets the inset, box-sizing:border-box above means it can't
           cause overflow.
           This is also *the* mechanism behind "shrink the player, don't
           touch anything else about it": every element from here down to
           #watch-player-div is set to width:100% of its own direct parent
           (see the float-based layout bucket below), so narrowing the top
           of that chain narrows every descendant in it by the same
           percentage math, all the way down -- including
           #watch-player-div's own 56.25% aspect-ratio box, since that
           percentage is resolved against ITS parent's (now-narrower)
           width. The player container and its video/object/embed rules
           further down are completely untouched; they just end up
           rendering into a narrower box, exactly like every other element
           on the page now does. */
        #baseDiv {
            padding: 0 16px !important;
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
        #masthead-search, #masthead-utility,
        #masthead-nav-main, #masthead-nav-user,
        #watch-longform-buttons {
            float: none !important;
            display: block !important;
            width: 100% !important;
            max-width: 100% !important;
            margin-left: 0 !important;
            margin-right: 0 !important;
            box-sizing: border-box !important;
        }
        /* #masthead itself got squeezed from 960px down to mobile width,
           but #masthead-search (float:left) and #masthead-nav-user
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

        /* keep search/nav reachable on long pages without needing to scroll
           back up -- layout-only (no color changes here, deliberately: the
           masthead's #logo is a cropped sprite image tuned for yt2009's own
           original background, and dark mode is handled globally by the
           invert filter below rather than per-element color overrides, so
           hardcoding a background here would fight both of those). */
        #masthead-container {
            position: sticky !important;
            top: 0 !important;
            z-index: 1000 !important;
        }

        /* #logo (and #masthead-qr, unused in current markup but harmless
           to cover) are NOT plain elements -- they're .master-sprite
           buttons, a fixed-size crop window onto one shared sprite sheet
           image tiled with `background: ... repeat-x` (www-core-*.css).
           #logo's real width (110px, #masthead #logo in that same file)
           IS the crop window: it's what keeps only one copy of the
           YouTube wordmark visible. The rule above forcing every masthead
           child to width:100% included #logo initially, which widened
           that crop window across the whole mobile viewport and revealed
           the sprite tiling itself -- multiple repeated copies of the
           logo image side by side. Sprite/icon elements get unfloated
           like everything else, but must keep their real (small,
           intrinsic) width, never forced to 100%. */
        #logo, #masthead-qr {
            float: none !important;
            display: block !important;
            margin: 4px 0 !important;
        }

        /* grid/list video listing cells: inline-block percentage widths
           (sometimes set inline per-instance) -- forced full width so they
           stack one per row instead of cramming down to illegible slivers.
           Also given actual card chrome (background/radius/shadow/spacing)
           -- yt2009's own CSS gives these cells no visual separation of
           their own, so a stacked column of them reads as one undifferentiated
           wall of text+thumbnails rather than a list of distinct, tappable
           items. Purely additive (background/radius/padding/margin/shadow),
           doesn't touch the sizing rules above it. */
        .video-cell, .channel-cell, .playlist-cell, .movie-cell,
        .show-cell, .trailer-cell {
            display: block !important;
            width: 100% !important;
            max-width: 100% !important;
            box-sizing: border-box !important;
            background: #fff !important;
            border-radius: 8px !important;
            padding: 8px !important;
            margin: 0 0 10px !important;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15) !important;
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
            /* was overflow:hidden -- clipped things like the on-pause
               related-videos overlay, which (per the original 640x390
               fixed layout) can extend past the video's own bounds.
               visible can't cause horizontal page overflow on its own
               (that needs a wider box, not an unclipped one), and the
               #baseDiv * safety net below still catches anything that
               genuinely is too wide. */
            overflow: visible !important;
            background: #000 !important;
        }
        #watch-player-div video,
        #watch-player-div object,
        #watch-player-div embed {
            position: absolute !important;
            top: 0 !important;
            left: 0 !important;
            width: 100% !important;
            height: 100% !important;
            /* object-fit's initial value is "fill" -- without this, a
               video whose real aspect ratio isn't exactly 16:9 (this box's
               ratio) gets stretched/cropped to fill it exactly instead of
               letterboxed. contain fits the whole frame inside the box,
               adding black bars (the background above) instead of cutting
               anything off. */
            object-fit: contain !important;
        }

        /* yt2009's own CSS-driven fullscreen fallback (nbedit_style.css's
           .fullscreen-unsupported, forced via the requestFullscreen patch in
           buildInjectionScript below) needs #watch-player-div to actually
           cover the viewport when html5-player.js adds that class -- but the
           responsive-embed rules directly above (also !important, and a bare
           ID selector) would otherwise still win: #watch-player-div is an ID
           selector (specificity 100), nbedit_style.css's own
           `.fullscreen-unsupported { position: absolute !important; ... }`
           is a class selector (specificity 10) -- with both !important, the
           higher-specificity ID rule wins per the cascade, silently keeping
           the player locked to its normal in-page aspect-ratio box no matter
           what class html5-player.js adds. This is the actual reason
           "fullscreen" didn't visibly do anything: not a WebView platform
           limitation, a specificity conflict with this file's own earlier
           rules.
           #watch-player-div.fullscreen-unsupported (ID+class, specificity
           110) has higher specificity than plain #watch-player-div, so this
           correctly overrides it. Uses position:fixed + explicit viewport
           units rather than nbedit_style.css's own position:absolute --
           robust regardless of #baseDiv's own padding (see the page-gutter
           rule above) or scroll position, neither of which nbedit_style.css
           had to account for since it was never fighting an ID-selector
           override to begin with. z-index is deliberately far above both
           nbedit_style.css's own z-index:99 for this class *and* the sticky
           #masthead-container above (z-index:1000) -- that masthead would
           otherwise render on top of the fullscreen video once it's actually
           correctly positioned. */
        #watch-player-div.fullscreen-unsupported {
            position: fixed !important;
            top: 0 !important;
            left: 0 !important;
            width: 100vw !important;
            height: 100vh !important;
            max-width: 100vw !important;
            padding-top: 0 !important;
            margin: 0 !important;
            z-index: 999999 !important;
        }

        /* #masthead-container is a full-width header bar OUTSIDE
           #watch-player-div's own DOM subtree (a sibling under #baseDiv,
           not a descendant of the player) -- its sticky position and
           z-index:1000 (see the masthead rule above) still render it on top
           of the fullscreen video in practice, because it's not actually
           competing with #watch-player-div's z-index:999999 within the same
           stacking context once ancestor elements are involved. Hiding it
           outright while fake-fullscreen is active sidesteps that stacking
           fight entirely, and matches what real fullscreen should look like
           anyway -- no page chrome visible. Toggled on <html> (not #baseDiv
           itself, which nbedit_style.css's own JS already repurposes for
           the fullscreen-unsupported class) by the same MutationObserver
           that drives MaytubeFullscreenBridge, in buildInjectionScript below. */
        html.maytube-pseudo-fullscreen #masthead-container {
            display: none !important;
        }

        /* .watch-vid-ab-title (watch.html/back/yt2009html.js): reported
           directly from a real device rendering *enormous* (each word of a
           long title wrapping onto its own full-width line). This class
           selector rule alone turned out not to be reliable across
           instances -- some yt2009 deployments' own CSS apparently sizes
           #watch-vid-title h1 with real, if desktop-sized, rules of its
           own (an ID+tag selector, which under different
           versioning/!important combinations across instances isn't
           guaranteed to lose to a plain class selector the way it does on
           the one version this was originally checked against). Kept here
           as a normal stylesheet rule for anything that reads it before
           the belt-and-suspenders inline-style version below applies (it's
           already correct on instances where this class rule alone was
           always enough), but the buildInjectionScript JS below is what
           actually guarantees this now, regardless of instance. */
        .watch-vid-ab-title {
            font-size: 17px !important;
            line-height: 1.3 !important;
            margin: 6px 0 !important;
            font-weight: bold !important;
        }

        /* #watch-longform-buttons: yt2009's own desktop-only "change player
           size" / "popout" icon buttons (www-core CSS: float:right, meant
           to sit beside the title on a 960px page). Reported directly from
           a real device rendering oddly displaced up near the masthead once
           unfloated into the single mobile column below -- and neither
           button does anything useful in a mobile WebView shell to begin
           with (there's no separate window to pop the video out into, and
           the app already sizes the player responsively without a manual
           toggle). Simplest and safest fix for both problems at once:
           don't show them here at all. */
        #watch-longform-buttons {
            display: none !important;
        }

        /* watch.html's channel/description block (#watch-channel-vids-div):
           real selectors read straight from watch.html/www-core CSS, same
           as everything else in this file. Sized for the original 960px
           two-column layout (a 300px-wide right rail); none of it is
           unreadably broken the way the title/URL fields were, just cramped
           and undifferentiated at mobile width. */
        .watch-video-desc {
            font-size: 14px !important;
            line-height: 1.5 !important;
            padding: 8px 0 !important;
        }
        #watch-channel-stats {
            font-size: 13px !important;
            line-height: 1.6 !important;
        }
        #watch-channel-icon {
            width: 40px !important;
            height: 40px !important;
        }
        #watch-channel-icon img {
            width: 40px !important;
            height: 40px !important;
        }

        /* #watch-url-field/#embed_code: plain <input type="text"> holding
           the full watch URL / <object> embed snippet -- unconstrained
           desktop width overflowed the mobile viewport outright (visible
           directly on a real device: both fields showed only their first
           ~25 characters, cut off at the screen edge with the rest
           unreachable, no horizontal scroll affordance on a plain input
           wide enough to need one). */
        #watch-url-field, #embed_code {
            width: 100% !important;
            max-width: 100% !important;
            box-sizing: border-box !important;
            font-size: 12px !important;
        }
        #watch-url-div, #watch-embed-div {
            width: 100% !important;
            box-sizing: border-box !important;
        }

        /* .yt-uix-expander-head: watch.html's "Related Videos"/"More From:
           ..." collapsible section headers (h2.yt-uix-expander-head,
           shared by every yt-uix-expander panel site-wide). yt2009's own
           rule for it (www-core CSS: `.yt-uix-expander-head { cursor:
           pointer; color:#000 }`) sets no font-size/weight at all -- always
           rendered as a raw browser-default h2. A bold, slightly larger
           weight with a bottom divider reads as an actual section
           boundary instead of just another line of body text. */
        .yt-uix-expander-head {
            font-size: 15px !important;
            font-weight: bold !important;
            padding: 10px 0 8px !important;
            margin: 12px 0 8px !important;
            border-bottom: 1px solid #ddd !important;
        }

        /* #watch-channel-vids-div already has its own background/border
           (#eee/#ccc, www-core CSS) -- just rounding + insetting it to
           match the card treatment used everywhere else on the page
           (.video-cell etc, above). */
        #watch-channel-vids-div {
            border-radius: 8px !important;
            padding: 10px !important;
            box-sizing: border-box !important;
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
        /* touch-target polish: rounder corners, real padding instead of
           whatever inline sizing yt2009's own markup happens to have.
           Shape/spacing only -- no background/color, same reasoning as the
           masthead above (these often carry their own yt-button-primary/
           yt-button-urgent sprite-based styling that colors shouldn't fight). */
        a.yt-uix-button, button.yt-uix-button,
        input[type="submit"], input[type="button"] {
            border-radius: 4px !important;
            padding: 6px 14px !important;
        }

        /* yt2009's own line-height is whatever the browser default is,
           which reads tight/cramped for anything more than a line or two
           (descriptions, comments). body is always present regardless of
           which page template loaded, so this is safe everywhere. */
        body {
            line-height: 1.5 !important;
        }

        /* non-destructive safety net for pages/elements not specifically
           targeted above -- see the class kdoc for why this is here.
           max-width only *clamps*, it never forces an element wider, so
           this can't make anything worse than it already was */
        #baseDiv * {
            max-width: 100vw !important;
        }

        /* dark mode: yt2009 itself has no dark theme, and hand-overriding
           colors on a site this old (mostly inline styles, decades of
           accumulated CSS) isn't remotely tractable. Instead: invert the
           whole page, then invert actual media back a second time so
           thumbnails/video don't render with wrong colors -- the same
           trick browser reader-mode/dark-mode extensions use for sites
           that don't support it natively. Toggled by adding/removing this
           class on <html> in the injected JS below, not a media query, so
           it's independent of the device's system theme. */
        html.maytube-dark {
            filter: invert(1) hue-rotate(180deg) !important;
            background: #fff !important;
        }
        html.maytube-dark img,
        html.maytube-dark video,
        html.maytube-dark canvas,
        html.maytube-dark iframe {
            filter: invert(1) hue-rotate(180deg) !important;
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
        val darkModeJs = if (config.darkMode) "true" else "false"
        val nativePlayerJs = if (config.nativePlayer) "true" else "false"
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

                try {
                    document.documentElement.classList.toggle('maytube-dark', $darkModeJs);
                } catch (e) {}

                // Belt-and-suspenders for .watch-vid-ab-title: reported directly
                // from a real device rendering enormous (one word per line,
                // spanning the full viewport) despite the !important class rule
                // above. Some yt2009 deployments' own CSS apparently sizes
                // #watch-vid-title h1 with real rules of its own (an ID+tag
                // selector), and depending on that instance's exact CSS/versioning
                // this app has no visibility into, that isn't guaranteed to lose
                // to a plain class selector every time. An inline style set with
                // 'important' priority outranks *any* external stylesheet rule
                // regardless of selector or !important status, so this is correct
                // no matter what a given instance's CSS actually does -- no
                // guessing required.
                try {
                    var titles = document.querySelectorAll('.watch-vid-ab-title');
                    for (var i = 0; i < titles.length; i++) {
                        titles[i].style.setProperty('font-size', '17px', 'important');
                        titles[i].style.setProperty('line-height', '1.3', 'important');
                        titles[i].style.setProperty('margin', '6px 0', 'important');
                        titles[i].style.setProperty('font-weight', 'bold', 'important');
                    }
                } catch (e) {}

                // Settings > native player: playback happens in
                // PlayerActivity instead, via its own separate SABR fetch
                // (see VideoDownloader/SabrFragmentDownloader) -- letting
                // the page's own player run at the same time would just
                // waste battery/bandwidth double-fetching the same video
                // for a player the user can't even see (MainActivity hides
                // the WebView behind PlayerActivity while it's up). Stop it
                // from loading media at all, and swap in a small on-page
                // hint so the empty player box isn't just a dead black
                // rectangle. Playback itself is triggered from
                // MainActivity's "Play in native player" button, not from
                // here -- this only ever pauses/quiets the WebView's own copy.
                try {
                    if ($nativePlayerJs) {
                        var pv = document.querySelector('#watch-player-div video');
                        if (pv) {
                            pv.pause();
                            pv.removeAttribute('autoplay');
                            pv.src = '';
                            pv.load();
                        }
                        var box = document.getElementById('watch-player-div');
                        if (box && !document.getElementById('maytube-native-hint')) {
                            var hint = document.createElement('div');
                            hint.id = 'maytube-native-hint';
                            hint.textContent = 'Native player is on (Settings) -- use "Play in native player" below to watch.';
                            hint.style.cssText = 'position:absolute;inset:0;display:flex;align-items:center;justify-content:center;' +
                                'text-align:center;color:#fff;background:#000;padding:16px;box-sizing:border-box;font-size:13px;z-index:5;';
                            box.appendChild(hint);
                        }
                    }
                } catch (e) {}

                // Fullscreen: force yt2009's OWN CSS-driven "fullscreen-unsupported"
                // fallback (nbedit_style.css, loaded by watch.html: .fullscreen-unsupported
                // { position:absolute; width:100%; height:100%; z-index:99 }) instead of
                // ever letting Android WebView's real native fullscreen engage.
                //
                // WebView's HTML5 video-fullscreen path is known to drop sibling DOM
                // overlays: html5-player.js's own play/pause/seek/HD controls live in
                // sibling divs next to <video> inside #watch-player-div, not inside the
                // <video> element itself, so real native fullscreen here renders bare
                // video with zero controls -- confirmed side-by-side against Fennec,
                // which (notably) shows those same controls, meaning it likely already
                // hits this exact fallback path itself rather than truly going native.
                //
                // assets/site-assets/html5-player.js's own fullscreen button handler
                // already wraps player_element.requestFullscreen() (player_element ==
                // #watch-player-div) in try/catch specifically for this: forcing that
                // call to throw synchronously is what triggers its fallback. Its matching
                // exit path expects document.exitFullscreen() to throw the same way when
                // there's nothing real to exit -- real WebView doesn't do that on its own
                // (rejects a Promise instead, invisibly to a plain sync try/catch), so
                // that's patched too, or the page gets stuck showing the fake-fullscreen
                // CSS with no way out via the page's own button.
                try {
                    var playerDiv = document.getElementById('watch-player-div');
                    if (playerDiv && !playerDiv.__maytubeFsPatched) {
                        playerDiv.__maytubeFsPatched = true;
                        var forceUnsupported = function() {
                            throw new DOMException('maytube: forcing the yt2009 CSS fullscreen fallback', 'NotSupportedError');
                        };
                        playerDiv.requestFullscreen = forceUnsupported;
                        playerDiv.webkitRequestFullscreen = forceUnsupported;
                        playerDiv.webkitRequestFullScreen = forceUnsupported;
                    }
                } catch (e) {}

                try {
                    if (!document.__maytubeExitFsPatched) {
                        document.__maytubeExitFsPatched = true;
                        var origExitFullscreen = document.exitFullscreen ? document.exitFullscreen.bind(document) : null;
                        document.exitFullscreen = function() {
                            if (!document.fullscreenElement) {
                                throw new DOMException('maytube: nothing is really fullscreen', 'InvalidStateError');
                            }
                            return origExitFullscreen ? origExitFullscreen() : undefined;
                        };
                    }
                } catch (e) {}

                // Relay yt2009's own fullscreen-unsupported class toggle (see above)
                // back to native code so the app can still apply the same system-bar-
                // hiding/landscape-lock/keep-screen-on treatment real native fullscreen
                // would have gotten from MaytubeWebChromeClient.onShowCustomView --
                // otherwise "fullscreen" here would just be a same-size CSS overlay with
                // the Android status bar and app toolbar still sitting on top of it.
                // #baseDiv is present on every page template (see the CSS kdoc above),
                // so this is safe to attach unconditionally, not just on watch pages.
                try {
                    var baseDiv = document.getElementById('baseDiv');
                    if (baseDiv && !baseDiv.__maytubeFsObserved && window.MaytubeFullscreen) {
                        baseDiv.__maytubeFsObserved = true;
                        var wasFullscreen = baseDiv.classList.contains('fullscreen-unsupported');
                        var observer = new MutationObserver(function() {
                            var isFullscreenNow = baseDiv.classList.contains('fullscreen-unsupported');
                            if (isFullscreenNow === wasFullscreen) return;
                            wasFullscreen = isFullscreenNow;
                            // #masthead-container is a full-width header bar
                            // OUTSIDE #watch-player-div's own DOM subtree,
                            // not something the player's own z-index (however
                            // high) can reliably out-stack once ancestor
                            // stacking contexts are involved -- hiding it
                            // outright while fake-fullscreen is active sidesteps
                            // that entirely, and matches what real fullscreen
                            // should look like anyway (no page chrome visible).
                            document.documentElement.classList.toggle('maytube-pseudo-fullscreen', isFullscreenNow);
                            if (isFullscreenNow) {
                                window.MaytubeFullscreen.onEnter();
                            } else {
                                window.MaytubeFullscreen.onExit();
                            }
                        });
                        observer.observe(baseDiv, { attributes: true, attributeFilter: ['class'] });
                    }
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
