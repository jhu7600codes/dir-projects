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
     * maytube's own wordmark (red rounded-square play glyph + "maytube"),
     * replacing yt2009's YouTube sprite crop at #logo. Plain inline SVG,
     * base64'd so it can drop straight into a CSS `url(data:...)` without
     * escaping quotes/whitespace for embedding in a raw string. No separate
     * dark-mode variant needed -- #logo is a background-image, not an
     * img/video/canvas/iframe tag, so it isn't one of the elements the
     * dark-mode rule below re-inverts back to normal; it inverts once along
     * with the rest of the page, same as every other sprite/icon here,
     * which is the correct behavior for flat graphic content (unlike
     * photographic img/video content, which would look wrong inverted).
     */
    private const val MAYTUBE_LOGO_SVG_BASE64 =
        "PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxODAiIGhlaWdodD0iNTQiIHZpZXdCb3g9IjAgMCAxODAgNTQiPgogIDxyZWN0IHg9IjIiIHk9IjEyIiB3aWR0aD0iMzAiIGhlaWdodD0iMzAiIHJ4PSI4IiBmaWxsPSIjQ0MxQjFCIi8+CiAgPHBvbHlnb24gcG9pbnRzPSIxMywyMCAxMywzNCAyNSwyNyIgZmlsbD0iI2ZmZmZmZiIvPgogIDx0ZXh0IHg9IjQwIiB5PSIzNiIgZm9udC1mYW1pbHk9IkhlbHZldGljYSwgQXJpYWwsIHNhbnMtc2VyaWYiIGZvbnQtc2l6ZT0iMjYiIGZvbnQtd2VpZ2h0PSI3MDAiIGZpbGw9IiMxYTFhMWEiIGxldHRlci1zcGFjaW5nPSItMC41Ij5tYXl0dWJlPC90ZXh0Pgo8L3N2Zz4K"

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
        #watch-longform-buttons {
            float: none !important;
            display: block !important;
            width: 100% !important;
            max-width: 100% !important;
            margin-left: 0 !important;
            margin-right: 0 !important;
            box-sizing: border-box !important;
        }

        /* keep the masthead reachable on long pages without needing to
           scroll back up -- layout-only (no color changes here,
           deliberately: dark mode is handled globally by the invert filter
           below rather than per-element color overrides). */
        #masthead-container {
            position: sticky !important;
            top: 0 !important;
            z-index: 1000 !important;
        }

        /* #masthead: reported directly from a real device as unacceptably
           tall -- the previous approach (unfloating every child into its
           own full-width block) turned yt2009's original single-row 960px
           bar into FIVE stacked rows (logo, search, account links,
           quicklist/subs/history/upload, home/videos/channels) before any
           actual page content even started. Real app headers are one
           compact line; rebuilt as one here instead of trying to reflow
           yt2009's own desktop nav sprawl into something that still reads
           as one desktop-era menu bar just turned sideways.
           #logo + the search form share the one line as a flex row;
           everything else in the masthead (account links, quicklist/
           subscriptions/history/upload, home/videos/channels) is hidden --
           not lost functionality, just not permanently taking up header
           space on every single page: channels are reached by tapping a
           channel name, related/recommended videos are reached by tapping
           a video, same as the rest of a normal video-watching flow. */
        #masthead {
            display: flex !important;
            align-items: center !important;
            flex-wrap: nowrap !important;
            gap: 10px !important;
            padding: 8px 10px !important;
            box-sizing: border-box !important;
            width: 100% !important;
        }
        #masthead-utility, #masthead-nav-main, #masthead-nav-user,
        #masthead-qr, #masthead-end {
            display: none !important;
        }

        /* #logo is NOT a plain element -- it's a .master-sprite button, a
           fixed-size crop window onto one shared sprite sheet image tiled
           with `background: ... repeat-x` (www-core-*.css). Its real width
           (110px, #masthead #logo in that same file) IS the crop window:
           widening it (e.g. via a naive width:100%) would reveal the
           sprite tiling itself -- multiple repeated copies side by side.
           Moot now that #logo's background is maytube's own single-image
           wordmark (below) rather than a sprite crop, but flex:0 0 auto
           (never grow/shrink to fill the row) is still exactly right for a
           logo sharing a line with a search box that should get the rest
           of the space. */
        #logo {
            flex: 0 0 auto !important;
            float: none !important;
            margin: 0 !important;
        }

        /* #masthead-search (the search box + button) fills whatever space
           #logo doesn't use; .search-form (its real child, watch.html) is
           itself made a flex row so the text input can grow to fill that
           while the "Search" button/link keeps its own natural width
           beside it, instead of the two stacking on separate lines. */
        #masthead-search {
            flex: 1 1 auto !important;
            float: none !important;
            display: block !important;
            width: auto !important;
            min-width: 0 !important;
            margin: 0 !important;
            box-sizing: border-box !important;
        }
        .search-form {
            display: flex !important;
            align-items: center !important;
            gap: 6px !important;
            width: 100% !important;
        }
        #masthead-search-term {
            flex: 1 1 auto !important;
            width: auto !important;
            min-width: 0 !important;
            max-width: none !important;
            box-sizing: border-box !important;
        }

        /* #logo: swap yt2009's YouTube sprite crop for maytube's own
           wordmark (see MAYTUBE_LOGO_SVG_BASE64's kdoc above). Overrides
           every property .master-sprite's shared rule sets for it
           (background-image/repeat/position all come from that one
           shared class -- www-core-*.css) rather than relying on it. */
        #logo {
            background-image: url(data:image/svg+xml;base64,$MAYTUBE_LOGO_SVG_BASE64) !important;
            background-repeat: no-repeat !important;
            background-position: left center !important;
            background-size: contain !important;
            width: 150px !important;
            height: 45px !important;
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
            border-radius: 10px !important;
            padding: 12px !important;
            margin: 0 0 16px !important;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.18) !important;
        }

        /* Thumbnails: yt2009's own thumbnail wrappers are hard-locked to
           tiny fixed pixel boxes sized for the original dense desktop grid
           (back/yt2009templates.js's real markup + www-core CSS):
           .v120WrapperOuter/.v90WrapperOuter (122px/92px) contain
           .v120WrapperInner/.v90WrapperInner (120x72px/90x54px,
           overflow:hidden), which contain either a .video-thumb-link
           anchor or the <img class="vimg120|vimg90"> directly. The generic
           `img { max-width:100% }` rule near the top of this file already
           lets .vimg120/.vimg90 grow past their own hardcoded 120x90/90x70
           pixel size, but these WRAPPING divs' fixed pixel dimensions +
           overflow:hidden clip the image right back down regardless --
           that's the actual reason thumbnails still read as small even
           with the general img rule in place.

           Two wrapper shapes exist across yt2009's ~8 different listing
           templates (search/homepage/recommended use WrapperOuter+Inner;
           channel/playlist listings use .video-thumb-link/.video-thumb-90/
           .video-thumb-120 directly, no extra wrapper divs) -- both
           covered. The *WrapperOuter (or .video-thumb-link/90/120 when
           used standalone) becomes a real 16:9 box; nested descendants
           inside a *WrapperOuter fill that box at 100%/100% rather than
           re-establishing their own aspect-ratio box, since doing that at
           more than one nested level would compound instead of just
           fitting inside the established box. */
        .v120WrapperOuter, .v90WrapperOuter,
        .video-thumb-link, .video-thumb-90, .video-thumb-120 {
            display: block !important;
            position: relative !important;
            width: 100% !important;
            height: 0 !important;
            padding-top: 56.25% !important;
            overflow: hidden !important;
            border: 0 !important;
            border-radius: 6px !important;
        }
        .v120WrapperOuter .v120WrapperInner, .v90WrapperOuter .v90WrapperInner,
        .v120WrapperOuter .video-thumb-link, .v90WrapperOuter .video-thumb-link {
            position: absolute !important;
            top: 0 !important;
            left: 0 !important;
            width: 100% !important;
            height: 100% !important;
            padding-top: 0 !important;
            border: 0 !important;
            margin: 0 !important;
        }
        .vimg90, .vimg120,
        .video-thumb-link img, .video-thumb-90 img, .video-thumb-120 img {
            position: absolute !important;
            top: 0 !important;
            left: 0 !important;
            width: 100% !important;
            height: 100% !important;
            margin-top: 0 !important;
            object-fit: cover !important;
        }
        /* .video-time (the duration badge, e.g. "23:38"): yt2009's own
           positioning is a negative top margin (-15px) calibrated to pull
           it up over the bottom-right corner of the original tiny 72px/54px
           -tall thumbnail -- doesn't reach nearly far enough up a proper
           16:9 box at mobile width. Its containing block is now one of the
           positioned elements above (.v120WrapperInner or the *WrapperOuter
           itself), so absolute positioning against that same box places it
           correctly regardless of the thumbnail's now much larger size. */
        .video-time, .video-corner-text {
            position: absolute !important;
            right: 4px !important;
            bottom: 4px !important;
            margin: 0 !important;
            z-index: 2 !important;
        }

        /* .video-short-title (the title link under each thumbnail in a
           listing, back/yt2009templates.js): www-core CSS locks it to a
           fixed height:30px + overflow:hidden, calibrated to whatever
           small font-size the original 120px-wide desktop cells used --
           bumping the font size without clearing that height would just
           clip the now-larger text instead of making it more readable. */
        .video-short-title, .playlist-short-title {
            height: auto !important;
            max-height: none !important;
            overflow: visible !important;
            font-size: 16px !important;
            line-height: 1.35 !important;
            margin: 10px 0 5px !important;
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

        /* #watch-vid-title (watch.html/www-core CSS): the ACTUAL root cause
           of the "giant one-word-per-line title" bug, found by finally
           reading the real rule instead of guessing again --
           `#watch-vid-title.longform { margin-right: 320px; }`, and the
           real markup does carry that exact class
           (`<div id="watch-vid-title" class="title  longform">`). 320px of
           right margin was there to leave room for the original layout's
           300px-wide right rail; on a ~380-400px mobile viewport it leaves
           the title maybe 60-80px of actual width, which wraps even
           correctly-sized text one word per line. Nothing about font-size
           was ever actually wrong -- the box itself was being squeezed
           down to almost nothing, and the earlier font-size-only fix
           (still correct and kept below) could never have fixed that on
           its own. The existing #baseDiv * safety net doesn't catch this
           class of bug either: it only clamps max-width, and a large
           margin doesn't make an element wider than the viewport, just
           narrower than it should be. */
        #watch-vid-title {
            width: 100% !important;
            margin-right: 0 !important;
            box-sizing: border-box !important;
        }

        /* .watch-vid-ab-title: kept as a normal sizing rule (correct, just
           not sufficient on its own -- see #watch-vid-title above) and
           reinforced with a belt-and-suspenders inline-style version in
           buildInjectionScript below, since a differently-versioned
           instance's own CSS could in principle size #watch-vid-title h1
           with a rule specific enough to beat a plain class selector. */
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

        /* #watch-video-details (wraps the description + URL/embed boxes
           below): no background/border of its own in yt2009's CSS, just
           margin/padding on its inner pieces -- card-ifying the whole
           thing to match #watch-channel-vids-div/.video-cell above,
           instead of description text and the URL/embed inputs just
           sitting directly on the page background one after another. */
        #watch-video-details {
            background: #fff !important;
            border-radius: 8px !important;
            padding: 4px 12px 12px !important;
            margin-top: 10px !important;
            box-sizing: border-box !important;
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

        /* #masthead-search-term deliberately excluded here -- it's flex-sized
           within the one-line masthead now (see the #masthead rule above),
           and this rule's width would win the cascade tie (same specificity,
           this rule comes later) if left in, undoing that. Other pages'
           search boxes (results page etc, .search-term/name=... but no
           #masthead-search-term id) are unaffected. */
        .search-term, input[name="search_query"], input[name="q"] {
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

                // "Unlimited" homepage recommendations: yt2009's own
                // assets/site-assets/homepage-recommended.js already fetches
                // /yt2009_recommended once on page load (targetVideos=8, real
                // server code, back/backend.js) into
                // #yt2009-recommended-cells-container -- and, separately, the
                // real /videos page fetches the exact same endpoint with one
                // extra request header (`source: recommended_page`) that bumps
                // the server's targetVideos to 25 instead. Both mechanisms
                // already exist in yt2009 itself; this replicates that same
                // request (same "ids" header, computed from watch_history the
                // same way homepage-recommended.js does) but always sends that
                // header, and re-fires it every time the user scrolls near the
                // bottom of the module, appending only videos not already
                // shown (by data-id) instead of replacing the container's
                // content the way the original single-shot call does. Stops
                // auto-firing after two consecutive empty/all-duplicate
                // responses, so this can't spin forever once the server
                // genuinely has nothing further to suggest.
                try {
                    var recContainer = document.getElementById('yt2009-recommended-cells-container');
                    if (recContainer && !recContainer.__maytubeInfiniteRec) {
                        recContainer.__maytubeInfiniteRec = true;

                        var maytubeRecIds = function() {
                            var ids = '';
                            try {
                                if (window.localStorage && localStorage.watch_history) {
                                    var videos = JSON.parse(localStorage.watch_history);
                                    videos.slice(0, 3).forEach(function(v) { ids += v.id + ','; });
                                } else if (document.cookie && document.cookie.indexOf('watch_history=') !== -1) {
                                    var h = decodeURIComponent(
                                        document.cookie.split('watch_history=')[1].split(';')[0]
                                    ).split(':');
                                    var count = 0;
                                    for (var i = 0; i < h.length && count < 3; i++) {
                                        if (typeof h[i] === 'string' && h[i].indexOf('&') !== -1) {
                                            ids += h[i].split('&')[2] + ',';
                                            count++;
                                        }
                                    }
                                }
                            } catch (e) {}
                            return ids;
                        };

                        var maytubeRecLoading = false;
                        var maytubeRecEmptyStreak = 0;
                        var maytubeRecDone = false;

                        var maytubeLoadMoreRec = function() {
                            if (maytubeRecLoading || maytubeRecDone) return;
                            maytubeRecLoading = true;
                            var r = new XMLHttpRequest();
                            r.open('GET', '/yt2009_recommended?r=' + Math.random());
                            r.setRequestHeader('ids', maytubeRecIds());
                            // yt2009's own /videos-page trick for 25 results instead of 8
                            r.setRequestHeader('source', 'recommended_page');
                            r.onreadystatechange = function() {
                                if (r.readyState !== 4) return;
                                maytubeRecLoading = false;
                                if (r.status < 200 || r.status >= 300
                                || r.responseText.indexOf('YT2009_NO_DATA') !== -1) {
                                    maytubeRecDone = true;
                                    return;
                                }
                                try {
                                    var existingIds = {};
                                    var existingCells = recContainer.querySelectorAll('[data-id]');
                                    for (var i = 0; i < existingCells.length; i++) {
                                        existingIds[existingCells[i].getAttribute('data-id')] = true;
                                    }
                                    var temp = document.createElement('div');
                                    temp.innerHTML = r.responseText;
                                    var newCells = temp.querySelectorAll('[data-id]');
                                    var addedAny = false;
                                    for (var j = 0; j < newCells.length; j++) {
                                        var cell = newCells[j];
                                        var id = cell.getAttribute('data-id');
                                        if (id && !existingIds[id]) {
                                            existingIds[id] = true;
                                            recContainer.appendChild(cell);
                                            addedAny = true;
                                        }
                                    }
                                    maytubeRecEmptyStreak = addedAny ? 0 : maytubeRecEmptyStreak + 1;
                                    if (maytubeRecEmptyStreak >= 2) maytubeRecDone = true;
                                } catch (e) {
                                    maytubeRecDone = true;
                                }
                            };
                            try {
                                r.send(null);
                            } catch (e) {
                                maytubeRecLoading = false;
                                maytubeRecDone = true;
                            }
                        };

                        window.addEventListener('scroll', function() {
                            if (maytubeRecDone || maytubeRecLoading) return;
                            var scrollBottom = window.innerHeight + window.scrollY;
                            if (scrollBottom >= document.body.scrollHeight - 800) {
                                maytubeLoadMoreRec();
                            }
                        }, { passive: true });
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
