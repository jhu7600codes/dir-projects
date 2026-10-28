package dev.jhu.crossi.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.jhu.crossi.game.InputPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = true,
    val autoAdvance: Boolean = true,
    val skipFilled: Boolean = true,
    val jumpToNextClue: Boolean = true,
    val haptics: Boolean = true,
    val showTimer: Boolean = true,
    val highlightCrossing: Boolean = false,
    val seenHowToPlay: Boolean = false,
) {
    val inputPrefs: InputPrefs get() = InputPrefs(autoAdvance, skipFilled, jumpToNextClue)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

class SettingsRepository(context: Context) {
    private val store = context.dataStore

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic_color")
        val autoAdvance = booleanPreferencesKey("auto_advance")
        val skipFilled = booleanPreferencesKey("skip_filled")
        val jump = booleanPreferencesKey("jump_next_clue")
        val haptics = booleanPreferencesKey("haptics")
        val timer = booleanPreferencesKey("show_timer")
        val crossing = booleanPreferencesKey("highlight_crossing")
        val seenHowTo = booleanPreferencesKey("seen_how_to_play")
    }

    val settings: Flow<Settings> = store.data.map { p ->
        val d = Settings()
        Settings(
            themeMode = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.themeMode,
            dynamicColor = p[Keys.dynamic] ?: d.dynamicColor,
            autoAdvance = p[Keys.autoAdvance] ?: d.autoAdvance,
            skipFilled = p[Keys.skipFilled] ?: d.skipFilled,
            jumpToNextClue = p[Keys.jump] ?: d.jumpToNextClue,
            haptics = p[Keys.haptics] ?: d.haptics,
            showTimer = p[Keys.timer] ?: d.showTimer,
            highlightCrossing = p[Keys.crossing] ?: d.highlightCrossing,
            seenHowToPlay = p[Keys.seenHowTo] ?: d.seenHowToPlay,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[Keys.theme] = mode.name }
    suspend fun setDynamicColor(on: Boolean) = store.edit { it[Keys.dynamic] = on }
    suspend fun setAutoAdvance(on: Boolean) = store.edit { it[Keys.autoAdvance] = on }
    suspend fun setSkipFilled(on: Boolean) = store.edit { it[Keys.skipFilled] = on }
    suspend fun setJumpToNextClue(on: Boolean) = store.edit { it[Keys.jump] = on }
    suspend fun setHaptics(on: Boolean) = store.edit { it[Keys.haptics] = on }
    suspend fun setShowTimer(on: Boolean) = store.edit { it[Keys.timer] = on }
    suspend fun setHighlightCrossing(on: Boolean) = store.edit { it[Keys.crossing] = on }
    suspend fun setSeenHowToPlay() = store.edit { it[Keys.seenHowTo] = true }
}
