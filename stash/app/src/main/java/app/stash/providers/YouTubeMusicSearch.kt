package app.stash.providers

import app.stash.data.db.Track
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * search through youtube music's internal (innertube) api, the same call music.youtube.com makes.
 * songs first (real artist / album / duration); where the songs catalog isn't available
 * it falls back to the videos tab, which still has the channel and duration.
 */
@Singleton
class YouTubeMusicSearch @Inject constructor(private val http: OkHttpClient) {

    suspend fun search(query: String, limit: Int): List<Track> {
        val songs = request(query, SONGS)
        return (songs.ifEmpty { request(query, VIDEOS) }).take(limit)
    }

    private suspend fun request(query: String, params: String): List<Track> = httpIo {
        val body = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", CLIENT_VERSION)
                    put("hl", "en")
                }
            }
            put("query", query)
            put("params", params)
        }.toString()
        val req = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/search?prettyPrint=false")
            .header("User-Agent", BROWSER_UA)
            .header("Origin", "https://music.youtube.com")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        val text = http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw ProviderException("YouTube Music search failed (${r.code})")
            r.body!!.string()
        }
        val items = mutableListOf<JsonObject>()
        collect(lenientJson.parseToJsonElement(text), items)
        items.mapNotNull(::toTrack)
    }

    /**
     * youtube music's radio for a song: the "up next" queue its own app builds, ~50 similar
     * songs. this is what all the recommendations and mixes are made from.
     */
    suspend fun radio(videoId: String): List<Track> = httpIo {
        val body = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", CLIENT_VERSION)
                    put("hl", "en")
                }
            }
            put("videoId", videoId)
            put("playlistId", "RDAMVM$videoId")
            put("isAudioOnly", true)
        }.toString()
        val req = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/next?prettyPrint=false")
            .header("User-Agent", BROWSER_UA)
            .header("Origin", "https://music.youtube.com")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        val text = http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw ProviderException("YouTube Music radio failed (${r.code})")
            r.body!!.string()
        }
        val items = mutableListOf<JsonObject>()
        collectKey(lenientJson.parseToJsonElement(text), "playlistPanelVideoRenderer", items)
        items.mapNotNull(::radioTrack).filter { it.sourceId != watchUrl(videoId) }.distinctBy { it.id }
    }

    private fun runsText(o: JsonObject?): String =
        o?.get("runs").asObjectList().joinToString("") { (it["text"] as? JsonPrimitive)?.content.orEmpty() }

    private fun radioTrack(r: JsonObject): Track? {
        val videoId = r.str("videoId") ?: return null
        val title = runsText(r.obj("title")).ifBlank { return null }
        // "Artist • Album • 2019" for songs, "Channel • 1.5B views • 8M likes" for videos
        val parts = runsText(r.obj("longBylineText")).split(" • ").map { it.trim() }
        val byline = parts.firstOrNull().orEmpty()
        val album = parts.getOrNull(1)?.takeUnless { it.endsWith("views") || it.endsWith("likes") || it.all(Char::isDigit) }
        val (artist, cleanTitle) = YtDlpProvider.splitArtistTitle(title, byline, hasTrackField = false)
        val thumb = r.obj("thumbnail")?.get("thumbnails").asObjectList().lastOrNull()?.str("url")
        val url = watchUrl(videoId)
        return Track(
            id = Track.makeId("youtube", url),
            title = cleanTitle,
            artist = artist,
            album = album,
            durationMs = runsText(r.obj("lengthText")).takeIf { DURATION.matches(it) }?.let(::parseDuration) ?: 0,
            artworkUrl = thumb?.replace(Regex("=w\\d+-h\\d+.*$"), "=w544-h544-l90-rj") ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            provider = "youtube",
            sourceId = url,
        )
    }

    private fun watchUrl(videoId: String) = "https://www.youtube.com/watch?v=$videoId"

    private fun collectKey(el: JsonElement, key: String, out: MutableList<JsonObject>) {
        when (el) {
            is JsonObject -> {
                val item = el[key] as? JsonObject
                if (item != null) out += item else el.values.forEach { collectKey(it, key, out) }
            }
            is JsonArray -> el.forEach { collectKey(it, key, out) }
            else -> Unit
        }
    }

    private fun collect(el: JsonElement, out: MutableList<JsonObject>) {
        when (el) {
            is JsonObject -> {
                val item = el["musicResponsiveListItemRenderer"] as? JsonObject
                if (item != null) out += item else el.values.forEach { collect(it, out) }
            }
            is JsonArray -> el.forEach { collect(it, out) }
            else -> Unit
        }
    }

    private fun columnText(item: JsonObject, i: Int): List<String> =
        (item["flexColumns"] as? JsonArray)?.getOrNull(i)?.jsonObject
            ?.obj("musicResponsiveListItemFlexColumnRenderer")?.obj("text")?.get("runs").asObjectList()
            .mapNotNull { (it["text"] as? JsonPrimitive)?.content }

    private fun toTrack(item: JsonObject): Track? {
        val videoId = item.obj("playlistItemData")?.str("videoId") ?: return null
        val title = columnText(item, 0).joinToString("").ifBlank { return null }
        // "Artist • Album • 3:58" for songs, "Channel • 1.5B views • 3:57" for videos,
        // sometimes with a leading "Song" / "Video" label
        val parts = columnText(item, 1).joinToString("").split(" • ").map { it.trim() }.filter { it.isNotEmpty() }
            .dropWhile { it in TYPE_LABELS }
        val duration = parts.lastOrNull { DURATION.matches(it) }
        val rest = parts.filter { it != duration && !it.endsWith(" views") && !it.endsWith(" plays") }
        val artistRaw = rest.firstOrNull() ?: ""
        val album = rest.getOrNull(1)
        val url = "https://www.youtube.com/watch?v=$videoId"
        // videos: titles are often "Artist - Song (Official Video)", reuse the yt-dlp heuristics
        val (artist, cleanTitle) = if (album == null) {
            YtDlpProvider.splitArtistTitle(title, artistRaw, hasTrackField = false)
        } else artistRaw to title
        val thumb = item.obj("thumbnail")?.obj("musicThumbnailRenderer")?.obj("thumbnail")?.get("thumbnails")
            .asObjectList().lastOrNull()?.str("url")
        return Track(
            id = Track.makeId("youtube", url),
            title = cleanTitle,
            artist = artist,
            album = album,
            durationMs = duration?.let(::parseDuration) ?: 0,
            // song thumbnails come at 60px, ask googleusercontent for a bigger square
            artworkUrl = thumb?.replace(Regex("=w\\d+-h\\d+.*$"), "=w544-h544-l90-rj")
                ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            provider = "youtube",
            sourceId = url,
        )
    }

    private fun parseDuration(s: String): Long =
        s.split(':').fold(0L) { acc, p -> acc * 60 + (p.toLongOrNull() ?: 0) } * 1000

    companion object {
        private const val CLIENT_VERSION = "1.20250929.03.00"
        // search filter tabs, as music.youtube.com's chips send them
        private const val SONGS = "EgWKAQIIAWoMEAQQBRAQEBUQChAR"
        private const val VIDEOS = "EgWKAQIQAWoMEAQQBRAQEBUQChAR"
        private val TYPE_LABELS = setOf("Song", "Video")
        private val DURATION = Regex("""\d{1,2}(:\d{2}){1,2}""")
    }
}
