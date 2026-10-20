package app.stash.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
    val disabledProviders: Set<String> = emptySet(),
    val jamendoClientId: String = "",
    val spotifyClientId: String = "",
    val appleDeveloperToken: String = "",
    val spotifyConnected: Boolean = false,
    val lastYtDlpUpdate: Long = 0,
    val ytDlpVersion: String = "",
)

@Singleton
class Settings @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic_color")
        val wifiOnly = booleanPreferencesKey("wifi_only")
        val disabled = stringSetPreferencesKey("disabled_providers")
        val jamendo = stringPreferencesKey("jamendo_client_id")
        val spotifyClient = stringPreferencesKey("spotify_client_id")
        val apple = stringPreferencesKey("apple_dev_token")
        val spotifyAccess = stringPreferencesKey("spotify_access")
        val spotifyRefresh = stringPreferencesKey("spotify_refresh")
        val spotifyExpiry = longPreferencesKey("spotify_expiry")
        val spotifyVerifier = stringPreferencesKey("spotify_verifier")
        val ytUpdated = longPreferencesKey("ytdlp_updated")
        val ytVersion = stringPreferencesKey("ytdlp_version")
    }

    private val store get() = context.dataStore

    val flow: Flow<AppSettings> = store.data.map { it.toSettings() }

    suspend fun current(): AppSettings = flow.first()

    private fun Preferences.toSettings() = AppSettings(
        themeMode = this[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        dynamicColor = this[Keys.dynamic] ?: true,
        wifiOnlyDownloads = this[Keys.wifiOnly] ?: false,
        disabledProviders = this[Keys.disabled] ?: emptySet(),
        jamendoClientId = this[Keys.jamendo] ?: "",
        spotifyClientId = this[Keys.spotifyClient] ?: "",
        appleDeveloperToken = this[Keys.apple] ?: "",
        spotifyConnected = !this[Keys.spotifyRefresh].isNullOrEmpty(),
        lastYtDlpUpdate = this[Keys.ytUpdated] ?: 0,
        ytDlpVersion = this[Keys.ytVersion] ?: "",
    )

    suspend fun setTheme(mode: ThemeMode) = store.edit { it[Keys.theme] = mode.name }
    suspend fun setDynamicColor(on: Boolean) = store.edit { it[Keys.dynamic] = on }
    suspend fun setWifiOnly(on: Boolean) = store.edit { it[Keys.wifiOnly] = on }
    suspend fun setProviderEnabled(id: String, enabled: Boolean) = store.edit {
        val set = (it[Keys.disabled] ?: emptySet()).toMutableSet()
        if (enabled) set.remove(id) else set.add(id)
        it[Keys.disabled] = set
    }
    suspend fun setJamendoClientId(v: String) = store.edit { it[Keys.jamendo] = v.trim() }
    suspend fun setSpotifyClientId(v: String) = store.edit { it[Keys.spotifyClient] = v.trim() }
    suspend fun setAppleDeveloperToken(v: String) = store.edit { it[Keys.apple] = v.trim() }
    suspend fun setYtDlpUpdated(version: String) = store.edit {
        it[Keys.ytUpdated] = System.currentTimeMillis()
        it[Keys.ytVersion] = version
    }

    // spotify tokens
    data class SpotifyTokens(val access: String, val refresh: String, val expiresAt: Long)

    suspend fun spotifyTokens(): SpotifyTokens? {
        val p = store.data.first()
        val refresh = p[Keys.spotifyRefresh] ?: return null
        return SpotifyTokens(p[Keys.spotifyAccess] ?: "", refresh, p[Keys.spotifyExpiry] ?: 0)
    }

    suspend fun saveSpotifyTokens(t: SpotifyTokens) = store.edit {
        it[Keys.spotifyAccess] = t.access
        it[Keys.spotifyRefresh] = t.refresh
        it[Keys.spotifyExpiry] = t.expiresAt
    }

    suspend fun clearSpotify() = store.edit {
        it.remove(Keys.spotifyAccess); it.remove(Keys.spotifyRefresh); it.remove(Keys.spotifyExpiry)
    }

    suspend fun saveSpotifyVerifier(v: String) = store.edit { it[Keys.spotifyVerifier] = v }
    suspend fun spotifyVerifier(): String? = store.data.first()[Keys.spotifyVerifier]
}
