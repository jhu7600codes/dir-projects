package app.stash.providers

import app.stash.data.db.Track

/** a direct audio url plus whatever headers the host wants on the request */
data class StreamSource(val url: String, val headers: Map<String, String> = emptyMap())

sealed interface DownloadSource {
    /** plain http file, downloaded with okhttp (range requests for resume) */
    data class Http(val url: String, val extension: String, val headers: Map<String, String> = emptyMap()) : DownloadSource

    /** let yt-dlp fetch it, it handles resume and throttling itself */
    data class YtDlp(val pageUrl: String) : DownloadSource
}

/**
 * One place music can come from. Adding a source = one new class implementing this and one
 * line in [ProviderRegistry].
 *
 * Methods get a [Locator] instead of a [Track] because the resolver also calls them with the
 * fallback location cached on a track (resolvedProvider/resolvedSourceId).
 */
interface MusicProvider {
    val id: String
    val displayName: String

    /** false when the provider needs setup (an api key) the user hasn't given yet */
    suspend fun isConfigured(): Boolean = true

    suspend fun search(query: String, limit: Int = 20): List<Track>

    /** called right before playback. never cache the result for long, yt-dlp urls expire */
    suspend fun resolveStream(locator: Locator): StreamSource

    /** fresh metadata for a single item, or null if it's gone */
    suspend fun metadata(sourceId: String): Track?

    suspend fun downloadSource(locator: Locator): DownloadSource
}

data class Locator(val provider: String, val sourceId: String, val streamHint: String?)

val Track.primaryLocator get() = Locator(provider, sourceId, streamHint)

val Track.fallbackLocator: Locator?
    get() = if (resolvedProvider != null && resolvedSourceId != null) {
        Locator(resolvedProvider, resolvedSourceId, resolvedStreamHint)
    } else null

class ProviderException(message: String, cause: Throwable? = null) : Exception(message, cause)
