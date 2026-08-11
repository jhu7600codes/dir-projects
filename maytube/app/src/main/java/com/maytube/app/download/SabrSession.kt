package com.maytube.app.download

import android.webkit.CookieManager
import com.maytube.app.data.ServerConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

/**
 * Resolves a video's SABR session (the `/sabr_playback?pid=...` base path
 * and, if known, the video's duration) from its watch page. Shared by
 * [SabrFragmentDownloader] (buffer-then-play downloads) and
 * com.maytube.app.player.StreamingPlayer (true live-streaming native
 * playback) -- both need the exact same session before they can start
 * pulling fragments, just for different end goals.
 *
 * See SabrFragmentDownloader's class kdoc for why fragments are pulled
 * directly instead of using yt2009's /exp_hd resolver endpoints.
 */
object SabrSession {

    data class Session(val sabrPath: String, val totalMs: Long?, val qualities: List<QualityOption>)

    /** A selectable video quality/itag, as html5-player.js's own quality menu builds it. */
    data class QualityOption(val itag: Int, val label: String, val height: Int)

    class ResolveException(message: String) : Exception(message)

    /**
     * yt2009's watch page embeds the SABR session inline as plain JS
     * (back/yt2009html.js: `var sabrBase = "/sabr_playback?pid=...";`) when
     * SABR is enabled for the request. Forces exp_sabr on for this one
     * request regardless of the user's live-playback SABR setting --
     * downloading/native playback and WebView live playback are
     * independent choices.
     */
    fun resolve(client: OkHttpClient, config: ServerConfig, videoId: String): Session {
        val url = "${config.baseUrl}/watch?v=$videoId"
        val request = Request.Builder()
            .url(url)
            .header("Cookie", cookieHeader(config))
            .build()

        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ResolveException("could not open the watch page (HTTP ${response.code})")
            }
            response.body?.string().orEmpty()
        }

        val sabrPath = Regex("""var sabrBase = "(/sabr_playback\?pid=[^"]+)"""")
            .find(html)?.groupValues?.get(1)
            ?: throw ResolveException(
                "this instance didn't return a SABR session for this video " +
                    "(is exp_sabr available / is this actually a yt2009 watch page?)"
            )

        // back/yt2009html.js only takes the SABR branch's "initAsSabr()" path
        // for non-live videos; live videos get "initLiveChat"/"initAsLive()"
        // instead and never get a fixed duration, which this session
        // resolution (and everything that consumes it) isn't meant to handle.
        if (html.contains("initAsLive()")) {
            throw ResolveException("this is a live stream, which isn't supported here yet")
        }

        // yt2009utils.seconds_to_time formats duration as [H:]M:SS and the
        // page renders it as "0:00 / <duration>" next to the player
        val totalMs = Regex("""0:00\s*/\s*(\d+(?::\d{2}){1,2})""").find(html)?.groupValues?.get(1)
            ?.let { parseClock(it) }

        return Session(sabrPath, totalMs, parseQualities(html))
    }

    /**
     * html5-player.js's quality picker (the "Quality" flyout on the HD
     * button) is built from `window.sabrExactRes` -- embedded the same way
     * `sabrBase` is, as a JSON-string-typed JS var (yt2009templates.js
     * playerHDSabr(): `var sabrExactRes = '${JSON.stringify(exactData)}';`,
     * then `JSON.parse()`'d by the page's own JS before use, see
     * html5-player.js:3220). Each entry is `[itag, label, height, ...]`
     * (`q[0]`/`q[1]`/`q[2]` respectively) -- only those three fields are
     * used here, the rest (codec/super-resolution metadata) isn't needed
     * for a plain quality switcher. Absent entirely on instances/videos
     * that don't expose per-itag selection; callers should treat an empty
     * list as "no manual quality picker available," not an error.
     */
    internal fun parseQualities(html: String): List<QualityOption> {
        val raw = Regex("""var sabrExactRes = '(\[.*\])';""").find(html)?.groupValues?.get(1)
            ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val q = array.optJSONArray(i) ?: return@mapNotNull null
                val itag = q.optInt(0, -1)
                val label = q.optString(1, null) ?: return@mapNotNull null
                if (itag < 0) return@mapNotNull null
                QualityOption(itag, label, q.optInt(2, 0))
            }
        } catch (e: Exception) {
            emptyList() // quality switching is a nice-to-have, never worth failing playback over
        }
    }

    fun cookieHeader(config: ServerConfig): String {
        val existing = CookieManager.getInstance().getCookie(config.baseUrl)
        // make sure exp_sabr is present regardless of the user's live
        // playback preference -- see SabrFragmentDownloader's class kdoc
        return if (existing.isNullOrBlank()) {
            "maytube_dl_flags=exp_sabr"
        } else {
            "$existing; maytube_dl_flags=exp_sabr"
        }
    }

    private fun parseClock(clock: String): Long? {
        val parts = clock.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty()) return null
        var seconds = 0L
        for (p in parts) seconds = seconds * 60 + p
        return seconds * 1000
    }
}
