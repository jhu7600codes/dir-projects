package com.maytube.app.browse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.jsoup.Jsoup

/**
 * Regression tests for Yt2009Api's HTML parsing against real markup quoted
 * directly from yt2009's own templates (back/yt2009templates.js), not
 * reconstructed from memory -- the exact failure mode MobileInjector's
 * first draft shipped with (invented CSS selectors that matched nothing).
 * Every fixture below is copy-pasted from the actual template functions
 * with placeholder values filled in, field names/structure otherwise
 * untouched.
 */
class Yt2009ApiTest {

    private fun cell(html: String) = Jsoup.parse(html, "http://host:3000").selectFirst("div.video-cell")!!

    // -- searchVideo() / recommended_videoCell() (yt2009templates.js) --
    // both share identical field selectors relative to div.video-cell

    @Test
    fun `parses a search result video-cell`() {
        val html = """
            <div class="video-cell" data-id="dQw4w9WgXcQ">
                <div class="video-entry">
                    <div class="v120WideEntry"><div class="v120WrapperOuter"><div class="v120WrapperInner">
                        <a id="video-title-results" href="/watch?v=dQw4w9WgXcQ" rel="nofollow">
                            <img title="Never Gonna Give You Up" src="/thumb/dQw4w9WgXcQ.jpg" class="vimg120">
                        </a>
                        <div class="video-time"><span id="video-run-time">3:33</span></div>
                    </div></div></div>
                    <div class="video-main-content" id="video-main-content">
                        <div class="video-title video-title-results">
                            <div class="video-short-title">
                                <a id="video-short-title" href="/watch?v=dQw4w9WgXcQ" title="Never Gonna Give You Up" rel="nofollow">Never Gonna Give You Up</a>
                            </div>
                        </div>
                        <div id="video-description" class="video-description">Rick Astley's official video</div>
                        <div class="result-label">
                            <span class="result-type">Video:</span>
                            <span class="video-username"><a id="video-from-username" class="hLink" href="/user/RickAstleyVEVO">RickAstleyVEVO</a></span>
                        </div>
                        <div class="video-facets">
                            <span id="video-added-time" class="video-date-added">14 years ago</span>
                            <span id="video-num-views" class="video-view-count">1,234,567,890 views</span>
                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()

        val summary = Yt2009Api.parseVideoCell(cell(html))
        assertEquals("dQw4w9WgXcQ", summary?.videoId)
        assertEquals("Never Gonna Give You Up", summary?.title)
        assertTrue(summary?.thumbnailUrl.orEmpty().endsWith("/thumb/dQw4w9WgXcQ.jpg"))
        assertEquals("RickAstleyVEVO", summary?.channelName)
        assertEquals("3:33", summary?.durationText)
        assertEquals("1,234,567,890 views", summary?.viewCountText)
        assertEquals("14 years ago", summary?.uploadedText)
    }

    @Test
    fun `video-cell without a data-id attribute falls back to the watch link`() {
        // videoCell() (browse grid) still has data-id, but this guards
        // parseVideoCell's fallback path in case a future/unknown variant
        // doesn't
        val html = """
            <div class="video-cell">
                <a href="/watch?v=abc12345678">
                    <div class="video-short-title"><a title="Some Title">Some Title</a></div>
                </a>
            </div>
        """.trimIndent()
        val summary = Yt2009Api.parseVideoCell(cell(html))
        assertEquals("abc12345678", summary?.videoId)
    }

    // -- feeditem-bigthumb (homepage_watched/homepage_featured) --

    @Test
    fun `parses a homepage feeditem-bigthumb`() {
        val html = """
            <div class="feeditem-bigthumb super-large-video yt-uix-hovercard ">
                <div style="font-size: 12px;" class="floatL">
                    <div class="feedmodule-thumbnail">
                        <div class="v220WideEntry"><div class="v220WrapperOuter"><div class="v220WrapperInner">
                            <a class="video-thumb-link" href="/watch?v=jNQXAC9IVRw" rel="nofollow"><img title="Me at the zoo" src="/thumb/jNQXAC9IVRw.jpg" class="vimg220 yt-uix-hovercard-target"></a>
                        </div></div></div>
                    </div>
                </div>
                <div class="feedmodule-singleform-info">
                    <div class="video-title"><a href="/watch?v=jNQXAC9IVRw" class="yt-uix-hovercard-target" title="Me at the zoo">Me at the zoo</a></div>
                    <div>18,234,111 views</div>
                    <div><nobr><a href="/user/jawed">jawed</a></nobr></div>
                    <div class="feedmodule-singleform-info-ratings">stars</div>
                </div>
                <div class="spacer">&nbsp;</div>
            </div>
        """.trimIndent()

        val summary = Yt2009Api.parseFeedItemBigthumb(
            Jsoup.parse(html, "http://host:3000").selectFirst("div.feeditem-bigthumb")!!
        )
        assertEquals("jNQXAC9IVRw", summary?.videoId)
        assertEquals("Me at the zoo", summary?.title)
        assertEquals("jawed", summary?.channelName)
        assertEquals("18,234,111 views", summary?.viewCountText)
        // no duration/upload-date field exists on this template at all
        assertNull(summary?.durationText)
        assertNull(summary?.uploadedText)
    }

    // -- relatedVideo() (watch page sidebar) --

    @Test
    fun `parses a related video-entry, distinct from video-cell markup`() {
        val html = """
            <div class="video-entry" data-id="9bZkp7q19f0">
                <div class="v90WideEntry"><div class="v90WrapperOuter"><div class="v90WrapperInner">
                    <a href="/watch?v=9bZkp7q19f0" class="video-thumb-link" rel="nofollow">
                        <img title="Gangnam Style" thumb="/thumb/9bZkp7q19f0.jpg" src="/thumb/9bZkp7q19f0.jpg" class="vimg90" qlicon="9bZkp7q19f0" alt="Gangnam Style" loading="lazy">
                    </a>
                    <div class="video-time"><a href="/watch?v=9bZkp7q19f0" rel="nofollow">4:12</a></div>
                </div></div></div>
                <div class="video-main-content">
                    <div class="video-mini-title"><a href="/watch?v=9bZkp7q19f0" rel="nofollow">Gangnam Style</a></div>
                    <div class="video-view-count">4,600,000,000 views</div>
                    <div class="video-username"><a href="/user/officialpsy">officialpsy</a></div>
                </div>
                <span class="abs-views hid">4600000000</span>
            </div>
        """.trimIndent()

        val entry = Jsoup.parse(html, "http://host:3000").selectFirst("div.video-entry")!!
        val summary = Yt2009Api.parseRelatedVideoEntry(entry)
        assertEquals("9bZkp7q19f0", summary?.videoId)
        assertEquals("Gangnam Style", summary?.title)
        assertEquals("officialpsy", summary?.channelName)
        assertEquals("4:12", summary?.durationText)
        assertEquals("4,600,000,000 views", summary?.viewCountText)
    }

    // -- playnavVideo() (channel page scrollboxes) --

    @Test
    fun `parses a channel playnav-item, which has no watch link at all`() {
        val html = """
            <div class="playnav-item playnav-video  " id="playnav-video-tPEE9ZwTmy0" onclick="switchVideo(this);return false;">
                <div id="playnav-video-play-tPEE9ZwTmy0-selector" class="selector"></div>
                <div class="content">
                    <div class="playnav-video-thumb link-as-border-color">
                        <a class="video-thumb-90 no-quicklist" href="#"><img title="Charlie bit my finger" src="/thumb/tPEE9ZwTmy0.jpg" class="vimg90 yt-uix-hovercard-target" alt="Charlie bit my finger"><div class="video-time"><span>0:56</span></div></a>
                    </div>
                    <div class="playnav-video-info">
                        <a href="#" class="playnav-item-title ellipsis"><span class="video-title-tPEE9ZwTmy0">Charlie bit my finger</span></a>
                        <div class="metadata video-meta-tPEE9ZwTmy0">890,000,000 views - 15 years ago</div>
                        <div class="video-ratings-tPEE9ZwTmy0 hid">stars</div>
                    </div>
                </div>
            </div>
        """.trimIndent()

        val item = Jsoup.parse(html, "http://host:3000").selectFirst("div.playnav-item")!!
        val summary = Yt2009Api.parsePlaynavVideo(item, channelName = "HDCYT")
        assertEquals("tPEE9ZwTmy0", summary?.videoId)
        assertEquals("Charlie bit my finger", summary?.title)
        assertEquals("HDCYT", summary?.channelName)
        assertEquals("890,000,000 views", summary?.viewCountText)
        assertEquals("15 years ago", summary?.uploadedText)
        assertEquals("0:56", summary?.durationText)
    }

    // -- videoComment() (watch page comments) --

    @Test
    fun `parses a comment entry`() {
        val html = """
            <div class="watch-comment-entry" id="comment-abc123">
                <div class="watch-comment-head">
                    <div class="watch-comment-info">
                        <a class="watch-comment-auth" href="/user/someviewer" rel="nofollow">someviewer</a>
                        <span class="watch-comment-time"> (2 days ago) </span>
                    </div>
                    <div class="watch-comment-voting">
                        <span class="watch-comment-score watch-comment-green" data-initial="12">+12</span>
                    </div>
                </div>
                <div>
                    <div class="watch-comment-body">
                        <div>this is a great video</div>
                    </div>
                </div>
            </div>
        """.trimIndent()

        val entry = Jsoup.parse(html, "http://host:3000").selectFirst("div.watch-comment-entry")!!
        val comment = Yt2009Api.parseCommentEntry(entry)
        assertEquals("abc123", comment?.id)
        assertEquals("someviewer", comment?.author)
        assertEquals("this is a great video", comment?.text)
        assertEquals("2 days ago", comment?.timeText)
        assertEquals("+12", comment?.scoreText)
    }

    // -- parseInitialComments: continuation token / hasMore signal --

    @Test
    fun `treats the unfilled continuation placeholder as no continuation`() {
        val doc = Jsoup.parse(
            """
            <div class="comments-container" data-continuation-token="yt2009_comments_continuation_token" data-page="1">
            </div>
            """.trimIndent()
        )
        val page = Yt2009Api.parseInitialComments(doc)
        assertNull(page.nextContinuation)
        assertEquals(emptyList<CommentItem>(), page.comments)
    }

    @Test
    fun `a real continuation token is kept, and the more-comments footer signals another page`() {
        // the footer td is only ever real inside its actual
        // watch-comments-footer-table (watch.html:601) -- an orphan <td>
        // outside a <table> gets foster-parented/dropped by Jsoup's HTML5
        // parser, same as a real browser would, so this has to mirror the
        // real structure to mean anything
        val doc = Jsoup.parse(
            """
            <div class="comments-container" data-continuation-token="realtoken123">
            </div>
            <table id="watch-comments-footer-table"><tr>
                <td class="watch-comments-footer-td yt2009_hook_more_comments" id="watch-comments-show-more-td"></td>
            </tr></table>
            """.trimIndent()
        )
        val page = Yt2009Api.parseInitialComments(doc)
        assertEquals("realtoken123", page.nextContinuation)
        assertEquals(2, page.nextPage)
    }
}
