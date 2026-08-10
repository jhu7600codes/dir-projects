package com.maytube.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * A yt2009 instance the user pointed this app at, plus the small set of
 * playback flags maytube injects as cookies (see MobileInjector / yt2009's
 * own /yt2009_flags.htm for what these do server-side).
 */
data class ServerConfig(
    val host: String,
    val port: Int,
    val useHttps: Boolean = false,
    val sabrEnabled: Boolean = true,
    val prefer1080p: Boolean = false
) {
    val scheme: String get() = if (useHttps) "https" else "http"

    /** Origin the WebView should load, e.g. "http://192.168.1.20:3000" */
    val baseUrl: String get() = "$scheme://$host:$port"

    /** Bare host:port pair, used for cookie domain matching. */
    val hostAndPort: String get() = "$host:$port"
}

/**
 * Thin SharedPreferences-backed store for [ServerConfig]. Everything here is
 * local to the device -- there is no account system of maytube's own, yt2009
 * accounts are simulated purely via cookies inside the WebView itself.
 */
class ServerConfigRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isConfigured(): Boolean = prefs.getString(KEY_HOST, null)?.isNotBlank() == true

    fun get(): ServerConfig? {
        val host = prefs.getString(KEY_HOST, null)?.takeIf { it.isNotBlank() } ?: return null
        val port = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        return ServerConfig(
            host = host,
            port = port,
            useHttps = prefs.getBoolean(KEY_HTTPS, false),
            sabrEnabled = prefs.getBoolean(KEY_SABR, true),
            prefer1080p = prefs.getBoolean(KEY_1080P, false)
        )
    }

    fun save(config: ServerConfig) {
        prefs.edit()
            .putString(KEY_HOST, config.host)
            .putInt(KEY_PORT, config.port)
            .putBoolean(KEY_HTTPS, config.useHttps)
            .putBoolean(KEY_SABR, config.sabrEnabled)
            .putBoolean(KEY_1080P, config.prefer1080p)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "maytube_server_prefs"
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_HTTPS = "use_https"
        private const val KEY_SABR = "sabr_enabled"
        private const val KEY_1080P = "prefer_1080p"
        const val DEFAULT_PORT = 3000
    }
}
