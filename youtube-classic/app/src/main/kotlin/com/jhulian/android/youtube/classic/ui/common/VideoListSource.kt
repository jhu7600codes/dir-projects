package com.jhulian.android.youtube.classic.ui.common

/**
 * Where a [VideoListFragment] gets its rows from. Search rides
 * NewPipeExtractor's public scraping (paginated via its own [org.schabi.newpipe.extractor.Page]);
 * Home and Subscriptions only exist behind a signed-in cookie session and go
 * through [com.jhulian.android.youtube.classic.network.InnertubeFeedClient] instead - see that
 * class for why NewPipeExtractor can't cover them. Home falls back to
 * NewPipeExtractor's public Trending kiosk when signed out (there's no
 * public "recommended for you" surface), but that's an implementation
 * detail of [VideoListViewModel.loadFeed] rather than its own tab/source -
 * Shorts is the actual fourth tab now.
 */
sealed class VideoListSource {
    object Home : VideoListSource()
    object Subscriptions : VideoListSource()
    data class Search(val query: String) : VideoListSource()

    fun serialize(): Pair<String, String?> = when (this) {
        Home -> "home" to null
        Subscriptions -> "subscriptions" to null
        is Search -> "search" to query
    }

    companion object {
        fun deserialize(type: String, extra: String?): VideoListSource = when (type) {
            "home" -> Home
            "subscriptions" -> Subscriptions
            "search" -> Search(extra.orEmpty())
            else -> Home
        }
    }
}
