package com.ytclassic.app.ui.common

/**
 * Where a [VideoListFragment] gets its rows from. Trending and Search ride
 * NewPipeExtractor's public scraping (paginated via its own [org.schabi.newpipe.extractor.Page]);
 * Home and Subscriptions only exist behind a signed-in cookie session and go
 * through [com.ytclassic.app.network.InnertubeFeedClient] instead - see that
 * class for why NewPipeExtractor can't cover them.
 */
sealed class VideoListSource {
    object Home : VideoListSource()
    object Trending : VideoListSource()
    object Subscriptions : VideoListSource()
    data class Search(val query: String) : VideoListSource()

    fun serialize(): Pair<String, String?> = when (this) {
        Home -> "home" to null
        Trending -> "trending" to null
        Subscriptions -> "subscriptions" to null
        is Search -> "search" to query
    }

    companion object {
        fun deserialize(type: String, extra: String?): VideoListSource = when (type) {
            "home" -> Home
            "trending" -> Trending
            "subscriptions" -> Subscriptions
            "search" -> Search(extra.orEmpty())
            else -> Trending
        }
    }
}
