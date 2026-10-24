package app.stash.recs

import android.content.Context
import app.stash.providers.Matcher
import app.stash.providers.httpIo
import app.stash.providers.lenientJson
import app.stash.providers.num
import app.stash.providers.obj
import app.stash.providers.str
import app.stash.providers.asObjectList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * musixmatch through its desktop app's api, the way spotify lyrics plugins use it (no key, a
 * token the api hands out): synced lyrics plus musixmatch's community translations. optional
 * (settings → lyrics); lrclib is used whenever this has nothing or refuses.
 */
@Singleton
class Musixmatch @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: OkHttpClient,
) {
    data class Found(val lyrics: SongLyrics, val commontrackId: Long?)

    private val prefs get() = context.getSharedPreferences("musixmatch", Context.MODE_PRIVATE)

    private fun url(method: String) = "$BASE$method".toHttpUrl().newBuilder()
        .addQueryParameter("app_id", APP_ID)
        .addQueryParameter("format", "json")

    private suspend fun call(url: HttpUrl): JsonObject? = httpIo {
        val req = Request.Builder().url(url)
            .header("Cookie", "AWSELB=unknown; x-mxm-user-id=; x-mxm-token-guid=")
            .header("User-Agent", UA)
            .build()
        http.newCall(req).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
            ?.let { lenientJson.parseToJsonElement(it).jsonObject.obj("message") }
    }

    /** the anonymous user token, kept (they last a long time) and renewed when refused */
    private suspend fun token(renew: Boolean = false): String? {
        if (!renew) prefs.getString("token", null)?.let { return it }
        val body = call(url("token.get").addQueryParameter("user_language", "en").build())?.obj("body") ?: return null
        val token = body.str("user_token")?.takeIf { it.isNotBlank() && !it.all { c -> c == '0' } && "Upgrade" !in it } ?: return null
        prefs.edit().putString("token", token).apply()
        return token
    }

    suspend fun lyrics(title: String, artist: String, durationMs: Long): Found? {
        for (attempt in 0..1) {
            val t = token(renew = attempt == 1) ?: return null
            val u = url("macro.subtitles.get")
                .addQueryParameter("namespace", "lyrics_richsynched")
                .addQueryParameter("subtitle_format", "mxm")
                .addQueryParameter("q_artist", Matcher.primaryArtist(artist))
                .addQueryParameter("q_track", Matcher.stripForSearch(title))
                .apply {
                    if (durationMs > 0) {
                        addQueryParameter("q_duration", (durationMs / 1000).toString())
                        addQueryParameter("f_subtitle_length", (durationMs / 1000).toString())
                    }
                }
                .addQueryParameter("usertoken", t)
                .build()
            val msg = call(u) ?: return null
            // 401: the token was refused (renew once)
            if (msg.obj("header")?.num("status_code")?.toInt() == 401) continue
            val calls = msg.obj("body")?.obj("macro_calls") ?: return null
            val track = calls.obj("matcher.track.get")?.obj("message")?.obj("body")?.obj("track")
            val subtitle = calls.obj("track.subtitles.get")?.obj("message")?.obj("body")?.get("subtitle_list")
                .asObjectList().firstOrNull()?.obj("subtitle")?.str("subtitle_body")
            val lines = subtitle?.let { body ->
                runCatching {
                    (lenientJson.parseToJsonElement(body) as JsonArray).mapNotNull { el ->
                        val o = el as? JsonObject ?: return@mapNotNull null
                        val ms = ((o.obj("time")?.num("total") ?: return@mapNotNull null) * 1000).toLong()
                        ms to (o.str("text") ?: "")
                    }
                }.getOrNull()
            }.orEmpty()
            val plain = calls.obj("track.lyrics.get")?.obj("message")?.obj("body")?.obj("lyrics")?.str("lyrics_body")
                ?.substringBefore("*******")?.trim()
            if (lines.isEmpty() && plain.isNullOrBlank()) return null
            return Found(SongLyrics(lines, plain, source = "Musixmatch"), track?.num("commontrack_id")?.toLong())
        }
        return null
    }

    /** musixmatch's community translation into [lang], as original line -> translated line */
    suspend fun translation(commontrackId: Long, lang: String): Map<String, String>? {
        val t = token() ?: return null
        val u = url("crowd.track.translations.get")
            .addQueryParameter("commontrack_id", commontrackId.toString())
            .addQueryParameter("selected_language", lang)
            .addQueryParameter("comment_format", "text")
            .addQueryParameter("part", "user")
            .addQueryParameter("translation_fields_set", "minimal")
            .addQueryParameter("usertoken", t)
            .build()
        val list = call(u)?.obj("body")?.get("translations_list").asObjectList()
        val map = list.mapNotNull { it.obj("translation") }.mapNotNull { tr ->
            val from = tr.str("subtitle_matched_line") ?: tr.str("matched_line") ?: return@mapNotNull null
            val to = tr.str("description") ?: return@mapNotNull null
            from.trim() to to.trim()
        }.toMap()
        return map.ifEmpty { null }
    }

    private companion object {
        const val BASE = "https://apic-desktop.musixmatch.com/ws/1.1/"
        const val APP_ID = "web-desktop-app-v1.0"
        const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Musixmatch/0.19.4 Chrome/58.0.3029.110 Electron/1.7.6 Safari/537.36"
    }
}
