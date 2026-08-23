package com.jhulian.android.youtube.classic.network

import com.jhulian.android.youtube.classic.data.model.VideoUi
import com.jhulian.android.youtube.classic.data.model.videoRendererToVideoUi
import com.jhulian.android.youtube.classic.util.JsonWalk
import org.json.JSONObject

/**
 * The personalized Home and Subscriptions feeds only exist behind a
 * logged-in session - NewPipeExtractor deliberately doesn't scrape them
 * (there's no stable public surface for "recommended for you"), so this
 * calls innertube's `/browse` directly with the cookie session from
 * [com.jhulian.android.youtube.classic.auth.SessionManager], the same way the youtube.com
 * web client itself loads those tabs.
 *
 * Videos are pulled out by scanning the response for `videoRenderer`
 * objects (see [JsonWalk]) rather than modeling YouTube's actual section/
 * shelf schema, since that schema is reshuffled far more often than the
 * renderer's own fields are.
 */
object InnertubeFeedClient {

    private const val HOME_BROWSE_ID = "FEwhat_to_watch"
    private const val SUBSCRIPTIONS_BROWSE_ID = "FEsubscriptions"

    suspend fun home(cookieHeader: String): List<VideoUi> = browse(HOME_BROWSE_ID, cookieHeader)

    suspend fun subscriptionsFeed(cookieHeader: String): List<VideoUi> =
        browse(SUBSCRIPTIONS_BROWSE_ID, cookieHeader)

    private suspend fun browse(browseId: String, cookieHeader: String): List<VideoUi> {
        val body = JSONObject().apply {
            put("context", Innertube.buildContext())
            put("browseId", browseId)
        }
        val response = Innertube.post("browse", body, cookieHeader)
        return JsonWalk.findAllObjectsWithKey(response, "videoRenderer")
            .mapNotNull { videoRendererToVideoUi(it) }
    }
}
