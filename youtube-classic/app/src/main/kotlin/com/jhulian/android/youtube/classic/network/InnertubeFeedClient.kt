package com.jhulian.android.youtube.classic.network

import android.util.Log
import com.jhulian.android.youtube.classic.data.model.VideoUi
import com.jhulian.android.youtube.classic.data.model.lockupViewModelToVideoUi
import com.jhulian.android.youtube.classic.data.model.reelItemRendererToVideoUi
import com.jhulian.android.youtube.classic.data.model.shortsLockupViewModelToVideoUi
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
 * Videos are pulled out by scanning the response for renderer/view-model
 * objects (see [JsonWalk]) rather than modeling YouTube's actual
 * section/shelf schema, since that schema is reshuffled far more often than
 * the renderers' own fields are. Confirmed on a real device that the
 * current web client returns the newer "Kevlar" view-model schema
 * (`lockupViewModel`/`shortsLockupViewModel`) for these surfaces, not just
 * the classic `videoRenderer`/`reelItemRenderer` - both are handled so this
 * degrades gracefully if YouTube ships either shape.
 */
object InnertubeFeedClient {

    private const val HOME_BROWSE_ID = "FEwhat_to_watch"
    private const val SUBSCRIPTIONS_BROWSE_ID = "FEsubscriptions"

    suspend fun home(cookieHeader: String?): List<VideoUi> {
        val response = rawBrowse(HOME_BROWSE_ID, cookieHeader)
        val results = (extractVideos(response) + extractShorts(response)).distinctBy { it.videoId }
        logIfEmpty(HOME_BROWSE_ID, response, results)
        return results
    }

    suspend fun subscriptionsFeed(cookieHeader: String): List<VideoUi> {
        val response = rawBrowse(SUBSCRIPTIONS_BROWSE_ID, cookieHeader)
        val results = (extractVideos(response) + extractShorts(response)).distinctBy { it.videoId }
        logIfEmpty(SUBSCRIPTIONS_BROWSE_ID, response, results)
        return results
    }

    /**
     * Shorts have no standalone top-level browse surface the way Home and
     * Subscriptions do - `browseId: "FEshorts"` (this app's original guess)
     * comes back `HTTP 400 INVALID_ARGUMENT` on a real account, confirmed
     * via device logcat. The actual web client only ever gets Shorts as a
     * `shortsLockupViewModel` shelf embedded inside the Home feed response,
     * so that's what this reuses - same request as [home], filtered down to
     * just the short-form items instead of the regular video grid.
     */
    suspend fun shorts(cookieHeader: String): List<VideoUi> {
        val response = rawBrowse(HOME_BROWSE_ID, cookieHeader)
        val results = extractShorts(response).distinctBy { it.videoId }
        logIfEmpty("shorts (via $HOME_BROWSE_ID)", response, results)
        return results
    }

    // These are the specific surfaces that were coming back HTTP 200 with
    // "logged_in":"0" no matter how closely Innertube.post()'s headers
    // were made to match a real browser - routed through
    // Innertube.postAuthenticated() (a real WebView's fetch()) instead of
    // plain OkHttp when a session is actually present. See
    // WebViewInnertubeBridge's kdoc for why.
    private suspend fun rawBrowse(browseId: String, cookieHeader: String?): JSONObject {
        val body = JSONObject().apply {
            put("context", if (cookieHeader.isNullOrBlank()) Innertube.buildContext() else Innertube.buildWebViewContext())
            put("browseId", browseId)
        }
        return if (cookieHeader.isNullOrBlank()) {
            Innertube.post("browse", body, cookieHeader)
        } else {
            Innertube.postAuthenticated("browse", body, cookieHeader)
        }
    }

    private fun extractVideos(response: JSONObject): List<VideoUi> =
        JsonWalk.findAllObjectsWithKey(response, "videoRenderer").mapNotNull { videoRendererToVideoUi(it) } +
            JsonWalk.findAllObjectsWithKey(response, "lockupViewModel").mapNotNull { lockupViewModelToVideoUi(it) }

    private fun extractShorts(response: JSONObject): List<VideoUi> =
        JsonWalk.findAllObjectsWithKey(response, "reelItemRenderer").mapNotNull { reelItemRendererToVideoUi(it) } +
            JsonWalk.findAllObjectsWithKey(response, "shortsLockupViewModel").mapNotNull { shortsLockupViewModelToVideoUi(it) }

    private fun logIfEmpty(label: String, response: JSONObject, results: List<VideoUi>) {
        if (results.isEmpty()) {
            // This is the one thing that can't be root-caused without
            // seeing a real response: could be a stale API key/context,
            // a wrong browseId, an auth/cookie problem the server accepts
            // but treats as signed-out, or a genuinely empty feed. Logging
            // the raw body (truncated) means the *next* report of "Home is
            // empty" comes with the actual answer instead of another guess.
            Log.w(TAG, "browse($label) returned 0 items. Raw response (first 4000 chars): ${response.toString().take(4000)}")
        }
    }

    private const val TAG = "InnertubeFeedClient"
}
