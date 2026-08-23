package com.ytclassic.app.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** A page of results plus the continuation to fetch the next one, if any. */
data class PageResult<T>(val items: List<T>, val nextPage: Page?)

/**
 * Thin coroutine wrapper around NewPipeExtractor's YouTube service. Nothing
 * here needs a signed-in session or an API key - it's all public scraping,
 * the same surface NewPipe/PipePipe/etc. are built on. The cookie session
 * ([com.ytclassic.app.auth.SessionManager]) only comes into play for the
 * write actions and the home/subscriptions feed in
 * [com.ytclassic.app.network.InnertubeFeedClient] and
 * [com.ytclassic.app.network.InnertubeActions] - extraction and those two
 * concerns are deliberately kept apart.
 */
object YouTubeRepository {

    private val service = ServiceList.YouTube

    suspend fun trending(): PageResult<StreamInfoItem> = withContext(Dispatchers.IO) {
        val extractor = service.kioskList.getExtractorById("Trending", null)
        extractor.fetchPage()
        val page = extractor.initialPage
        PageResult(page.items.filterIsInstance<StreamInfoItem>(), page.nextPage)
    }

    suspend fun search(query: String): PageResult<StreamInfoItem> = withContext(Dispatchers.IO) {
        val handler = service.searchQHFactory.fromQuery(query)
        val info = org.schabi.newpipe.extractor.search.SearchInfo.getInfo(service, handler)
        PageResult(info.relatedItems.filterIsInstance<StreamInfoItem>(), info.nextPage)
    }

    suspend fun searchMore(query: String, page: Page): PageResult<StreamInfoItem> =
        withContext(Dispatchers.IO) {
            val handler = service.searchQHFactory.fromQuery(query)
            val next = org.schabi.newpipe.extractor.search.SearchInfo.getMoreItems(service, handler, page)
            PageResult(next.items.filterIsInstance<StreamInfoItem>(), next.nextPage)
        }

    suspend fun streamInfo(url: String): StreamInfo = withContext(Dispatchers.IO) {
        StreamInfo.getInfo(service, url)
    }

    suspend fun comments(url: String): PageResult<CommentsInfoItem> = withContext(Dispatchers.IO) {
        val info = CommentsInfo.getInfo(service, url)
        PageResult(info.relatedItems.filterIsInstance<CommentsInfoItem>(), info.nextPage)
    }

    suspend fun commentsMore(url: String, page: Page): PageResult<CommentsInfoItem> =
        withContext(Dispatchers.IO) {
            val next = CommentsInfo.getMoreItems(service, url, page)
            PageResult(next.items.filterIsInstance<CommentsInfoItem>(), next.nextPage)
        }

    /**
     * Fetches one comment's reply thread. The item's own `replies` [Page]
     * already encodes (internally, via the page's id/url) that it's a
     * reply continuation rather than a plain next-page one, so it's routed
     * through the same `getMoreItems` entry point as a normal comments
     * page - this mirrors how the NewPipe app itself resolves replies.
     */
    suspend fun replies(videoUrl: String, repliesPage: Page): PageResult<CommentsInfoItem> =
        withContext(Dispatchers.IO) {
            val next = CommentsInfo.getMoreItems(service, videoUrl, repliesPage)
            PageResult(next.items.filterIsInstance<CommentsInfoItem>(), next.nextPage)
        }

    data class ChannelData(
        val name: String,
        val avatarUrl: String?,
        val bannerUrl: String?,
        val subscriberCount: Long,
        val description: String?,
        val videos: List<StreamInfoItem>,
    )

    suspend fun channel(url: String): ChannelData = withContext(Dispatchers.IO) {
        val info = ChannelInfo.getInfo(service, url)
        val videosTab = info.tabs.firstOrNull { tab -> tab.contentFilters.contains(ChannelTabs.VIDEOS) }
        val videos: List<StreamInfoItem> = try {
            videosTab?.let { ChannelTabInfo.getInfo(service, it) }
                ?.relatedItems
                ?.filterIsInstance<StreamInfoItem>()
                .orEmpty()
        } catch (e: Exception) {
            emptyList()
        }

        ChannelData(
            name = info.name.orEmpty(),
            avatarUrl = info.avatars.maxByOrNull { it.height }?.url,
            bannerUrl = info.banners.maxByOrNull { it.height }?.url,
            subscriberCount = info.subscriberCount,
            description = info.description,
            videos = videos,
        )
    }
}
