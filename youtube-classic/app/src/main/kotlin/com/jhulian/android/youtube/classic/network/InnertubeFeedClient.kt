package com.jhulian.android.youtube.classic.network

import android.util.Log
import com.jhulian.android.youtube.classic.data.model.VideoUi
import com.jhulian.android.youtube.classic.data.model.reelItemRendererToVideoUi
import com.jhulian.android.youtube.classic.data.model.videoRendererToVideoUi
import com.jhulian.android.youtube.classic.util.JsonWalk
import org.json.JSONObject

/**
 * The personalized Home, Subscriptions and Shorts feeds only exist behind a
 * logged-in session - NewPipeExtractor deliberately doesn't scrape them
 * (there's no stable public surface for "recommended for you", and no
 * Shorts kiosk at all), so this calls innertube's `/browse` directly with
 * the cookie session from [com.jhulian.android.youtube.classic.auth.SessionManager],
 * the same way the youtube.com web client itself loads those tabs.
 *
 * Videos are pulled out by scanning the response for `videoRenderer` and
 * `reelItemRenderer` (Shorts) objects (see [JsonWalk]) rather than modeling
 * YouTube's actual section/shelf schema, since that schema is reshuffled
 * far more often than the renderers' own fields are.
 */
object InnertubeFeedClient {

    private const val HOME_BROWSE_ID = "FEwhat_to_watch"
    private const val SUBSCRIPTIONS_BROWSE_ID = "FEsubscriptions"
    private const val SHORTS_BROWSE_ID = "FEshorts"

    suspend fun home(cookieHeader: String?): List<VideoUi> = browse(HOME_BROWSE_ID, cookieHeader)

    suspend fun subscriptionsFeed(cookieHeader: String): List<VideoUi> =
        browse(SUBSCRIPTIONS_BROWSE_ID, cookieHeader)

    /**
     * Shorts have no signed-out public surface either (no NewPipeExtractor
     * kiosk, no anonymous browse endpoint that reliably returns a shelf) -
     * this always needs a session, unlike [home] which at least degrades to
     * Trending when signed out.
     */
    suspend fun shorts(cookieHeader: String): List<VideoUi> = browse(SHORTS_BROWSE_ID, cookieHeader)

    private suspend fun browse(browseId: String, cookieHeader: String?): List<VideoUi> {
        val body = JSONObject().apply {
            put("context", Innertube.buildContext())
            put("browseId", browseId)
        }
        val response = Innertube.post("browse", body, cookieHeader)
        val videos = JsonWalk.findAllObjectsWithKey(response, "videoRenderer")
            .mapNotNull { videoRendererToVideoUi(it) }
        val shorts = JsonWalk.findAllObjectsWithKey(response, "reelItemRenderer")
            .mapNotNull { reelItemRendererToVideoUi(it) }
        val results = videos + shorts

        if (results.isEmpty()) {
            // This is the one thing that can't be root-caused without
            // seeing a real response: could be a stale API key/context,
            // a wrong browseId, an auth/cookie problem the server accepts
            // but treats as signed-out, or a genuinely empty feed. Logging
            // the raw body (truncated) means the *next* report of "Home is
            // empty" comes with the actual answer instead of another guess.
            Log.w(TAG, "browse($browseId) returned 0 items. Raw response (first 2000 chars): ${response.toString().take(2000)}")
        }
        return results
    }

    private const val TAG = "InnertubeFeedClient"
}
