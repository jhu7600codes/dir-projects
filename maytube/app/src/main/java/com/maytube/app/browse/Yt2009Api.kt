package com.maytube.app.browse

import android.webkit.CookieManager
import com.maytube.app.data.ServerConfig
import com.maytube.app.webview.MobileInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

/**
 * yt2009 has no JSON API -- every page (home, search, watch, channel) is
 * server-rendered HTML, so "native" browsing means parsing that HTML
 * directly, the same way a screen-scraper/userscript would. Every selector
 * below was read from yt2009's actual source (back/yt2009templates.js,
 * back/yt2009html.js, back/yt2009search.js, back/yt2009channels.js,
 * watch.html/channelpage.htm/search-generic-page.htm), not guessed --
 * yt2009 has upward of eight *different* markups for "a video in a list"
 * depending on context (search results, related videos, channel grids,
 * homepage modules all differ), so guessing here would repeat the exact
 * mistake MobileInjector's first draft made with invented CSS selectors.
 *
 * Auth: yt2009 gates several routes (confirmed: /results) behind a
 * site-wide `auth=<token>` cookie whitelist (yt2009utils.isAuthorized),
 * separate from the `login_simulate` account cookie. Rather than
 * reimplement whatever token flow a given instance uses, every request
 * here just forwards CookieManager's current cookie jar for this origin --
 * exactly what SabrFragmentDownloader already does for the same reason:
 * whatever cookies got the WebView browsing successfully are byte-for-byte
 * what a native request needs too.
 */
object Yt2009Api {

    class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // -- home -----------------------------------------------------------

    /**
     * index.htm's "Watched Now"/"Featured" modules (`div.feeditem-bigthumb`,
     * yt2009templates.js homepage_watched/homepage_featured, filled in
     * back/yt2009homepage.js) are embedded directly in the initial HTML.
     * The real "Recommended" grid (`recommended_videoCell`, real
     * `.video-cell` markup) is loaded by the page's own JS as a *second*
     * request to `/yt2009_recommended` with an `ids` header of recently
     * watched video ids (assets/site-assets/homepage-recommended.js) --
     * without that header the endpoint replies with the literal string
     * "YT2009_NO_DATA". [recentlyWatchedIds] lets WatchActivity's own local
     * watch history feed that same signal in, so recommendations genuinely
     * personalize the same way the JS-driven homepage does; recommended
     * results are put first when available.
     */
    suspend fun fetchHome(config: ServerConfig, recentlyWatchedIds: List<String>): VideoPage =
        withContext(Dispatchers.IO) {
            val recommended = fetchRecommended(config, recentlyWatchedIds)

            val doc = fetchDocument(config, "/")
            val featured = doc.select("div.feeditem-bigthumb").mapNotNull { parseFeedItemBigthumb(it) }

            val seen = HashSet<String>()
            val combined = (recommended + featured).filter { seen.add(it.videoId) }
            VideoPage(combined, nextPageUrl = null)
        }

    private fun fetchRecommended(config: ServerConfig, recentlyWatchedIds: List<String>): List<VideoSummary> {
        if (recentlyWatchedIds.isEmpty()) return emptyList()
        return try {
            val request = Request.Builder()
                .url("${config.baseUrl}/yt2009_recommended")
                .header("Cookie", cookieHeader(config))
                .header("ids", recentlyWatchedIds.joinToString(","))
                .build()
            val body = client.newCall(request).execute().use { response ->
                persistCookies(config, response)
                if (!response.isSuccessful) return emptyList()
                response.body?.string().orEmpty()
            }
            if (body.isBlank() || body.contains("YT2009_NO_DATA")) return emptyList()
            Jsoup.parse(body, config.baseUrl).select("div.video-cell").mapNotNull { parseVideoCell(it) }
        } catch (e: Exception) {
            emptyList() // recommendations are a nice-to-have, never worth failing the whole home feed over
        }
    }

    /**
     * `div.feeditem-bigthumb` (homepage_watched/homepage_featured modules):
     * no data-id attribute on this template (unlike the .video-cell
     * family), so the video id has to come from the /watch?v= link itself.
     * No duration/upload-date field exists on this markup at all.
     */
    internal fun parseFeedItemBigthumb(cell: Element): VideoSummary? {
        val titleLink = cell.selectFirst(".video-title a") ?: return null
        val videoId = MobileInjector.extractVideoId(titleLink.attr("href")) ?: return null
        val channelLink = cell.selectFirst("nobr a")
        return VideoSummary(
            videoId = videoId,
            title = titleLink.text().ifBlank { titleLink.attr("title") },
            thumbnailUrl = cell.selectFirst(".video-thumb-link img")?.attr("abs:src"),
            channelName = channelLink?.text(),
            channelId = channelLink?.attr("href"),
            viewCountText = cell.select(".feedmodule-singleform-info > div").getOrNull(1)?.text(),
            durationText = null,
            uploadedText = null
        )
    }

    // -- search -----------------------------------------------------------

    /**
     * `#video_grid > div.video-cell` (searchVideo() template,
     * yt2009templates.js:304). "No results" is unambiguous:
     * #search-no-results-message is present when the query matched
     * nothing, distinct from an empty page just being blank.
     */
    suspend fun search(config: ServerConfig, query: String, page: Int = 1): VideoPage =
        withContext(Dispatchers.IO) {
            val path = "/results?search_query=${java.net.URLEncoder.encode(query, "UTF-8")}&page=$page"
            val doc = fetchDocument(config, path)
            if (doc.selectFirst("#search-no-results-message") != null) {
                return@withContext VideoPage(emptyList(), null)
            }
            val videos = doc.select("#video_grid > div.video-cell").mapNotNull { parseVideoCell(it) }
            val nextUrl = doc.select("div.pagingDiv a.pagerNotCurrent")
                .firstOrNull { it.text().trim().equals("Next", ignoreCase = true) }
                ?.attr("abs:href")
            VideoPage(videos, nextUrl?.takeIf { it.isNotBlank() })
        }

    // -- watch page ---------------------------------------------------------

    /**
     * watch.html's real ids/classes for everything except comments (see
     * [fetchComments]): h1.watch-vid-ab-title (title),
     * a.yt2009-channel-link (channel name+url),
     * a.yt2009-channel-avatar img (avatar), span.watch-video-added.post-date
     * (upload date, an absolute date, not relative), #watch-view-count
     * (views), button.yt2009-stars (5-star rating -- yt2009 has no
     * like/dislike, see VideoDetails' kdoc), #watch-video-details-inner-more
     * .watch-video-desc span (full description; falls back to the
     * `-inner-less` short version if the full one is blank -- both are
     * present in the raw HTML regardless of the `-more` div's inline
     * display:none, so no JS execution is needed to read it),
     * #watch-related-discoverbox .yt2009-default-related > .video-entry
     * (related videos, relatedVideo() template).
     */
    suspend fun fetchWatchPage(config: ServerConfig, videoId: String): VideoDetails =
        fetchWatchPageWithComments(config, videoId).first

    /**
     * Comments' first page is embedded in this exact same document (see
     * [parseInitialComments]) -- fetching it once and deriving both saves a
     * second round trip to the same URL.
     */
    suspend fun fetchWatchPageWithComments(config: ServerConfig, videoId: String): Pair<VideoDetails, CommentPage> =
        withContext(Dispatchers.IO) {
            val doc = fetchDocument(config, "/watch?v=$videoId")
            val channelLink = doc.selectFirst("a.yt2009-channel-link")
            val ratingButton = doc.selectFirst("button.yt2009-stars")
            val fullDescription = doc.selectFirst("#watch-video-details-inner-more .watch-video-desc span")?.text()
            val shortDescription = doc.selectFirst("#watch-video-details-inner-less .watch-video-desc span")?.text()

            val details = VideoDetails(
                videoId = videoId,
                title = doc.selectFirst("h1.watch-vid-ab-title")?.text().orEmpty(),
                channelName = channelLink?.text(),
                channelUrl = channelLink?.attr("href"),
                channelAvatarUrl = doc.selectFirst("a.yt2009-channel-avatar img")?.attr("abs:src"),
                viewCountText = doc.selectFirst("#watch-view-count")?.text(),
                uploadedText = doc.selectFirst("span.watch-video-added.post-date")?.text(),
                ratingText = ratingButton?.attr("title")?.takeIf { it.isNotBlank() },
                ratingCountText = doc.selectFirst("#defaultRatingMessage span.smallText")?.text(),
                description = fullDescription?.takeIf { it.isNotBlank() } ?: shortDescription,
                related = doc.select("#watch-related-discoverbox .yt2009-default-related > div.video-entry")
                    .mapNotNull { parseRelatedVideoEntry(it) }
            )
            details to parseInitialComments(doc)
        }

    internal fun parseRelatedVideoEntry(cell: Element): VideoSummary? {
        val videoId = cell.attr("data-id").takeIf { it.isNotBlank() } ?: return null
        val channelLink = cell.selectFirst(".video-username a")
        return VideoSummary(
            videoId = videoId,
            title = cell.selectFirst(".video-mini-title a")?.text().orEmpty(),
            thumbnailUrl = cell.selectFirst("a.video-thumb-link img")?.attr("abs:src"),
            channelName = channelLink?.text(),
            channelId = channelLink?.attr("href"),
            viewCountText = cell.selectFirst(".video-view-count")?.text(),
            durationText = cell.selectFirst(".video-time a")?.text(),
            uploadedText = null
        )
    }

    /**
     * `div.video-cell` fields shared verbatim by searchVideo(),
     * recommended_videoCell(), and playlistVideo() -- see Yt2009Api's
     * class kdoc / the source investigation this was built from.
     */
    internal fun parseVideoCell(cell: Element): VideoSummary? {
        val videoId = cell.attr("data-id").takeIf { it.isNotBlank() }
            ?: cell.selectFirst("a[href*=watch?v=]")?.let { MobileInjector.extractVideoId(it.attr("href")) }
            ?: return null
        val titleLink = cell.selectFirst(".video-short-title a")
        val channelLink = cell.selectFirst(".video-username a")
        return VideoSummary(
            videoId = videoId,
            title = titleLink?.text()?.ifBlank { titleLink.attr("title") }.orEmpty(),
            thumbnailUrl = cell.selectFirst("img")?.attr("abs:src"),
            channelName = channelLink?.text(),
            channelId = channelLink?.attr("href"),
            viewCountText = cell.selectFirst(".video-view-count")?.text(),
            durationText = cell.selectFirst(".video-time")?.text(),
            uploadedText = cell.selectFirst(".video-date-added")?.text()
        )
    }

    // -- comments -----------------------------------------------------------

    /**
     * Page 1 of comments is embedded directly in the watch page's own HTML
     * (`.comments-container`, no extra request needed) -- pass the already
     * -fetched [watchDoc] from [fetchWatchPage] rather than re-fetching.
     * `data-continuation-token` starts as the literal placeholder string
     * "yt2009_comments_continuation_token" when unfilled; treated as "no
     * continuation" here. Whether more comments exist at all is a separate
     * signal: the "show more" footer cell loses a `yt2009_hook_more_comments`
     * class (replaced with `hid`) once there are none.
     */
    fun parseInitialComments(watchDoc: Document): CommentPage {
        val container = watchDoc.selectFirst(".comments-container")
        val comments = container?.select("> div.watch-comment-entry")?.mapNotNull { parseCommentEntry(it) }
            .orEmpty()
        val hasMore = watchDoc.selectFirst("td.yt2009_hook_more_comments") != null
        val token = container?.attr("data-continuation-token")
            ?.takeIf { it.isNotBlank() && it != "yt2009_comments_continuation_token" }
        return CommentPage(comments, nextContinuation = token, nextPage = if (hasMore) 2 else null)
    }

    /**
     * `GET /get_more_comments` (back/backend.js) takes its paging state via
     * request *headers*, not query params: `source` (the full watch page
     * URL the comments belong to), and either `continuation` (preferred) or
     * `page`. The response is a raw HTML fragment of `.watch-comment-entry`
     * divs with an optional ";yt_continuation=<token>" suffix appended when
     * there's another page -- nbedit_watch.js's own client does exactly
     * this same string split, not JSON parsing.
     */
    suspend fun fetchMoreComments(
        config: ServerConfig,
        videoId: String,
        continuation: String?,
        page: Int?
    ): CommentPage = withContext(Dispatchers.IO) {
        val watchUrl = "${config.baseUrl}/watch?v=$videoId"
        val requestBuilder = Request.Builder()
            .url("${config.baseUrl}/get_more_comments")
            .header("Cookie", cookieHeader(config))
            .header("source", watchUrl)
        if (continuation != null) {
            requestBuilder.header("continuation", continuation)
        } else {
            requestBuilder.header("page", (page ?: 2).toString())
        }
        val body = client.newCall(requestBuilder.build()).execute().use { response ->
            persistCookies(config, response)
            if (!response.isSuccessful) throw ApiException("HTTP ${response.code} fetching more comments")
            response.body?.string().orEmpty()
        }
        val parts = body.split(";yt_continuation=")
        val html = parts.getOrNull(0).orEmpty()
        val nextToken = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        val comments = Jsoup.parseBodyFragment(html, config.baseUrl)
            .select("div.watch-comment-entry")
            .mapNotNull { parseCommentEntry(it) }
        CommentPage(
            comments,
            nextContinuation = nextToken,
            nextPage = if (nextToken == null && comments.isNotEmpty()) (page ?: 1) + 1 else null
        )
    }

    internal fun parseCommentEntry(entry: Element): CommentItem? {
        val author = entry.selectFirst(".watch-comment-auth") ?: return null
        val text = entry.selectFirst(".watch-comment-body > div")?.text()
            ?: entry.selectFirst(".watch-comment-body")?.text()
            ?: return null
        return CommentItem(
            id = entry.id().removePrefix("comment-").takeIf { it.isNotBlank() },
            author = author.text(),
            authorUrl = author.attr("href").takeIf { it.isNotBlank() },
            authorAvatarUrl = entry.selectFirst("img.author-image")?.attr("abs:src"),
            text = text,
            timeText = entry.selectFirst(".watch-comment-time")?.text()?.trim()?.trim('(', ')')?.trim(),
            scoreText = entry.selectFirst(".watch-comment-score")?.text()
        )
    }

    // -- channel -----------------------------------------------------------

    /**
     * channelPath is whatever href a video's channel link already carries
     * (`/channel/<id>`, `/user/<name>`, or `/c/<name>` -- yt2009 supports
     * all three, see yt2009channels.js), so this never has to reconstruct
     * a channel URL itself. `.scrollbox-uploads` (not `.scrollbox-all`,
     * which is capped at 10) holds the channel's complete upload list with
     * no pagination -- yt2009channels.js renders it all in one response.
     */
    suspend fun fetchChannel(config: ServerConfig, channelPath: String): ChannelPage =
        withContext(Dispatchers.IO) {
            val doc = fetchDocument(config, channelPath)
            val name = doc.selectFirst(".yt2009-name")?.text()
                ?: doc.selectFirst("#channel_title")?.text().orEmpty()
            val subscribers = doc.select(".profile_info .show_info").firstOrNull {
                it.selectFirst("div")?.text()?.equals("Subscribers", ignoreCase = true) == true
            }?.select("div")?.getOrNull(1)?.text()
            val description = doc.select(".profile_info .show_info").firstOrNull {
                it.selectFirst("div")?.text()?.equals("Description", ignoreCase = true) == true
            }?.select("div")?.getOrNull(1)?.text()

            val channel = ChannelSummary(
                channelUrl = channelPath,
                name = name,
                avatarUrl = doc.selectFirst(".user-thumb-xlarge img")?.attr("abs:src"),
                bannerUrl = doc.selectFirst("#user_banner img")?.attr("abs:src"),
                subscriberCountText = subscribers,
                description = description
            )
            val videos = doc.select("div.scrollbox-uploads div.playnav-item.playnav-video")
                .mapNotNull { parsePlaynavVideo(it, name) }
            ChannelPage(channel, videos)
        }

    /**
     * playnavVideo() (channel page scrollboxes) has no /watch?v= link at
     * all -- the video id only exists as the `playnav-video-<id>` suffix of
     * the wrapping div's own `id` attribute.
     */
    internal fun parsePlaynavVideo(item: Element, channelName: String): VideoSummary? {
        val videoId = item.id().removePrefix("playnav-video-").takeIf { it.isNotBlank() } ?: return null
        val metaText = item.selectFirst(".metadata")?.text().orEmpty()
        val metaParts = metaText.split(" - ", limit = 2)
        return VideoSummary(
            videoId = videoId,
            title = item.selectFirst("a.playnav-item-title span")?.text().orEmpty(),
            thumbnailUrl = item.selectFirst(".playnav-video-thumb img")?.attr("abs:src"),
            channelName = channelName,
            channelId = null,
            viewCountText = metaParts.getOrNull(0)?.trim(),
            durationText = item.selectFirst(".playnav-video-thumb .video-time span")?.text(),
            uploadedText = metaParts.getOrNull(1)?.trim()
        )
    }

    // -- shared ---------------------------------------------------------

    private fun cookieHeader(config: ServerConfig): String =
        CookieManager.getInstance().getCookie(config.baseUrl).orEmpty()

    /**
     * Native requests never go through an actual WebView, so nothing else
     * would ever capture a Set-Cookie yt2009 issues on a native-only
     * install (e.g. whatever session/auth cookie a fresh visit gets --
     * yt2009utils.isAuthorized gates /results, and likely more, on an
     * `auth=` cookie separate from the login_simulate account cookie).
     * Every response here gets its Set-Cookie headers persisted back into
     * the same CookieManager the WebView path already reads/writes (see
     * MainActivity.applyFlagCookie, SabrFragmentDownloader), so native
     * mode bootstraps its own session exactly like a real browser visit
     * would, and both paths stay consistent with each other regardless of
     * which one happens to run first.
     */
    private fun persistCookies(config: ServerConfig, response: Response) {
        val cookieManager = CookieManager.getInstance()
        response.headers("Set-Cookie").forEach { setCookie ->
            cookieManager.setCookie(config.baseUrl, setCookie)
        }
    }

    private fun fetchDocument(config: ServerConfig, path: String): Document {
        val request = Request.Builder()
            .url("${config.baseUrl}$path")
            .header("Cookie", cookieHeader(config))
            .build()
        val body = client.newCall(request).execute().use { response ->
            persistCookies(config, response)
            if (!response.isSuccessful) throw ApiException("HTTP ${response.code} fetching $path")
            response.body?.string() ?: throw ApiException("empty response fetching $path")
        }
        return Jsoup.parse(body, config.baseUrl)
    }
}
