package com.maytube.app.download

import com.maytube.app.data.ServerConfig
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches and parses a single SABR fragment (`/sabr_playback?pid=...&offset=...`).
 * Shared by [SabrFragmentDownloader] and com.maytube.app.player.StreamingPlayer
 * -- both pull the exact same fragments, just for different purposes
 * (buffer-then-play vs. feed-as-it-arrives).
 */
object SabrFragmentFetcher {

    class FetchException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun fetch(
        client: OkHttpClient,
        config: ServerConfig,
        sabrPath: String,
        offsetMs: Long,
        attempts: Int = 3
    ): List<SabrFragmentParser.Part> {
        var lastError: Exception? = null
        repeat(attempts) { attempt ->
            try {
                val forceReplayer = if (attempt > 0) "&force_replayer=1" else ""
                val url = "${config.baseUrl}$sabrPath&offset=$offsetMs&hd=1$forceReplayer"
                val request = Request.Builder()
                    .url(url)
                    .header("Cookie", SabrSession.cookieHeader(config))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw FetchException("HTTP ${response.code} fetching offset ${offsetMs}ms")
                    }
                    val partCount = response.header("x-part-count")?.toIntOrNull() ?: 0
                    val body = response.body?.bytes() ?: ByteArray(0)
                    return SabrFragmentParser.parse(body, partCount)
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw FetchException("giving up on offset ${offsetMs}ms after $attempts attempts", lastError)
    }
}
