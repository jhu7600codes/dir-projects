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

    suspend fun search(query: String, limit: Int): List<Track> = searchHits(query, limit).map { it.track }

    /** songs, or videos where the songs catalog isn't available, with view counts when shown */
    suspend fun searchHits(query: String, limit: Int): List<SearchHit> {
        val songs = request(query, SONGS).mapNotNull(::toHit)
        return songs.ifEmpty { request(query, VIDEOS).mapNotNull(::toHit) }.take(limit)
    }

    /** artist results; the id is the artist's youtube music browse id (UC…) */
    suspend fun searchArtists(query: String, limit: Int): List<RemoteArtist> =
        request(query, ARTISTS).mapNotNull { item ->
            val name = columnText(item, 0).joinToString("").ifBlank { return@mapNotNull null }
            val sub = columnText(item, 1).joinToString("")
            val browseId = item.obj("navigationEndpoint")?.obj("browseEndpoint")?.str("browseId")
                ?: return@mapNotNull null
            RemoteArtist(
                provider = "youtube",
                id = browseId,
                name = name,
                avatarUrl = item.obj("thumbnail")?.obj("musicThumbnailRenderer")?.obj("thumbnail")?.get("thumbnails")
                    .asObjectList().lastOrNull()?.str("url")?.replace(Regex("=w\\d+-h\\d+.*$"), "=w400-h400-l90-rj"),
                followers = Regex("""([\d.,]+[KMB]?) (monthly audience|subscribers)""").find(sub)?.groupValues?.get(1)?.let(::parseCount),
            )
        }.take(limit)

    private suspend fun request(query: String, params: String): List<JsonObject> = httpIo {
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
        items
    }

    private fun toHit(item: JsonObject): SearchHit? {
        val track = toTrack(item) ?: return null
        val plays = columnText(item, 1).joinToString("").split(" • ")
            .firstOrNull { it.endsWith(" views") || it.endsWith(" plays") }
            ?.substringBefore(' ')?.let(::parseCount)
        return SearchHit(track, plays)
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

    /** an artist's youtube music page: banner, bio, audience, top songs/videos, "fans also like" */
    suspend fun artist(browseId: String): ArtistDetails? = httpIo {
        val body = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", CLIENT_VERSION)
                    put("hl", "en")
                }
            }
            put("browseId", browseId)
        }.toString()
        val req = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/browse?prettyPrint=false")
            .header("User-Agent", BROWSER_UA)
            .header("Origin", "https://music.youtube.com")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        val text = http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw ProviderException("YouTube Music artist page failed (${r.code})")
            r.body!!.string()
        }
        val root = lenientJson.parseToJsonElement(text).jsonObject
        val header = root.obj("header")?.let { h -> (h.values.firstOrNull() as? JsonObject) } ?: return@httpIo null
        val name = runsText(header.obj("title")).ifBlank { return@httpIo null }
        val pictures = header.obj("thumbnail")?.obj("musicThumbnailRenderer")?.obj("thumbnail")?.get("thumbnails").asObjectList()
        val banner = pictures.lastOrNull()?.str("url")

        // top songs come as list rows, videos / singles as cards; both are songs to us
        val rows = mutableListOf<JsonObject>()
        collectKey(root.obj("contents") ?: root, "musicResponsiveListItemRenderer", rows)
        val cards = mutableListOf<JsonObject>()
        collectKey(root.obj("contents") ?: root, "musicTwoRowItemRenderer", cards)
        val songs = rows.mapNotNull(::toTrack) + cards.mapNotNull { c -> cardTrack(c, name) }
        val similar = cards.mapNotNull { c ->
            val id = c.obj("navigationEndpoint")?.obj("browseEndpoint")?.str("browseId")?.takeIf { it.startsWith("UC") && it != browseId }
                ?: return@mapNotNull null
            RemoteArtist(
                provider = "youtube", id = id, name = runsText(c.obj("title")).ifBlank { return@mapNotNull null },
                avatarUrl = c.obj("thumbnailRenderer")?.obj("musicThumbnailRenderer")?.obj("thumbnail")?.get("thumbnails")
                    .asObjectList().lastOrNull()?.str("url"),
                followers = runsText(c.obj("subtitle")).substringBefore(" ").let(::parseCount),
            )
        }.distinctBy { it.id }
        ArtistDetails(
            provider = "youtube",
            id = browseId,
            name = name,
            avatarUrl = banner?.replace(Regex("=w\\d+-h\\d+.*$"), "=w600-h600-p-l90-rj"),
            bannerUrl = banner,
            bio = runsText(header.obj("description")).ifBlank { null },
            followers = header.obj("subscriptionButton")?.obj("subscribeButtonRenderer")?.obj("subscriberCountText")
                ?.let(::runsText)?.let(::parseCount),
            listeners = runsText(header.obj("monthlyListenerCount")).ifBlank { null },
            link = "https://music.youtube.com/channel/$browseId",
            popular = songs.distinctBy { it.id },
            similar = similar,
        )
    }

    /** a video/single card on an artist page ("Creep" · "Radiohead • 1.5B views") */
    private fun cardTrack(c: JsonObject, artist: String): Track? {
        val videoId = c.obj("navigationEndpoint")?.obj("watchEndpoint")?.str("videoId") ?: return null
        val title = runsText(c.obj("title")).ifBlank { return null }
        val url = watchUrl(videoId)
        val (a, t) = YtDlpProvider.splitArtistTitle(title, artist, hasTrackField = false)
        return Track(
            id = Track.makeId("youtube", url), title = t, artist = a, provider = "youtube", sourceId = url,
            artworkUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
        )
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

    /** "1.5B" -> 1_500_000_000 */
    private fun parseCount(s: String): Long? {
        val n = s.replace(",", "").trim()
        val mult = when (n.lastOrNull()) { 'K' -> 1e3; 'M' -> 1e6; 'B' -> 1e9; else -> 1.0 }
        return n.trimEnd('K', 'M', 'B').toDoubleOrNull()?.let { (it * mult).toLong() }
    }

    private fun parseDuration(s: String): Long =
        s.split(':').fold(0L) { acc, p -> acc * 60 + (p.toLongOrNull() ?: 0) } * 1000

    companion object {
        private const val CLIENT_VERSION = "1.20250929.03.00"
        // search filter tabs, as music.youtube.com's chips send them
        private const val SONGS = "EgWKAQIIAWoMEAQQBRAQEBUQChAR"
        private const val VIDEOS = "EgWKAQIQAWoMEAQQBRAQEBUQChAR"
        private const val ARTISTS = "EgWKAQIgAWoMEAQQBRAQEBUQChAR"
        private val TYPE_LABELS = setOf("Song", "Video")
        private val DURATION = Regex("""\d{1,2}(:\d{2}){1,2}""")
    }
}
