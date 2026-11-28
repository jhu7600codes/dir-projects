package app.krala.data.provider

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import app.krala.BuildConfig
import app.krala.data.settings.SettingsRepository
import app.krala.data.sync.SupabaseHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini refuses requests from russia. With a vpn on, requests go straight to google. Without
 * one, krala looks up the country of the current ip and, if it's russia (or google already said
 * "location not supported"), sends gemini calls through the gemini-proxy edge function in the eu.
 */
@Singleton
class GeminiRoute @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: HttpClient,
    private val settings: SettingsRepository,
    private val supabase: SupabaseHolder,
) {
    private data class Check(val network: String, val country: String?, val at: Long)

    @Volatile private var check: Check? = null
    @Volatile private var blockedOn: String? = null

    /** what the last decision was, for the settings screen */
    @Volatile var lastCountry: String? = null
        private set

    fun isGemini(cfg: ProviderConfig) = "generativelanguage.googleapis.com" in cfg.baseUrl

    fun vpnOn(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    /** identifies the current connection, so switching wifi / mobile / vpn re-checks */
    private fun networkKey(): String {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        return (cm?.activeNetwork?.toString() ?: "none") + "/" + vpnOn()
    }

    /** country code of the current ip ("RU", "DE", ...), cached per connection for 10 minutes */
    suspend fun country(force: Boolean = false): String? {
        val key = networkKey()
        val c = check
        if (!force && c != null && c.network == key && System.currentTimeMillis() - c.at < 10 * 60_000) return c.country
        val found = withTimeoutOrNull(6_000) {
            runCatching {
                http.get("https://www.cloudflare.com/cdn-cgi/trace").bodyAsText()
                    .lines().firstOrNull { it.startsWith("loc=") }?.removePrefix("loc=")?.trim()
            }.getOrNull() ?: runCatching {
                Regex(""""country"\s*:\s*"([A-Z]{2})"""").find(http.get("https://api.country.is/").bodyAsText())?.groupValues?.get(1)
            }.getOrNull()
        }
        check = Check(key, found, System.currentTimeMillis())
        lastCountry = found
        return found
    }

    /** google said "location not supported" on this connection: proxy from now on (unless vpn) */
    fun markBlocked() { blockedOn = networkKey() }

    suspend fun shouldProxy(): Boolean {
        val mode = settings.current().geminiProxy
        if (mode == "off" || !supabase.configured) return false
        if (vpnOn()) return false
        if (mode == "always") return true
        if (blockedOn == networkKey()) return true
        return country() == "RU"
    }

    /** the config to actually call: unchanged, or pointed at the proxy */
    suspend fun route(cfg: ProviderConfig, force: Boolean = false): ProviderConfig {
        if (!isGemini(cfg)) return cfg
        if (!force && !shouldProxy()) return cfg
        val client = supabase.client ?: return cfg
        val token = runCatching { client.auth.currentSessionOrNull()?.accessToken }.getOrNull()
            ?: throw ProviderException(app.krala.core.FallbackPlanner.ErrorKind.AUTH, null, "the gemini proxy needs a krala session (guest is fine): open account once while online")
        return cfg.copy(
            baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/gemini-proxy",
            apiKey = token,
            headers = cfg.headers + mapOf(
                "apikey" to BuildConfig.SUPABASE_ANON_KEY,
                "x-krala-key" to cfg.apiKey,
                "x-region" to "eu-central-1",
            ),
        )
    }

    fun isGeoBlock(e: ProviderException) =
        e.message.contains("location is not supported", true) || e.message.contains("not available in your country", true) ||
            e.message.contains("unsupported_country", true)
}
