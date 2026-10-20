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
    val appleDeveloperToken: String = "",
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
        val apple = stringPreferencesKey("apple_dev_token")
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
        appleDeveloperToken = this[Keys.apple] ?: "",
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
    suspend fun setAppleDeveloperToken(v: String) = store.edit { it[Keys.apple] = v.trim() }
    suspend fun setYtDlpUpdated(version: String) = store.edit {
        it[Keys.ytUpdated] = System.currentTimeMillis()
        it[Keys.ytVersion] = version
    }
}
