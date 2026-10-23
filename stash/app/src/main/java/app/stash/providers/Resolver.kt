package app.stash.providers

import android.util.Log
import app.stash.data.db.Track
import app.stash.data.db.TrackDao
import app.stash.data.prefs.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.io.File
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max

/** every provider, in fallback order */
@Singleton
class ProviderRegistry @Inject constructor(
    youtube: YouTubeProvider,
    soundcloud: SoundCloudProvider,
    bandcamp: BandcampProvider,
    audius: AudiusProvider,
    jamendo: JamendoProvider,
    archive: InternetArchiveProvider,
    fma: FreeMusicArchiveProvider,
    private val settings: Settings,
) {
    val all: List<MusicProvider> = listOf(youtube, soundcloud, audius, bandcamp, jamendo, archive, fma)

    fun get(id: String): MusicProvider? = all.firstOrNull { it.id == id }

    /** enabled and configured, in fallback order */
    suspend fun active(): List<MusicProvider> {
        val disabled = settings.current().disabledProviders
        return all.filter { it.id !in disabled && it.isConfigured() }
    }
}

object Matcher {
    private val brackets = Regex("""\s*[(\[][^)\]]*[)\]]""")
    private val feat = Regex("""\s+(feat\.?|ft\.?|featuring|with)\s+.*$""", RegexOption.IGNORE_CASE)
    private val nonWord = Regex("""[^\p{L}\p{N}]+""")

    fun normalize(s: String): String {
        val noAccents = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        return noAccents.replace(brackets, " ").replace(feat, "").replace(nonWord, " ").trim()
    }

    private fun tokens(s: String) = normalize(s).split(' ').filter { it.isNotEmpty() }.toSet()

    private fun overlap(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return a.intersect(b).size.toDouble() / max(a.size, b.size)
    }

    /**
     * 0..1 for how likely [candidate] is the song "[artist] - [title]" of [durationMs].
     * Titles carry the most weight, artist can also show up inside a youtube title,
     * and a close duration is a strong signal that it's not a live/extended cut.
     */
    fun score(candidate: Track, title: String, artist: String, durationMs: Long): Double {
        val wantTitle = tokens(title)
        val wantArtist = tokens(artist.split(',', '&').first())
        val haveTitle = tokens(candidate.title)
        val haveAll = haveTitle + tokens(candidate.artist)

        val titleScore = if (wantTitle.isNotEmpty()) wantTitle.count { it in haveAll }.toDouble() / wantTitle.size else 0.0
        val extra = (haveTitle - wantTitle - wantArtist).size // words like "live", "remix", "cover"
        val artistScore = overlap(wantArtist, tokens(candidate.artist)).coerceAtLeast(
            if (wantArtist.isNotEmpty()) wantArtist.count { it in haveAll }.toDouble() / wantArtist.size else 0.0
        )
        var s = titleScore * 0.55 + artistScore * 0.3 - minOf(extra, 4) * 0.03
        if (durationMs > 0 && candidate.durationMs > 0) {
            val diff = abs(durationMs - candidate.durationMs) / 1000
            s += when {
                diff <= 3 -> 0.15
                diff <= 8 -> 0.1
                diff <= 20 -> 0.0
                else -> -0.2
            }
        } else {
            s += 0.05
        }
        // right title by a different artist is a cover, not the song
        if (wantArtist.isNotEmpty() && artistScore == 0.0) s = minOf(s, 0.4)
        return s.coerceIn(0.0, 1.0)
    }

    const val ACCEPT = 0.6
}

/**
 * Picks where a track actually plays from: the downloaded file first, then the track's own
 * provider, then whichever fallback worked last time, then a fresh search through every other
 * provider in order. Whatever works is remembered on the track.
 */
@Singleton
class Resolver @Inject constructor(
    private val registry: ProviderRegistry,
    private val dao: TrackDao,
) {
    private data class Cached(val source: StreamSource, val at: Long)

    /**
     * Stream urls are only memoised for a few minutes so seeking doesn't re-run yt-dlp
     * every time. They're never written anywhere, they expire within hours.
     */
    private val shortCache = ConcurrentHashMap<String, Cached>()
    private val inFlight = ConcurrentHashMap<String, Deferred<StreamSource>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun invalidate(trackId: String) {
        shortCache.remove(trackId)
    }

    suspend fun streamFor(trackId: String): StreamSource {
        val track = dao.get(trackId) ?: throw ProviderException("Unknown song $trackId")
        track.localPath?.let { path ->
            val f = File(path)
            if (f.exists() && f.length() > 0) return StreamSource(f.toURI().toString())
        }
        shortCache[trackId]?.let { if (System.currentTimeMillis() - it.at < CACHE_MS) return it.source }

        // the prefetch and exoplayer can ask for the same song at once; share one lookup
        val job = inFlight.computeIfAbsent(trackId) {
            scope.async { resolveWithFallback(track) { provider, loc -> provider.resolveStream(loc) } }
        }
        val source = try {
            job.await()
        } finally {
            inFlight.remove(trackId, job)
        }
        shortCache[trackId] = Cached(source, System.currentTimeMillis())
        return source
    }

    suspend fun downloadSourceFor(track: Track): DownloadSource =
        resolveWithFallback(track) { provider, loc -> provider.downloadSource(loc) }

    private suspend fun <T> resolveWithFallback(track: Track, op: suspend (MusicProvider, Locator) -> T): T {
        val errors = mutableListOf<String>()
        val tried = mutableSetOf<String>()

        for (loc in listOfNotNull(track.fallbackLocator, track.primaryLocator)) {
            val provider = registry.get(loc.provider) ?: continue
            tried += loc.provider
            try {
                return op(provider, loc)
            } catch (e: Exception) {
                Log.w(TAG, "${provider.id} failed for ${track.title}", e)
                errors += "${provider.displayName}: ${e.message}"
            }
        }

        for (provider in registry.active()) {
            if (provider.id in tried) continue
            val match = runCatching { bestMatch(provider, track.title, track.artist, track.durationMs) }.getOrNull() ?: continue
            try {
                val result = op(provider, match.primaryLocator)
                dao.setResolved(track.id, match.provider, match.sourceId, match.streamHint)
                return result
            } catch (e: Exception) {
                errors += "${provider.displayName}: ${e.message}"
            }
        }
        throw ProviderException(errors.lastOrNull() ?: "Nothing could play this")
    }

    private suspend fun bestMatch(provider: MusicProvider, title: String, artist: String, durationMs: Long): Track? {
        val results = provider.search("$artist $title".trim(), 8)
        return results.map { it to Matcher.score(it, title, artist, durationMs) }
            .filter { it.second >= Matcher.ACCEPT }
            .maxByOrNull { it.second }?.first
    }

    /** finds a song from another service (an imported playlist) on the first provider that has it */
    suspend fun match(title: String, artist: String, durationMs: Long): Track? {
        for (provider in registry.active()) {
            val hit = runCatching { bestMatch(provider, title, artist, durationMs) }
                .onFailure { Log.w(TAG, "match via ${provider.id} failed", it) }
                .getOrNull()
            if (hit != null) return hit
        }
        return null
    }

    companion object {
        private const val TAG = "Resolver"
        private const val CACHE_MS = 15 * 60 * 1000L
    }
}
