package app.krala.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val haptics: Boolean = true,
    val language: String = "",
    val systemPrompt: String? = null,
    val customInstructions: String = "",
    val defaultProviderId: String? = null,
    val defaultModel: String? = null,
    val memoryProviderId: String? = null,
    val memoryModel: String? = null,
    val autoRecall: Boolean = true,
    val embeddingsEnabled: Boolean = false,
    val embeddingProviderId: String? = null,
    val embeddingModel: String? = null,
    val biometricLock: Boolean = false,
    val syncEnabled: Boolean = false,
    val syncKeys: Boolean = false,
    val syncPassphraseCipher: String? = null,
    val lastSyncAt: Long = 0,
    val lastSyncError: String? = null,
    val sendOnEnter: Boolean = false,
    val settingsUpdatedAt: Long = 0,
    val presetsSeeded: Boolean = false,
    /** duckduckgo (free, no key), brave, tavily or searxng */
    val webProvider: String = "duckduckgo",
    val webKeyCipher: String? = null,
    val webSearxUrl: String = "",
    val webPages: Int = 3,
    /** talk mode shows what was said; off by default */
    val talkTranscript: Boolean = false,
    /** text to speech engine package, null = phone default */
    val ttsEngine: String? = null,
    val ttsVoiceEn: String? = null,
    val ttsVoiceRu: String? = null,
    val voiceRate: Float = 1f,
    val voicePitch: Float = 1f,
    /** how talk mode listens: auto, phone or whisper */
    val sttMode: String = "auto",
    /** gemini proxy for russia: auto (no vpn + russian ip), always, off */
    val geminiProxy: String = "auto",
) {
    val effectiveSystemPrompt: String get() = systemPrompt ?: Personality.DEFAULT_SYSTEM_PROMPT
}

/** the part of the settings that follows the account to other devices */
@Serializable
data class SyncedSettings(
    val themeMode: String? = null,
    val dynamicColor: Boolean? = null,
    val haptics: Boolean? = null,
    val language: String? = null,
    val systemPrompt: String? = null,
    val systemPromptIsDefault: Boolean? = null,
    val customInstructions: String? = null,
    val defaultProviderId: String? = null,
    val defaultModel: String? = null,
    val memoryProviderId: String? = null,
    val memoryModel: String? = null,
    val autoRecall: Boolean? = null,
    val sendOnEnter: Boolean? = null,
)

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic")
        val haptics = booleanPreferencesKey("haptics")
        val language = stringPreferencesKey("language")
        val systemPrompt = stringPreferencesKey("system_prompt")
        val instructions = stringPreferencesKey("instructions")
        val defaultProvider = stringPreferencesKey("default_provider")
        val defaultModel = stringPreferencesKey("default_model")
        val memoryProvider = stringPreferencesKey("memory_provider")
        val memoryModel = stringPreferencesKey("memory_model")
        val autoRecall = booleanPreferencesKey("auto_recall")
        val embeddings = booleanPreferencesKey("embeddings")
        val embeddingProvider = stringPreferencesKey("embedding_provider")
        val embeddingModel = stringPreferencesKey("embedding_model")
        val biometric = booleanPreferencesKey("biometric")
        val sync = booleanPreferencesKey("sync")
        val syncKeys = booleanPreferencesKey("sync_keys")
        val passphrase = stringPreferencesKey("sync_passphrase")
        val lastSync = longPreferencesKey("last_sync")
        val lastSyncError = stringPreferencesKey("last_sync_error")
        val sendOnEnter = booleanPreferencesKey("send_on_enter")
        val settingsUpdated = longPreferencesKey("settings_updated")
        val seeded = booleanPreferencesKey("presets_seeded")
        val webProvider = stringPreferencesKey("web_provider")
        val webKey = stringPreferencesKey("web_key")
        val webSearx = stringPreferencesKey("web_searx")
        val webPages = androidx.datastore.preferences.core.intPreferencesKey("web_pages")
        val transcript = booleanPreferencesKey("talk_transcript")
        val ttsEngine = stringPreferencesKey("tts_engine")
        val ttsVoiceEn = stringPreferencesKey("tts_voice_en")
        val ttsVoiceRu = stringPreferencesKey("tts_voice_ru")
        val voiceRate = androidx.datastore.preferences.core.floatPreferencesKey("voice_rate")
        val voicePitch = androidx.datastore.preferences.core.floatPreferencesKey("voice_pitch")
        val sttMode = stringPreferencesKey("stt_mode")
        val geminiProxy = stringPreferencesKey("gemini_proxy")
    }

    val settings: Flow<AppSettings> = context.store.data
        .catch { emit(emptyPreferences()) }
        .map { p ->
            AppSettings(
                themeMode = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
                dynamicColor = p[K.dynamic] ?: false,
                haptics = p[K.haptics] ?: true,
                language = p[K.language] ?: "",
                systemPrompt = p[K.systemPrompt],
                customInstructions = p[K.instructions] ?: "",
                defaultProviderId = p[K.defaultProvider],
                defaultModel = p[K.defaultModel],
                memoryProviderId = p[K.memoryProvider],
                memoryModel = p[K.memoryModel],
                autoRecall = p[K.autoRecall] ?: true,
                embeddingsEnabled = p[K.embeddings] ?: false,
                embeddingProviderId = p[K.embeddingProvider],
                embeddingModel = p[K.embeddingModel],
                biometricLock = p[K.biometric] ?: false,
                syncEnabled = p[K.sync] ?: false,
                syncKeys = p[K.syncKeys] ?: false,
                syncPassphraseCipher = p[K.passphrase],
                lastSyncAt = p[K.lastSync] ?: 0,
                lastSyncError = p[K.lastSyncError],
                sendOnEnter = p[K.sendOnEnter] ?: false,
                settingsUpdatedAt = p[K.settingsUpdated] ?: 0,
                presetsSeeded = p[K.seeded] ?: false,
                webProvider = p[K.webProvider] ?: "duckduckgo",
                webKeyCipher = p[K.webKey],
                webSearxUrl = p[K.webSearx] ?: "",
                webPages = p[K.webPages] ?: 3,
                talkTranscript = p[K.transcript] ?: false,
                ttsEngine = p[K.ttsEngine],
                ttsVoiceEn = p[K.ttsVoiceEn],
                ttsVoiceRu = p[K.ttsVoiceRu],
                voiceRate = p[K.voiceRate] ?: 1f,
                voicePitch = p[K.voicePitch] ?: 1f,
                sttMode = p[K.sttMode] ?: "auto",
                geminiProxy = p[K.geminiProxy] ?: "auto",
            )
        }

    suspend fun current(): AppSettings = settings.first()

    private suspend fun editSynced(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.store.edit {
            block(it)
            it[K.settingsUpdated] = System.currentTimeMillis()
        }
    }

    private fun <T> androidx.datastore.preferences.core.MutablePreferences.setOrRemove(key: Preferences.Key<T>, v: T?) {
        if (v == null) remove(key) else this[key] = v
    }

    suspend fun setThemeMode(m: ThemeMode) = editSynced { it[K.theme] = m.name }
    suspend fun setDynamicColor(v: Boolean) = editSynced { it[K.dynamic] = v }
    suspend fun setHaptics(v: Boolean) = editSynced { it[K.haptics] = v }
    suspend fun setLanguage(v: String) = editSynced { it[K.language] = v }
    suspend fun setSystemPrompt(v: String?) = editSynced { it.setOrRemove(K.systemPrompt, v?.takeIf { s -> s.isNotBlank() }) }
    suspend fun setCustomInstructions(v: String) = editSynced { it[K.instructions] = v }
    suspend fun setDefaultModel(providerId: String?, model: String?) = editSynced {
        it.setOrRemove(K.defaultProvider, providerId); it.setOrRemove(K.defaultModel, model)
    }
    suspend fun setMemoryModel(providerId: String?, model: String?) = editSynced {
        it.setOrRemove(K.memoryProvider, providerId); it.setOrRemove(K.memoryModel, model)
    }
    suspend fun setAutoRecall(v: Boolean) = editSynced { it[K.autoRecall] = v }
    suspend fun setSendOnEnter(v: Boolean) = editSynced { it[K.sendOnEnter] = v }

    suspend fun setEmbeddings(enabled: Boolean, providerId: String?, model: String?) = context.store.edit {
        it[K.embeddings] = enabled
        it.setOrRemove(K.embeddingProvider, providerId)
        it.setOrRemove(K.embeddingModel, model)
    }

    suspend fun setBiometricLock(v: Boolean) = context.store.edit { it[K.biometric] = v }
    suspend fun setSyncEnabled(v: Boolean) = context.store.edit { it[K.sync] = v }
    suspend fun setSyncKeys(v: Boolean, passphraseCipher: String?) = context.store.edit {
        it[K.syncKeys] = v
        it.setOrRemove(K.passphrase, passphraseCipher)
    }
    suspend fun setLastSync(at: Long, error: String?) = context.store.edit {
        if (error == null) it[K.lastSync] = at
        it.setOrRemove(K.lastSyncError, error)
    }
    suspend fun setWebSearch(provider: String, keyCipher: String?, searxUrl: String, pages: Int) = context.store.edit {
        it[K.webProvider] = provider
        it.setOrRemove(K.webKey, keyCipher)
        it[K.webSearx] = searxUrl.trim()
        it[K.webPages] = pages.coerceIn(0, 5)
    }

    suspend fun setTtsEngine(v: String?) = context.store.edit { it.setOrRemove(K.ttsEngine, v); it.remove(K.ttsVoiceEn); it.remove(K.ttsVoiceRu) }
    suspend fun setTtsVoice(russian: Boolean, name: String?) = context.store.edit { it.setOrRemove(if (russian) K.ttsVoiceRu else K.ttsVoiceEn, name) }
    suspend fun setVoiceTuning(rate: Float, pitch: Float) = context.store.edit { it[K.voiceRate] = rate; it[K.voicePitch] = pitch }
    suspend fun setGeminiProxy(v: String) = context.store.edit { it[K.geminiProxy] = v }
    suspend fun setSttMode(v: String) = context.store.edit { it[K.sttMode] = v }

    suspend fun setTalkTranscript(v: Boolean) = context.store.edit { it[K.transcript] = v }

    suspend fun setPresetsSeeded() = context.store.edit { it[K.seeded] = true }

    // ---- sync bookkeeping ----

    suspend fun lastPulled(table: String): Long = context.store.data.first()[longPreferencesKey("pull_$table")] ?: 0
    suspend fun setLastPulled(table: String, at: Long) = context.store.edit { it[longPreferencesKey("pull_$table")] = at }
    suspend fun resetSyncCursors() = context.store.edit { p ->
        p.asMap().keys.filter { it.name.startsWith("pull_") }.forEach { k -> p.remove(k) }
        p.remove(K.lastSync)
    }

    suspend fun syncUser(): String? = context.store.data.first()[stringPreferencesKey("sync_user")]
    suspend fun setSyncUser(id: String?) = context.store.edit { it.setOrRemove(stringPreferencesKey("sync_user"), id) }

    fun toSynced(s: AppSettings) = SyncedSettings(
        themeMode = s.themeMode.name,
        dynamicColor = s.dynamicColor,
        haptics = s.haptics,
        language = s.language,
        systemPrompt = s.systemPrompt,
        systemPromptIsDefault = s.systemPrompt == null,
        customInstructions = s.customInstructions,
        defaultProviderId = s.defaultProviderId,
        defaultModel = s.defaultModel,
        memoryProviderId = s.memoryProviderId,
        memoryModel = s.memoryModel,
        autoRecall = s.autoRecall,
        sendOnEnter = s.sendOnEnter,
    )

    /** applies settings from the cloud without bumping the local timestamp past the remote one */
    suspend fun applySynced(r: SyncedSettings, remoteUpdatedAt: Long) = context.store.edit { p ->
        r.themeMode?.let { p[K.theme] = it }
        r.dynamicColor?.let { p[K.dynamic] = it }
        r.haptics?.let { p[K.haptics] = it }
        r.language?.let { p[K.language] = it }
        if (r.systemPromptIsDefault == true) p.remove(K.systemPrompt) else r.systemPrompt?.let { p[K.systemPrompt] = it }
        r.customInstructions?.let { p[K.instructions] = it }
        p.setOrRemove(K.defaultProvider, r.defaultProviderId)
        p.setOrRemove(K.defaultModel, r.defaultModel)
        p.setOrRemove(K.memoryProvider, r.memoryProviderId)
        p.setOrRemove(K.memoryModel, r.memoryModel)
        r.autoRecall?.let { p[K.autoRecall] = it }
        r.sendOnEnter?.let { p[K.sendOnEnter] = it }
        p[K.settingsUpdated] = remoteUpdatedAt
    }
}
