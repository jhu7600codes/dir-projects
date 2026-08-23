package com.ytclassic.app.network

import com.ytclassic.app.util.JsonWalk
import org.json.JSONArray
import org.json.JSONObject

enum class LikeStatus { LIKE, DISLIKE, NONE }

/**
 * Write actions against a signed-in YouTube session: like/dislike, subscribe,
 * and posting a top-level comment. All of it rides on the cookie captured by
 * the WebView login (see [Innertube]) - there's no OAuth scope being granted,
 * this is exactly what a browser tab logged into youtube.com could do.
 *
 * The endpoint paths are the stable, long-lived internal routes YouTube's
 * own web client calls; the opaque `params` blobs on subscribe/unsubscribe
 * are reverse-engineered constants (same ones yt-dlp uses) and are the most
 * likely thing to need updating if YouTube changes its subscription flow.
 */
object InnertubeActions {

    suspend fun setLikeStatus(videoId: String, status: LikeStatus, cookieHeader: String) {
        val endpoint = when (status) {
            LikeStatus.LIKE -> "like/like"
            LikeStatus.DISLIKE -> "like/dislike"
            LikeStatus.NONE -> "like/removelike"
        }
        val body = JSONObject().apply {
            put("context", Innertube.buildContext())
            put("target", JSONObject().put("videoId", videoId))
        }
        Innertube.post(endpoint, body, cookieHeader)
    }

    suspend fun setSubscribed(channelId: String, subscribed: Boolean, cookieHeader: String) {
        val endpoint = if (subscribed) "subscription/subscribe" else "subscription/unsubscribe"
        val body = JSONObject().apply {
            put("context", Innertube.buildContext())
            put("channelIds", JSONArray().put(channelId))
            put("params", if (subscribed) "EgIIAg%3D%3D" else "CgIIAQ%3D%3D")
        }
        Innertube.post(endpoint, body, cookieHeader)
    }

    /**
     * Posting a fresh top-level comment needs a `createCommentParams` token
     * that YouTube only hands out from the video's own comments section (it
     * encodes the video id + a server-side signature), so this makes a
     * throwaway `/next` call first to fish it out - see [JsonWalk], which
     * finds it wherever the comments-section renderer currently nests it.
     */
    suspend fun postComment(videoId: String, text: String, cookieHeader: String): Boolean {
        val nextBody = JSONObject().apply {
            put("context", Innertube.buildContext())
            put("videoId", videoId)
        }
        val nextResponse = Innertube.post("next", nextBody, cookieHeader)
        val createCommentParams = JsonWalk.findFirstString(nextResponse, "createCommentParams")
            ?: return false

        val commentBody = JSONObject().apply {
            put("context", Innertube.buildContext())
            put("createCommentParams", createCommentParams)
            put("commentText", text)
        }
        Innertube.post("comment/create_comment", commentBody, cookieHeader)
        return true
    }
}
