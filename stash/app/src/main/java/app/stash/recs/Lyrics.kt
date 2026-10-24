package app.stash.recs

import app.stash.providers.Matcher
import app.stash.providers.asObjectList
import app.stash.providers.getText
import app.stash.providers.httpIo
import app.stash.providers.lenientJson
import app.stash.providers.num
import app.stash.providers.str
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/** a song's lyrics: [lines] are time-synced (ms, text), [plain] when only unsynced text exists */
data class SongLyrics(
    val lines: List<Pair<Long, String>>,
    val plain: String?,
    val source: String = "LRCLIB",
    /** musixmatch's id for the song, for its translations */
    val commontrackId: Long? = null,
) {
    val synced get() = lines.isNotEmpty()
}

/**
 * live lyrics from lrclib.net, a free open lyrics database with time-synced (lrc) lyrics. no
 * key. looked up by artist + title + length, then by search; cached for the session.
 */
@Singleton
class Lyrics @Inject constructor(
    private val http: OkHttpClient,
    private val musixmatch: Musixmatch,
) {
    private val cache = ConcurrentHashMap<String, SongLyrics>()
    private val translations = ConcurrentHashMap<String, List<String?>>()

    /** [preferMusixmatch]: musixmatch first, lrclib if it has nothing */
    suspend fun forSong(title: String, artist: String, durationMs: Long, preferMusixmatch: Boolean = false): SongLyrics? {
        val key = "$artist|$title|$preferMusixmatch"
        cache[key]?.let { return it }
        if (preferMusixmatch) {
            runCatching { musixmatch.lyrics(title, artist, durationMs) }.getOrNull()?.let { f ->
                val l = f.lyrics.copy(commontrackId = f.commontrackId)
                cache[key] = l
                return l
            }
        }
        val cleanTitle = Matcher.stripForSearch(title)
        val firstArtist = Matcher.primaryArtist(artist)
        val exact = runCatching {
            val url = "https://lrclib.net/api/get".toHttpUrl().newBuilder()
                .addQueryParameter("artist_name", firstArtist)
                .addQueryParameter("track_name", cleanTitle)
                .apply { if (durationMs > 0) addQueryParameter("duration", (durationMs / 1000).toString()) }
                .build()
            lenientJson.parseToJsonElement(httpIo { http.getText(url.toString(), mapOf("User-Agent" to UA)) }).jsonObject
        }.getOrNull()?.takeIf { it.str("syncedLyrics") != null || it.str("plainLyrics") != null }
        val found = exact ?: runCatching {
            val url = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
                .addQueryParameter("track_name", cleanTitle)
                .addQueryParameter("artist_name", firstArtist)
                .build()
            val results = lenientJson.parseToJsonElement(httpIo { http.getText(url.toString(), mapOf("User-Agent" to UA)) }) as? JsonArray
            pick(results.asObjectList(), durationMs)
        }.getOrNull()
            // romanized / other spellings: search by title alone, still by length
            ?: runCatching {
                val url = "https://lrclib.net/api/search".toHttpUrl().newBuilder().addQueryParameter("q", "$firstArtist $cleanTitle").build()
                val results = lenientJson.parseToJsonElement(httpIo { http.getText(url.toString(), mapOf("User-Agent" to UA)) }) as? JsonArray
                pick(results.asObjectList(), durationMs)
            }.getOrNull()
            // the title alone (the artist spelled another way, "サツキ" for "Satsuki"), only an exact-ish length counts
            ?: runCatching {
                if (durationMs <= 0) return@runCatching null
                val url = "https://lrclib.net/api/search".toHttpUrl().newBuilder().addQueryParameter("q", cleanTitle).build()
                val results = lenientJson.parseToJsonElement(httpIo { http.getText(url.toString(), mapOf("User-Agent" to UA)) }) as? JsonArray
                pick(results.asObjectList().filter { abs((it.num("duration") ?: 0.0) * 1000 - durationMs) <= 2000 }, durationMs)
            }.getOrNull()
            ?: return null
        val lyrics = SongLyrics(parseLrc(found.str("syncedLyrics").orEmpty()), found.str("plainLyrics"))
        if (!lyrics.synced && lyrics.plain == null) return null
        cache[key] = lyrics
        return lyrics
    }

    /**
     * a translation for every line (null where there's none) into [lang]: musixmatch's community
     * translation when the song came from there and has one, else google's machine translation
     */
    suspend fun translate(lyrics: SongLyrics, lang: String): List<String?>? {
        val texts = if (lyrics.synced) lyrics.lines.map { it.second } else lyrics.plain.orEmpty().lines()
        val key = "${texts.hashCode()}|$lang"
        translations[key]?.let { return it }
        val community = lyrics.commontrackId?.let { id -> runCatching { musixmatch.translation(id, lang) }.getOrNull() }
        val result = if (community != null) texts.map { community[it.trim()] }
        else runCatching { machine(texts, lang) }.getOrNull() ?: return null
        translations[key] = result
        return result
    }

    /** google translate's free web endpoint, the whole song in one request (lines kept apart) */
    private suspend fun machine(texts: List<String>, lang: String): List<String?>? {
        val joined = texts.joinToString("\n")
        val url = "https://translate.googleapis.com/translate_a/single".toHttpUrl().newBuilder()
            .addQueryParameter("client", "gtx").addQueryParameter("sl", "auto").addQueryParameter("tl", lang)
            .addQueryParameter("dt", "t").addQueryParameter("q", joined).build()
        val root = lenientJson.parseToJsonElement(httpIo { http.getText(url.toString()) }) as? JsonArray ?: return null
        val out = (root.firstOrNull() as? JsonArray)?.joinToString("") { seg ->
            ((seg as? JsonArray)?.firstOrNull() as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
        } ?: return null
        val lines = out.split("\n")
        // blank lines stay blank; if google merged lines, give up rather than misalign
        if (lines.size != texts.size) return null
        return lines.mapIndexed { i, t -> if (texts[i].isBlank() || t.trim().equals(texts[i].trim(), true)) null else t.trim() }
    }

    /** closest in length (within 5s when the length is known), synced ones first */
    private fun pick(results: List<JsonObject>, durationMs: Long): JsonObject? =
        results.filter { it.str("syncedLyrics") != null || it.str("plainLyrics") != null }
            .filter { durationMs <= 0 || abs((it.num("duration") ?: 0.0) * 1000 - durationMs) <= 5000 }
            .sortedWith(compareBy({ if (it.str("syncedLyrics") != null) 0 else 1 }, { abs((it.num("duration") ?: 0.0) * 1000 - durationMs) }))
            .firstOrNull()

    companion object {
        private const val UA = "Stash (personal music app)"
        private val stamp = Regex("""\[(\d+):(\d+(?:\.\d+)?)]""")

        /** "[01:02.50] line" (several stamps per line allowed) -> sorted (ms, text) */
        fun parseLrc(lrc: String): List<Pair<Long, String>> = lrc.lines().flatMap { line ->
            val stamps = stamp.findAll(line).toList()
            if (stamps.isEmpty()) return@flatMap emptyList()
            val text = line.substring(stamps.last().range.last + 1).trim()
            stamps.map { m -> (m.groupValues[1].toLong() * 60_000 + (m.groupValues[2].toDouble() * 1000).toLong()) to text }
        }.sortedBy { it.first }
    }
}
