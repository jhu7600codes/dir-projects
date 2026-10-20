package app.stash.importer

import android.net.Uri
import android.util.Base64
import app.stash.data.prefs.Settings
import app.stash.providers.BROWSER_UA
import app.stash.providers.ProviderException
import app.stash.providers.arr
import app.stash.providers.asObjectList
import app.stash.providers.getText
import app.stash.providers.httpIo
import app.stash.providers.lenientJson
import app.stash.providers.num
import app.stash.providers.obj
import app.stash.providers.str
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/** a song as another service describes it, before it's matched to a provider */
data class SourceTrack(val title: String, val artist: String, val album: String?, val durationMs: Long, val artworkUrl: String?)

data class SourcePlaylist(
    val name: String,
    val origin: String,
    val originId: String?,
    val coverUrl: String?,
    val tracks: List<SourceTrack>,
)

data class RemotePlaylistInfo(val id: String, val name: String, val coverUrl: String?, val trackCount: Int, val owner: String?)

/**
 * spotify through the web api with an oauth pkce login (no client secret on the phone).
 * the user registers their own app at developer.spotify.com with redirect uri stash://spotify-callback.
 */
@Singleton
class Spotify @Inject constructor(
    private val http: OkHttpClient,
    private val settings: Settings,
) {
    fun redirectUri() = REDIRECT

    suspend fun authorizeUri(): Uri {
        val clientId = settings.current().spotifyClientId
        if (clientId.isBlank()) throw ProviderException("add your spotify client id in settings first")
        val verifier = randomString()
        settings.saveSpotifyVerifier(verifier)
        val challenge = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
        return Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("scope", "playlist-read-private playlist-read-collaborative user-library-read")
            .build()
    }

    suspend fun handleRedirect(uri: Uri) {
        uri.getQueryParameter("error")?.let { throw ProviderException("spotify login: $it") }
        val code = uri.getQueryParameter("code") ?: throw ProviderException("spotify login: no code")
        val verifier = settings.spotifyVerifier() ?: throw ProviderException("spotify login expired, try again")
        token(
            FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT)
                .add("client_id", settings.current().spotifyClientId)
                .add("code_verifier", verifier)
                .build()
        )
    }

    private suspend fun token(form: FormBody): String {
        val text = httpIo {
            http.newCall(Request.Builder().url("https://accounts.spotify.com/api/token").post(form).build()).execute().use {
                val body = it.body!!.string()
                if (!it.isSuccessful) throw ProviderException("spotify token: ${it.code} $body")
                body
            }
        }
        val o = lenientJson.parseToJsonElement(text).jsonObject
        val access = o.str("access_token") ?: throw ProviderException("spotify: no token")
        val refresh = o.str("refresh_token") ?: settings.spotifyTokens()?.refresh ?: ""
        val expires = System.currentTimeMillis() + ((o.num("expires_in") ?: 3600.0) * 1000).toLong() - 60_000
        settings.saveSpotifyTokens(Settings.SpotifyTokens(access, refresh, expires))
        return access
    }

    private suspend fun accessToken(): String {
        val t = settings.spotifyTokens() ?: throw ProviderException("not logged in to spotify")
        if (t.access.isNotEmpty() && System.currentTimeMillis() < t.expiresAt) return t.access
        return token(
            FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", t.refresh)
                .add("client_id", settings.current().spotifyClientId)
                .build()
        )
    }

    private suspend fun get(url: String): JsonObject {
        val token = accessToken()
        val text = httpIo { http.getText(url, mapOf("Authorization" to "Bearer $token")) }
        return lenientJson.parseToJsonElement(text).jsonObject
    }

    private suspend fun paged(first: String, each: (JsonObject) -> Unit) {
        var next: String? = first
        while (next != null) {
            val page = get(next)
            page["items"].asObjectList().forEach(each)
            next = page.str("next")
        }
    }

    suspend fun playlists(): List<RemotePlaylistInfo> {
        val out = mutableListOf<RemotePlaylistInfo>()
        paged("https://api.spotify.com/v1/me/playlists?limit=50") { p ->
            out += RemotePlaylistInfo(
                id = p.str("id") ?: return@paged,
                name = p.str("name") ?: "untitled",
                coverUrl = p["images"].asObjectList().firstOrNull()?.str("url"),
                trackCount = (p.obj("tracks")?.num("total") ?: p.obj("items")?.num("total") ?: 0.0).toInt(),
                owner = p.obj("owner")?.str("display_name"),
            )
        }
        return out
    }

    private fun trackOf(t: JsonObject?): SourceTrack? {
        if (t == null || t.str("type") == "episode") return null
        return SourceTrack(
            title = t.str("name") ?: return null,
            artist = t["artists"].asObjectList().mapNotNull { it.str("name") }.joinToString(", "),
            album = t.obj("album")?.str("name"),
            durationMs = (t.num("duration_ms") ?: 0.0).toLong(),
            artworkUrl = t.obj("album")?.get("images").asObjectList().firstOrNull()?.str("url"),
        )
    }

    suspend fun playlist(info: RemotePlaylistInfo): SourcePlaylist {
        val tracks = mutableListOf<SourceTrack>()
        // spotify renamed /tracks to /items in 2026; try the new one first, fall back to the old
        val collect: (JsonObject) -> Unit = { item -> trackOf(item.obj("item") ?: item.obj("track"))?.let(tracks::add) }
        try {
            paged("https://api.spotify.com/v1/playlists/${info.id}/items?limit=100", collect)
        } catch (e: ProviderException) {
            tracks.clear()
            paged("https://api.spotify.com/v1/playlists/${info.id}/tracks?limit=100", collect)
        }
        return SourcePlaylist(info.name, "spotify", info.id, info.coverUrl, tracks)
    }

    suspend fun likedSongs(): SourcePlaylist {
        val tracks = mutableListOf<SourceTrack>()
        paged("https://api.spotify.com/v1/me/tracks?limit=50") { item -> trackOf(item.obj("track"))?.let(tracks::add) }
        return SourcePlaylist("liked songs (spotify)", "spotify", "liked", null, tracks)
    }

    suspend fun logout() = settings.clearSpotify()

    private fun randomString(): String {
        val bytes = ByteArray(48).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    companion object {
        const val REDIRECT = "stash://spotify-callback"
    }
}

/**
 * apple music: with a musickit developer token the catalog api is used, otherwise the public
 * playlist page is read (it embeds the whole tracklist as json for its own rendering).
 */
@Singleton
class AppleMusic @Inject constructor(
    private val http: OkHttpClient,
    private val settings: Settings,
) {
    private val linkPattern = Regex("""music\.apple\.com/([a-z]{2})/playlist/[^/]*/?(pl\.[A-Za-z0-9.\-]+)""")

    fun findLink(text: String): String? =
        Regex("""https?://music\.apple\.com/\S+""").find(text)?.value

    suspend fun playlist(link: String): SourcePlaylist {
        val m = linkPattern.find(link) ?: throw ProviderException("that doesn't look like an apple music playlist link")
        val (storefront, id) = m.destructured
        val token = settings.current().appleDeveloperToken
        return if (token.isNotBlank()) {
            runCatching { viaApi(storefront, id, token) }.getOrElse { viaPage(link, id) }
        } else viaPage(link, id)
    }

    private suspend fun viaApi(storefront: String, id: String, token: String): SourcePlaylist {
        val headers = mapOf("Authorization" to "Bearer $token", "Origin" to "https://music.apple.com")
        val root = lenientJson.parseToJsonElement(
            httpIo { http.getText("https://api.music.apple.com/v1/catalog/$storefront/playlists/$id", headers) }
        ).jsonObject
        val pl = root["data"].asObjectList().firstOrNull() ?: throw ProviderException("playlist not found")
        val attrs = pl.obj("attributes")
        val tracks = mutableListOf<SourceTrack>()
        var next: String? = "/v1/catalog/$storefront/playlists/$id/tracks?limit=100"
        while (next != null) {
            val page = lenientJson.parseToJsonElement(httpIo { http.getText("https://api.music.apple.com$next", headers) }).jsonObject
            page["data"].asObjectList().forEach { t ->
                val a = t.obj("attributes") ?: return@forEach
                tracks += SourceTrack(
                    title = a.str("name") ?: return@forEach,
                    artist = a.str("artistName") ?: "",
                    album = a.str("albumName"),
                    durationMs = (a.num("durationInMillis") ?: 0.0).toLong(),
                    artworkUrl = a.obj("artwork")?.str("url")?.let(::artwork),
                )
            }
            next = page.str("next")
        }
        return SourcePlaylist(attrs?.str("name") ?: "apple music playlist", "apple", id,
            attrs?.obj("artwork")?.str("url")?.let(::artwork), tracks)
    }

    private suspend fun viaPage(link: String, id: String): SourcePlaylist {
        val html = httpIo { http.getText(link, mapOf("User-Agent" to BROWSER_UA)) }
        val json = Regex("""<script[^>]*id="serialized-server-data"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1) ?: throw ProviderException("couldn't read that playlist page")
        val tracks = mutableListOf<SourceTrack>()
        collect(lenientJson.parseToJsonElement(json), tracks)
        if (tracks.isEmpty()) throw ProviderException("no songs found on that page")
        val title = Regex("""<meta property="og:title" content="([^"]*)"""").find(html)?.groupValues?.get(1)
            ?.let { android.text.Html.fromHtml(it, android.text.Html.FROM_HTML_MODE_LEGACY).toString() }
            ?.substringBefore(" - Apple Music")?.substringBefore(" on Apple Music")
        val cover = Regex("""<meta property="og:image" content="([^"]*)"""").find(html)?.groupValues?.get(1)
        return SourcePlaylist(title ?: "apple music playlist", "apple", id, cover, tracks.distinct())
    }

    /** every object with a title + artistName in the page data is a track lockup */
    private fun collect(el: JsonElement, out: MutableList<SourceTrack>) {
        when (el) {
            is JsonObject -> {
                val title = el.str("title")
                val artist = el.str("artistName")
                if (title != null && artist != null && el.containsKey("duration")) {
                    out += SourceTrack(
                        title = title,
                        artist = artist,
                        album = el.arr("tertiaryLinks")?.firstOrNull()?.let { (it as? JsonObject)?.str("title") }
                            ?.removeSuffix(" - Single")?.removeSuffix(" - EP"),
                        durationMs = (el.num("duration") ?: 0.0).toLong(),
                        artworkUrl = el.obj("artwork")?.obj("dictionary")?.str("url")?.let(::artwork),
                    )
                    return
                }
                el.values.forEach { collect(it, out) }
            }
            is kotlinx.serialization.json.JsonArray -> el.forEach { collect(it, out) }
            else -> Unit
        }
    }

    private fun artwork(template: String) =
        template.replace("{w}", "600").replace("{h}", "600").replace("{f}", "jpg").replace("{c}", "bb")
}
